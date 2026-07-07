const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// Setup PostgreSQL connection pool (Vercel Postgres uses POSTGRES_URL)
const pool = new Pool({
    connectionString: process.env.POSTGRES_URL || process.env.DATABASE_URL || 'postgres://postgres:postgres@localhost:5432/focuslock',
});

// Initialize database schema — mirrors Room entities exactly
async function initDb() {
    try {
        // 1. Users Table (mirrors UserProfile.kt)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                nickname TEXT NOT NULL UNIQUE,
                email TEXT DEFAULT '',
                pin_hash TEXT DEFAULT '',
                avatar_index INTEGER DEFAULT 0,
                custom_avatar_uri TEXT,
                gender TEXT DEFAULT 'neutral',
                language TEXT DEFAULT 'es',
                current_xp INTEGER DEFAULT 0,
                level INTEGER DEFAULT 1,
                total_focused_seconds BIGINT DEFAULT 0,
                total_sessions_completed INTEGER DEFAULT 0,
                total_sessions_failed INTEGER DEFAULT 0,
                current_streak INTEGER DEFAULT 0,
                best_streak INTEGER DEFAULT 0,
                last_session_date BIGINT,
                quote_style_strict BOOLEAN DEFAULT false,
                interests TEXT DEFAULT '',
                long_term_goals TEXT DEFAULT '',
                is_logged_in BOOLEAN DEFAULT false,
                is_registered BOOLEAN DEFAULT false,
                is_account_locked BOOLEAN DEFAULT false,
                guest_expiry_date BIGINT,
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                last_seen BIGINT
            )
        `);

        // 2. Goals Table (mirrors Goal.kt)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS goals (
                id SERIAL PRIMARY KEY,
                owner_nickname TEXT REFERENCES users(nickname) ON DELETE CASCADE,
                title TEXT DEFAULT '',
                description TEXT DEFAULT '',
                duration_minutes INTEGER DEFAULT 25,
                is_pomodoro BOOLEAN DEFAULT false,
                is_completed BOOLEAN DEFAULT false,
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                completed_at BIGINT,
                category TEXT DEFAULT 'general',
                xp_reward INTEGER DEFAULT 50,
                target_app_package TEXT,
                allow_early_complete BOOLEAN DEFAULT true
            )
        `);

        // 3. Focus Sessions Table (mirrors FocusSession.kt)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id SERIAL PRIMARY KEY,
                owner_nickname TEXT REFERENCES users(nickname) ON DELETE CASCADE,
                goal_id INTEGER,
                goal_title TEXT DEFAULT '',
                start_time BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
                end_time BIGINT,
                duration_seconds INTEGER DEFAULT 0,
                is_success BOOLEAN DEFAULT false,
                earned_xp INTEGER DEFAULT 0,
                day_of_week INTEGER DEFAULT 0,
                hour_of_day INTEGER DEFAULT 0
            )
        `);

        // 4. Duels Table (mirrors Duel.kt)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS duels (
                id SERIAL PRIMARY KEY,
                creator_nickname TEXT REFERENCES users(nickname) ON DELETE CASCADE,
                rival_name TEXT NOT NULL,
                rival_avatar TEXT DEFAULT '🐼',
                duration_hours INTEGER NOT NULL,
                xp_wager INTEGER NOT NULL,
                player_progress REAL DEFAULT 0,
                rival_progress REAL DEFAULT 0,
                status TEXT DEFAULT 'Active',
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
            )
        `);

        // 5. Squads Table (mirrors Squad.kt)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS squads (
                id SERIAL PRIMARY KEY,
                creator_nickname TEXT REFERENCES users(nickname) ON DELETE CASCADE,
                name TEXT NOT NULL,
                members_count INTEGER DEFAULT 4,
                cumulative_focus_hours REAL DEFAULT 0,
                penalty_xp INTEGER DEFAULT 200,
                health INTEGER DEFAULT 100,
                status TEXT DEFAULT 'Active',
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
            )
        `);

        // 6. User Settings Table (mirrors UserSettings.kt — NOT synced by default, local-only)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS user_settings (
                id SERIAL PRIMARY KEY,
                owner_nickname TEXT REFERENCES users(nickname) ON DELETE CASCADE,
                vpn_shield_active BOOLEAN DEFAULT false,
                accessibility_locker_active BOOLEAN DEFAULT false,
                wa_timer_minutes INTEGER DEFAULT 5,
                focus_sleep_enabled BOOLEAN DEFAULT true,
                force_sleep_simulation BOOLEAN DEFAULT false,
                allowed_apps TEXT DEFAULT '',
                lesson_alarm_time TEXT DEFAULT ''
            )
        `);

        // 7. Sync Queue Table (mirrors SyncAction.kt — tracks offline-first queue)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS sync_queue (
                id SERIAL PRIMARY KEY,
                action_type TEXT NOT NULL,
                entity_id TEXT,
                payload TEXT,
                status TEXT DEFAULT 'PENDING',
                created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
            )
        `);

        console.log("✅ PostgreSQL Database schema initialized — 7 tables ready.");
    } catch (err) {
        console.error("❌ Error initializing database:", err);
    }
}
initDb();

// ========================
// AUTH ENDPOINTS
// ========================

// Register new user
app.post('/api/users/register', async (req, res) => {
    const { nickname, email, password } = req.body;
    if (!nickname || !password) return res.status(400).json({ error: 'Missing credentials' });

    try {
        const result = await pool.query(
            `INSERT INTO users (nickname, email, pin_hash, is_registered, is_account_locked)
             VALUES ($1, $2, $3, true, true) RETURNING id, nickname, email`,
            [nickname, email || '', password] // In production: use bcrypt
        );
        res.json({ status: 'success', user: result.rows[0] });
    } catch (err) {
        if (err.code === '23505') {
            res.status(409).json({ error: 'User or Email already exists' });
        } else {
            res.status(500).json({ error: err.message });
        }
    }
});

// Register guest (33-day expiry)
app.post('/api/users/register-guest', async (req, res) => {
    const { nickname } = req.body;
    if (!nickname) return res.status(400).json({ error: 'Missing nickname' });

    const expiryMs = Date.now() + (33 * 24 * 60 * 60 * 1000);
    try {
        const result = await pool.query(
            `INSERT INTO users (nickname, is_registered, is_account_locked, guest_expiry_date)
             VALUES ($1, true, true, $2) RETURNING id, nickname, guest_expiry_date`,
            [nickname, expiryMs]
        );
        res.json({ status: 'success', user: result.rows[0] });
    } catch (err) {
        if (err.code === '23505') {
            res.status(409).json({ error: 'Nickname already taken' });
        } else {
            res.status(500).json({ error: err.message });
        }
    }
});

// Login
app.post('/api/users/login', async (req, res) => {
    const { nickname, password } = req.body;
    try {
        const result = await pool.query(
            `SELECT id, nickname, email, avatar_index, custom_avatar_uri, gender, current_xp, level,
                    total_focused_seconds, total_sessions_completed, current_streak, best_streak,
                    is_account_locked, guest_expiry_date, created_at
             FROM users WHERE nickname = $1 AND pin_hash = $2`,
            [nickname, password]
        );
        if (result.rows.length > 0) {
            // Update last_seen
            await pool.query(`UPDATE users SET last_seen = $1, is_logged_in = true WHERE nickname = $2`, [Date.now(), nickname]);
            res.json({ status: 'success', user: result.rows[0] });
        } else {
            res.status(401).json({ error: 'Invalid credentials' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Logout
app.post('/api/users/logout', async (req, res) => {
    const { nickname } = req.body;
    try {
        await pool.query(`UPDATE users SET is_logged_in = false WHERE nickname = $1`, [nickname]);
        res.json({ status: 'success' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// PROFILE SYNC
// ========================

// Full profile sync (upsert)
app.post('/api/users/sync', async (req, res) => {
    const { nickname, avatarIndex, customAvatarUri, gender, currentXp, level,
            totalFocusedSeconds, totalSessionsCompleted, totalSessionsFailed,
            currentStreak, bestStreak, interests } = req.body;
    if (!nickname) return res.status(400).json({ error: 'Missing nickname' });

    try {
        await pool.query(
            `UPDATE users SET
                avatar_index = COALESCE($2, avatar_index),
                custom_avatar_uri = COALESCE($3, custom_avatar_uri),
                gender = COALESCE($4, gender),
                current_xp = COALESCE($5, current_xp),
                level = COALESCE($6, level),
                total_focused_seconds = COALESCE($7, total_focused_seconds),
                total_sessions_completed = COALESCE($8, total_sessions_completed),
                total_sessions_failed = COALESCE($9, total_sessions_failed),
                current_streak = COALESCE($10, current_streak),
                best_streak = COALESCE($11, best_streak),
                interests = COALESCE($12, interests),
                last_seen = $13
             WHERE nickname = $1`,
            [nickname, avatarIndex, customAvatarUri, gender, currentXp, level,
             totalFocusedSeconds, totalSessionsCompleted, totalSessionsFailed,
             currentStreak, bestStreak, interests, Date.now()]
        );
        res.json({ status: 'success' });
    } catch (err) {
        console.error("Sync Error:", err);
        res.status(500).json({ error: err.message });
    }
});

// Get user profile
app.get('/api/users/:nickname', async (req, res) => {
    try {
        const result = await pool.query(`SELECT * FROM users WHERE nickname = $1`, [req.params.nickname]);
        if (result.rows.length > 0) {
            res.json({ status: 'success', user: result.rows[0] });
        } else {
            res.status(404).json({ error: 'User not found' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// LEADERBOARD
// ========================

app.get('/api/users/ranking', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT nickname, custom_avatar_uri, avatar_index, current_xp, level,
                    total_focused_seconds, current_streak
             FROM users
             ORDER BY total_focused_seconds DESC, current_xp DESC
             LIMIT 20`
        );
        res.json({ status: 'success', ranking: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// GOALS
// ========================

app.post('/api/goals', async (req, res) => {
    const { ownerNickname, title, description, durationMinutes, isPomodoro,
            category, xpReward, targetAppPackage, allowEarlyComplete } = req.body;
    if (!ownerNickname) return res.status(400).json({ error: 'Missing ownerNickname' });

    try {
        const result = await pool.query(
            `INSERT INTO goals (owner_nickname, title, description, duration_minutes, is_pomodoro,
                                category, xp_reward, target_app_package, allow_early_complete)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING id`,
            [ownerNickname, title || '', description || '', durationMinutes || 25,
             isPomodoro || false, category || 'general', xpReward || 50,
             targetAppPackage || null, allowEarlyComplete !== false]
        );
        res.json({ status: 'success', goalId: result.rows[0].id });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/goals/:nickname', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT * FROM goals WHERE owner_nickname = $1 ORDER BY created_at DESC`,
            [req.params.nickname]
        );
        res.json({ status: 'success', goals: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/goals/:id/complete', async (req, res) => {
    try {
        await pool.query(
            `UPDATE goals SET is_completed = true, completed_at = $1 WHERE id = $2`,
            [Date.now(), req.params.id]
        );
        res.json({ status: 'success' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.delete('/api/goals/:id', async (req, res) => {
    try {
        await pool.query(`DELETE FROM goals WHERE id = $1`, [req.params.id]);
        res.json({ status: 'success' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// FOCUS SESSIONS
// ========================

app.post('/api/sessions', async (req, res) => {
    const { ownerNickname, goalId, goalTitle, startTime, endTime,
            durationSeconds, isSuccess, earnedXp, dayOfWeek, hourOfDay } = req.body;
    if (!ownerNickname) return res.status(400).json({ error: 'Missing ownerNickname' });

    try {
        const result = await pool.query(
            `INSERT INTO focus_sessions (owner_nickname, goal_id, goal_title, start_time, end_time,
                                         duration_seconds, is_success, earned_xp, day_of_week, hour_of_day)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) RETURNING id`,
            [ownerNickname, goalId || null, goalTitle || '', startTime || Date.now(),
             endTime || null, durationSeconds || 0, isSuccess || false,
             earnedXp || 0, dayOfWeek || 0, hourOfDay || 0]
        );
        res.json({ status: 'success', sessionId: result.rows[0].id });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/sessions/:nickname', async (req, res) => {
    const limit = parseInt(req.query.limit) || 50;
    try {
        const result = await pool.query(
            `SELECT * FROM focus_sessions WHERE owner_nickname = $1 ORDER BY start_time DESC LIMIT $2`,
            [req.params.nickname, limit]
        );
        res.json({ status: 'success', sessions: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Analytics: sessions grouped by day_of_week + hour_of_day
app.get('/api/sessions/:nickname/analytics', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT day_of_week, hour_of_day,
                    COUNT(*) as total_sessions,
                    SUM(CASE WHEN is_success = false THEN 1 ELSE 0 END) as failed_count,
                    AVG(duration_seconds) as avg_duration
             FROM focus_sessions WHERE owner_nickname = $1
             GROUP BY day_of_week, hour_of_day
             ORDER BY day_of_week, hour_of_day`,
            [req.params.nickname]
        );
        res.json({ status: 'success', analytics: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// DUELS
// ========================

app.post('/api/duels', async (req, res) => {
    const { creatorNickname, rivalName, durationHours, xpWager } = req.body;
    if (!creatorNickname || !rivalName) return res.status(400).json({ error: 'Missing creator or rival' });

    try {
        const rivalResult = await pool.query(
            `SELECT custom_avatar_uri, avatar_index FROM users WHERE nickname = $1`, [rivalName]
        );
        const rivalAvatar = (rivalResult.rows.length > 0 && rivalResult.rows[0].custom_avatar_uri)
            ? rivalResult.rows[0].custom_avatar_uri : "🐼";

        const result = await pool.query(
            `INSERT INTO duels (creator_nickname, rival_name, rival_avatar, duration_hours, xp_wager)
             VALUES ($1, $2, $3, $4, $5) RETURNING id`,
            [creatorNickname, rivalName, rivalAvatar, durationHours, xpWager]
        );
        res.json({ status: 'success', duelId: result.rows[0].id });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/duels/active/:nickname', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT * FROM duels
             WHERE (creator_nickname = $1 OR rival_name = $1) AND status = 'Active'`,
            [req.params.nickname]
        );
        res.json({ status: 'success', duels: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/duels/:id/resolve', async (req, res) => {
    const { status, playerProgress } = req.body;
    try {
        await pool.query(
            `UPDATE duels SET status = $1, player_progress = $2 WHERE id = $3`,
            [status, playerProgress || 0, req.params.id]
        );
        res.json({ status: 'success' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// SQUADS
// ========================

app.post('/api/squads', async (req, res) => {
    const { creatorNickname, name, penaltyXp } = req.body;
    if (!creatorNickname || !name) return res.status(400).json({ error: 'Missing creator or name' });

    try {
        const result = await pool.query(
            `INSERT INTO squads (creator_nickname, name, penalty_xp)
             VALUES ($1, $2, $3) RETURNING id`,
            [creatorNickname, name, penaltyXp || 200]
        );
        res.json({ status: 'success', squadId: result.rows[0].id });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/api/squads/active/:nickname', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT * FROM squads WHERE creator_nickname = $1 AND status = 'Active'`,
            [req.params.nickname]
        );
        res.json({ status: 'success', squads: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.put('/api/squads/:id', async (req, res) => {
    const { cumulativeFocusHours, health, status } = req.body;
    try {
        await pool.query(
            `UPDATE squads SET
                cumulative_focus_hours = COALESCE($1, cumulative_focus_hours),
                health = COALESCE($2, health),
                status = COALESCE($3, status)
             WHERE id = $4`,
            [cumulativeFocusHours, health, status, req.params.id]
        );
        res.json({ status: 'success' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ========================
// SYNC BATCH (Offline-First Queue)
// ========================

app.post('/api/sync/batch', async (req, res) => {
    const { actions } = req.body;
    if (!actions || !Array.isArray(actions)) {
        return res.status(400).json({ error: 'Missing actions array' });
    }

    const results = [];
    for (const action of actions) {
        try {
            const payload = JSON.parse(action.payload || '{}');
            switch (action.actionType) {
                case 'SYNC_PROFILE':
                    await pool.query(
                        `UPDATE users SET
                            current_xp = COALESCE($2, current_xp),
                            level = COALESCE($3, level),
                            total_focused_seconds = COALESCE($4, total_focused_seconds),
                            total_sessions_completed = COALESCE($5, total_sessions_completed),
                            current_streak = COALESCE($6, current_streak),
                            last_seen = $7
                         WHERE nickname = $1`,
                        [action.entityId, payload.currentXp, payload.level,
                         payload.totalFocusedSeconds, payload.totalSessionsCompleted,
                         payload.currentStreak, Date.now()]
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                case 'SYNC_GOAL':
                    await pool.query(
                        `INSERT INTO goals (owner_nickname, title, description, duration_minutes,
                                            is_pomodoro, is_completed, category, xp_reward,
                                            target_app_package, allow_early_complete, created_at, completed_at)
                         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
                         ON CONFLICT DO NOTHING`,
                        [action.entityId, payload.title, payload.description || '',
                         payload.durationMinutes || 25, payload.isPomodoro || false,
                         payload.isCompleted || false, payload.category || 'general',
                         payload.xpReward || 50, payload.targetAppPackage || null,
                         payload.allowEarlyComplete !== false,
                         payload.createdAt || Date.now(), payload.completedAt || null]
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                case 'SYNC_SESSION':
                    await pool.query(
                        `INSERT INTO focus_sessions (owner_nickname, goal_id, goal_title, start_time, end_time,
                                                     duration_seconds, is_success, earned_xp, day_of_week, hour_of_day)
                         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
                        [action.entityId, payload.goalId || null, payload.goalTitle || '',
                         payload.startTime || Date.now(), payload.endTime || null,
                         payload.durationSeconds || 0, payload.isSuccess || false,
                         payload.earnedXp || 0, payload.dayOfWeek || 0, payload.hourOfDay || 0]
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                case 'SYNC_DUEL':
                    await pool.query(
                        `INSERT INTO duels (creator_nickname, rival_name, rival_avatar, duration_hours,
                                            xp_wager, player_progress, rival_progress, status)
                         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                         ON CONFLICT DO NOTHING`,
                        [action.entityId, payload.rivalName, payload.rivalAvatar || '🐼',
                         payload.durationHours, payload.xpWager,
                         payload.playerProgress || 0, payload.rivalProgress || 0,
                         payload.status || 'Active']
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                default:
                    results.push({ id: action.id, status: 'UNKNOWN_TYPE' });
            }
        } catch (err) {
            console.error(`Sync action ${action.id} failed:`, err.message);
            results.push({ id: action.id, status: 'ERROR', error: err.message });
        }
    }

    res.json({ status: 'success', results });
});

// ========================
// HEALTH CHECK
// ========================

app.get('/api/health', async (req, res) => {
    try {
        const result = await pool.query('SELECT NOW()');
        res.json({ status: 'ok', serverTime: result.rows[0].now });
    } catch (err) {
        res.status(500).json({ status: 'error', error: err.message });
    }
});

// ========================
// SERVER START
// ========================

if (process.env.NODE_ENV !== 'production') {
    app.listen(PORT, () => {
        console.log(`🚀 FocusLock PostgreSQL backend running on port ${PORT}`);
    });
}

// Export for Vercel Serverless Functions
module.exports = app;

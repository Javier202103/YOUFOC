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

// Initialize database schema
async function initDb() {
    try {
        // 1. Users Table
        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                nickname TEXT PRIMARY KEY,
                email TEXT UNIQUE,
                passwordHash TEXT,
                customAvatarUri TEXT,
                avatarIndex INTEGER,
                totalHours REAL DEFAULT 0,
                xp INTEGER DEFAULT 0,
                lastSeen BIGINT
            )
        `);

        // Safely try to add columns if table already existed before this update
        try {
            await pool.query(`ALTER TABLE users ADD COLUMN email TEXT UNIQUE`);
            await pool.query(`ALTER TABLE users ADD COLUMN passwordHash TEXT`);
        } catch(e) {
            // Ignore if columns already exist
        }

        // 2. Goals Table (server-side backup)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS goals (
                id SERIAL PRIMARY KEY,
                owner TEXT REFERENCES users(nickname),
                title TEXT,
                description TEXT,
                durationMinutes INTEGER,
                isPomodoro BOOLEAN DEFAULT false,
                isCompleted BOOLEAN DEFAULT false,
                targetAppPackage TEXT,
                createdAt BIGINT,
                completedAt BIGINT
            )
        `);

        // 3. Focus Sessions Table
        await pool.query(`
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id SERIAL PRIMARY KEY,
                owner TEXT REFERENCES users(nickname),
                goalTitle TEXT,
                durationMinutes INTEGER,
                completedAt BIGINT,
                xpEarned INTEGER DEFAULT 0
            )
        `);

        // 4. Duels Table
        await pool.query(`
            CREATE TABLE IF NOT EXISTS duels (
                id SERIAL PRIMARY KEY,
                creatorName TEXT,
                rivalName TEXT,
                rivalAvatar TEXT,
                durationHours INTEGER,
                xpWager INTEGER,
                playerProgress REAL DEFAULT 0,
                rivalProgress REAL DEFAULT 0,
                status TEXT DEFAULT 'Active',
                createdAt BIGINT
            )
        `);
        console.log("PostgreSQL Database initialized successfully.");
    } catch (err) {
        console.error("Error initializing database:", err);
    }
}
initDb();

// ========================
// SYNC BATCH (Offline-First Queue)
// ========================
app.post('/api/sync/batch', async (req, res) => {
    const { actions } = req.body; // array of SyncAction objects
    if (!actions || !Array.isArray(actions)) {
        return res.status(400).json({ error: 'Missing actions array' });
    }

    const results = [];
    for (const action of actions) {
        try {
            switch (action.actionType) {
                case 'SYNC_PROFILE':
                    await pool.query(
                        `INSERT INTO users (nickname, totalHours, xp, lastSeen)
                         VALUES ($1, $2, $3, $4)
                         ON CONFLICT(nickname) DO UPDATE SET
                            totalHours = EXCLUDED.totalHours,
                            xp = EXCLUDED.xp,
                            lastSeen = EXCLUDED.lastSeen`,
                        [action.entityId, JSON.parse(action.payload).totalHours || 0, JSON.parse(action.payload).xp || 0, Date.now()]
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                case 'SYNC_GOAL':
                    const goalData = JSON.parse(action.payload);
                    await pool.query(
                        `INSERT INTO goals (owner, title, description, durationMinutes, isPomodoro, isCompleted, targetAppPackage, createdAt, completedAt)
                         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                         ON CONFLICT DO NOTHING`,
                        [action.entityId, goalData.title, goalData.description || '', goalData.durationMinutes, goalData.isPomodoro || false, goalData.isCompleted || false, goalData.targetAppPackage || null, goalData.createdAt || Date.now(), goalData.completedAt || null]
                    );
                    results.push({ id: action.id, status: 'OK' });
                    break;

                case 'SYNC_SESSION':
                    const sessionData = JSON.parse(action.payload);
                    await pool.query(
                        `INSERT INTO focus_sessions (owner, goalTitle, durationMinutes, completedAt, xpEarned)
                         VALUES ($1, $2, $3, $4, $5)`,
                        [action.entityId, sessionData.goalTitle, sessionData.durationMinutes, sessionData.completedAt || Date.now(), sessionData.xpEarned || 0]
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

// Register new user
app.post('/api/users/register', async (req, res) => {
    const { nickname, email, password } = req.body;
    if (!nickname || !password) return res.status(400).json({ error: 'Missing credentials' });

    try {
        await pool.query(
            `INSERT INTO users (nickname, email, passwordHash, avatarIndex) VALUES ($1, $2, $3, 0)`,
            [nickname, email || null, password] // in real app use bcrypt
        );
        res.json({ status: 'success' });
    } catch (err) {
        if (err.code === '23505') { // unique violation
            res.status(409).json({ error: 'User or Email already exists' });
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
            `SELECT * FROM users WHERE nickname = $1 AND passwordHash = $2`,
            [nickname, password]
        );
        if (result.rows.length > 0) {
            res.json({ status: 'success', user: result.rows[0] });
        } else {
            res.status(401).json({ error: 'Invalid credentials' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Sync User Profile
app.post('/api/users/sync', async (req, res) => {
    const { nickname, customAvatarUri, avatarIndex, totalHours, xp } = req.body;
    if (!nickname) {
        return res.status(400).json({ error: 'Missing nickname' });
    }

    const lastSeen = Date.now();
    try {
        await pool.query(
            `INSERT INTO users (nickname, customAvatarUri, avatarIndex, totalHours, xp, lastSeen)
             VALUES ($1, $2, $3, $4, $5, $6)
             ON CONFLICT(nickname) DO UPDATE SET
                customAvatarUri = EXCLUDED.customAvatarUri,
                avatarIndex = EXCLUDED.avatarIndex,
                totalHours = EXCLUDED.totalHours,
                xp = EXCLUDED.xp,
                lastSeen = EXCLUDED.lastSeen`,
            [nickname, customAvatarUri, avatarIndex, totalHours, xp, lastSeen]
        );
        res.json({ status: 'success' });
    } catch (err) {
        console.error("Sync Error:", err);
        res.status(500).json({ error: err.message });
    }
});

// Get global ranking (Live Leaderboard)
app.get('/api/users/ranking', async (req, res) => {
    try {
        const result = await pool.query(`SELECT nickname, customAvatarUri, avatarIndex, totalHours, xp FROM users ORDER BY totalHours DESC, xp DESC LIMIT 20`);
        res.json({ status: 'success', ranking: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Create active duel online
app.post('/api/duels/create', async (req, res) => {
    const { creatorName, rivalName, durationHours, xpWager } = req.body;
    if (!creatorName || !rivalName) {
        return res.status(400).json({ error: 'Missing creatorName or rivalName' });
    }

    try {
        // Get rival avatar details to attach
        const rivalResult = await pool.query(`SELECT customAvatarUri, avatarIndex FROM users WHERE nickname = $1`, [rivalName]);
        const rivalAvatar = (rivalResult.rows.length > 0 && rivalResult.rows[0].customavataruri) 
            ? rivalResult.rows[0].customavataruri 
            : "🐼";
            
        const createdAt = Date.now();

        const insertResult = await pool.query(
            `INSERT INTO duels (creatorName, rivalName, rivalAvatar, durationHours, xpWager, playerProgress, rivalProgress, status, createdAt)
             VALUES ($1, $2, $3, $4, $5, 0.0, 0.0, 'Active', $6) RETURNING id`,
            [creatorName, rivalName, rivalAvatar, durationHours, xpWager, createdAt]
        );
        
        res.json({ status: 'success', duelId: insertResult.rows[0].id });
    } catch (err) {
        console.error("Create Duel Error:", err);
        res.status(500).json({ error: err.message });
    }
});

// Get user active duels
app.get('/api/duels/active/:username', async (req, res) => {
    const username = req.params.username;
    try {
        const result = await pool.query(
            `SELECT * FROM duels 
             WHERE (creatorName = $1 OR rivalName = $2) AND status = 'Active'`,
            [username, username]
        );
        res.json({ status: 'success', duels: result.rows });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

if (process.env.NODE_ENV !== 'production') {
    app.listen(PORT, () => {
        console.log(`FocusLock PostgreSQL backend running on port ${PORT}`);
    });
}

// Export for Vercel Serverless Functions
module.exports = app;

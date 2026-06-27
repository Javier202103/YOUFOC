const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

// Setup PostgreSQL connection pool
const pool = new Pool({
    connectionString: process.env.DATABASE_URL || 'postgres://postgres:postgres@localhost:5432/focuslock',
    // Uncomment the following lines if deploying to Render/Heroku to accept self-signed SSL certs
    // ssl: {
    //     rejectUnauthorized: false
    // }
});

// Initialize database schema
async function initDb() {
    try {
        // 1. Users Table
        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                nickname TEXT PRIMARY KEY,
                customAvatarUri TEXT,
                avatarIndex INTEGER,
                totalHours REAL DEFAULT 0,
                xp INTEGER DEFAULT 0,
                lastSeen BIGINT
            )
        `);

        // 2. Duels Table
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

app.listen(PORT, () => {
    console.log(`FocusLock PostgreSQL backend running on port ${PORT}`);
});

# FocusLock: Advanced Backend Architecture & Predictive Analytics Blueprint
**Role / Persona**: Software Architect & Lead Data Scientist

This blueprint explains the complete mathematical, relational, and backend-level implementation for the two requested advanced modules:
1. **Predictive Session Failure Engine (Machine Learning & Probability Theory)**
2. **Strict 1v1 Multiplayer Duels (Atomic Database Transactions & anti-cheat gamification)**

---

## Part 1: Predictive Session Failure Engine (Motor Predictivo)

### 1.1 The Mathematical Logic

To calculate the probability that a user will fail a focus session ($P(\text{Fail})$) during a specific day of the week and hour slot, we cannot rely on simple ratio statistics when sample sizes are small (e.g., if a user has only done 1 session on Wednesday at 3:00 AM and failed it, a simple ratio gives a 100% failure rate, which is statistically inaccurate).

To resolve this **Cold Start / Sparse Data** issue, we design a **Bayesian Conjugate Prior** model using a **Beta-Binomial Distribution**:
- The historical focus session outcomes ($X_i \in \{0, 1\}$ where $1$ is Success and $0$ is Failure) follow a Bernoulli process.
- The prior probabilities of failure/success can be modeled using the **Beta Distribution**, parameterized by hyperparameters $\alpha$ (Prior Successes) and $\beta$ (Prior Failures).

#### Laplace & Bayesian Smoothing Formula:
$$P(\text{Fail} \mid D, H) = \frac{\text{Failed Sessions}_{(D, H)} + \beta}{\text{Total Sessions}_{(D, H)} + \alpha + \beta}$$

**Parameters Selected for Absolute Focus Mindset:**
- $\alpha = 1.8$: Reflects the global average assumption that focused humans successfully finish approximately $75\%$ of lock sessions.
- $\beta = 0.6$: Reflects the prior weight representing a baseline $25\%$ chance of failure due to ambient noise, fatigue, or interruption.

---

### 1.2 Node.js Express Endpoint Code

This high-performance Node.js controller script uses the official Postgres driver (`pg`) to analyze the `focus_sessions` table (previously referenced as `sesiones_bloqueo`), execute the mathematical smoothing calculations, and return a clean structured JSON payload tailored for the frontend charting library (like Vico or Compose-Charts).

```javascript
/**
 * FocusLock Backend - Express.js API Route Controller
 * FILE: controllers/analyticsController.js
 */
const { Pool } = require('pg');

// Create database Connection Pool
const pool = new Pool({
    connectionString: process.env.DATABASE_URL, // PostgreSQL Connection URL
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

/**
 * GET /api/analytics/predictive-failures/:userId
 * Calculates the statistically smoothed probability of session failure per hour & week weekday.
 */
const getPredictiveFailures = async (req, res) => {
    const { userId } = req.params;

    if (!userId) {
        return res.status(400).json({ error: "Missing parameter: userId" });
    }

    try {
        // Query to group successes vs failures by Hour (0-23) and Day of Week (0=Sunday, 6=Saturday)
        const sqlQuery = `
            SELECT 
                EXTRACT(DOW FROM start_time)::INTEGER AS day_of_week,
                EXTRACT(HOUR FROM start_time)::INTEGER AS hour_of_day,
                COUNT(*)::INTEGER AS total_sessions,
                SUM(CASE WHEN is_success = TRUE THEN 1 ELSE 0 END)::INTEGER AS success_count,
                SUM(CASE WHEN is_success = FALSE THEN 1 ELSE 0 END)::INTEGER AS failed_count
            FROM focus_sessions
            WHERE user_id = $1
            GROUP BY day_of_week, hour_of_day
            ORDER BY day_of_week, hour_of_day;
        `;

        const dbResult = await pool.query(sqlQuery, [userId]);

        // Bayesian Prior Hyperparameters (Laplace Smoothing Config)
        const ALPHA_PRIOR = 1.8; // Baseline Successes
        const BETA_PRIOR = 0.6;  // Baseline Failures

        // Process records and compute Bayesian probabilities
        const predictions = dbResult.rows.map(row => {
            const total = row.total_sessions;
            const fails = row.failed_count;
            
            // Bayes Formula implementation
            const rawProb = total > 0 ? (fails / total) : 0.0;
            const smoothedFailureProbability = (fails + BETA_PRIOR) / (total + ALPHA_PRIOR + BETA_PRIOR);

            return {
                dayOfWeek: row.day_of_week,       // 0-6 (Sun-Sat)
                hourOfDay: row.hour_of_day,       // 0-23
                totalSessions: total,
                successCount: row.success_count,
                failedCount: fails,
                rawFailureRate: parseFloat(rawProb.toFixed(4)),
                predictiveFailureProbability: parseFloat(smoothedFailureProbability.toFixed(4))
            };
        });

        // Calculate total recorded metadata
        const totalHistoricalSessions = dbResult.rows.reduce((sum, r) => sum + r.total_sessions, 0);

        // Standard response package for High-Performance Desktop / Android frontends
        res.status(200).json({
            status: "success",
            userId: userId,
            analyzedSessionsCount: totalHistoricalSessions,
            generatedAt: new Date().toISOString(),
            engine: "Bayesian Beta-Binomial Predictor v1.2",
            predictions: predictions
        });

    } catch (error) {
        console.error("Error generating predictive metrics:", error);
        res.status(500).json({
            status: "error",
            error: "Internal server architecture deviation",
            message: error.message
        });
    }
};

module.exports = {
    getPredictiveFailures
};
```

### 1.3 Expected JSON Structure (Frontend Integration Payload)
This is exactly how the client receives data to render an elite heatmap graph showing which hours represent the user's "Critical Vulnerability Zones":

```json
{
  "status": "success",
  "userId": "d748f3fb-61d0-40e9-b5ab-7ff2378941cf",
  "analyzedSessionsCount": 142,
  "generatedAt": "2026-06-20T05:22:10.150Z",
  "engine": "Bayesian Beta-Binomial Predictor v1.2",
  "predictions": [
    {
      "dayOfWeek": 1,
      "hourOfDay": 3,
      "totalSessions": 4,
      "successCount": 1,
      "failedCount": 3,
      "rawFailureRate": 0.7500,
      "predictiveFailureProbability": 0.5625
    },
    {
      "dayOfWeek": 1,
      "hourOfDay": 9,
      "totalSessions": 28,
      "successCount": 26,
      "failedCount": 2,
      "rawFailureRate": 0.0714,
      "predictiveFailureProbability": 0.0855
    },
    {
      "dayOfWeek": 5,
      "hourOfDay": 23,
      "totalSessions": 8,
      "successCount": 2,
      "failedCount": 6,
      "rawFailureRate": 0.7500,
      "predictiveFailureProbability": 0.6346
    }
  ]
}
```

---

## Part 2: Duels 1v1 Multiplayer Engine (Duelos)

### 2.1 The Relational Schema
The PostgreSQL declaration has been integrated seamlessly into `/database_schema.sql` to support secure 1v1 engagements, containing progress tracking, bet limits, state enums, and optimized indices:

```sql
CREATE TYPE duel_status AS ENUM ('pending_invite', 'active', 'completed', 'forfeited');

CREATE TABLE duelos_activos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    challenger_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    opponent_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    duration_hours INT NOT NULL DEFAULT 4 CHECK (duration_hours > 0),
    xp_wager INT NOT NULL DEFAULT 100 CHECK (xp_wager >= 0),
    
    -- Real-time progress trackers (computed as percentage e.g. 85.50%)
    challenger_progress NUMERIC(5,2) DEFAULT 0.00 CHECK (challenger_progress >= 0.00 AND challenger_progress <= 100.00),
    opponent_progress NUMERIC(5,2) DEFAULT 0.00 CHECK (opponent_progress >= 0.00 AND opponent_progress <= 100.00),
    
    status duel_status NOT NULL DEFAULT 'active',
    winner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Restraints: You cannot challenge yourself in a 1v1 contest
    CONSTRAINT check_self_challenge CHECK (challenger_id <> opponent_id)
);

-- Index optimizations for multiplayer matchmaking lookups
CREATE INDEX idx_duelos_challenger ON duelos_activos(challenger_id);
CREATE INDEX idx_duelos_opponent ON duelos_activos(opponent_id);
CREATE INDEX idx_duelos_status ON duelos_activos(status);
```

---

### 2.2 Transactional Backend Logic (Node.js & Postgres Isolation Levels)

In high-frequency gamified systems, multiple users can trigger bypasses or check-ins simultaneously. To avoid double-spending of XP or race-condition updates, the wager transfer module executes under an **atomic transaction bundle** utilizing PostgreSQL's **`FOR UPDATE` statement**. 

This locks specific participant rows in the `users` table until the transaction commits, ensuring strict mathematical precision.

```javascript
/**
 * FocusLock Backend - Transactional Duel Engine
 * FILE: services/duelService.js
 */
const { Pool } = require('pg');
const pool = new Pool({ connectionString: process.env.DATABASE_URL });

/**
 * Resolves a duel immediately due to one user yielding, cheating, or failing the focus lock.
 * This function atomicly transfers XP points and logs rewards.
 * 
 * @param {string} duelId - UUID of the active duel
 * @param {string} loserId - UUID of the failing participant
 */
async function penalizeAndCloseDuel(duelId, loserId) {
    const client = await pool.connect();
    
    try {
        // Init isolated transaction sequence
        await client.query('BEGIN;');

        // 1. Fetch, authenticate and lock the duel record to prevent concurrent operations
        const duelLockQuery = `
            SELECT challenger_id, opponent_id, xp_wager, status 
            FROM duelos_activos 
            WHERE id = $1 AND status = 'active'
            FOR UPDATE;
        `;
        const duelRes = await client.query(duelLockQuery, [duelId]);
        
        if (duelRes.rows.length === 0) {
            throw new Error("Conflict: Duel does not exist or has already been resolved.");
        }

        const duel = duelRes.rows[0];
        const challengerId = duel.challenger_id;
        const opponentId = duel.opponent_id;
        const xpWager = duel.xp_wager;

        // Verify the provided loser is indeed a participant
        if (loserId !== challengerId && loserId !== opponentId) {
            throw new Error("Malicious Request: Provided loser is not configured for this duel.");
        }

        // Determine the victor identity
        const winnerId = (loserId === challengerId) ? opponentId : challengerId;

        // 2. Lock the loser's user profile and deduct XP safely (minimum floor of 0)
        const deductUserXpQuery = `
            UPDATE users 
            SET current_xp = GREATEST(0, current_xp - $1) 
            WHERE id = $2
            RETURNING nickname, current_xp;
        `;
        const loserRes = await client.query(deductUserXpQuery, [xpWager, loserId]);
        if (loserRes.rows.length === 0) {
            throw new Error("System Error: Loser user profile could not be reached.");
        }
        
        // 3. Lock the winner's user profile and add reward XP
        const addUserXpQuery = `
            UPDATE users 
            SET current_xp = current_xp + $1 
            WHERE id = $2
            RETURNING nickname, current_xp;
        `;
        const winnerRes = await client.query(addUserXpQuery, [xpWager, winnerId]);
        if (winnerRes.rows.length === 0) {
            throw new Error("System Error: Winner user profile could not be reached.");
        }

        // 4. Update the active duel status to 'forfeited' or 'completed'
        const closeDuelQuery = `
            UPDATE duelos_activos 
            SET status = 'forfeited', 
                winner_id = $1, 
                ended_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = $2;
        `;
        await client.query(closeDuelQuery, [winnerId, duelId]);

        // 5. Append detailed logs to ranking points ledger for accountability checks
        const logRankingsQuery = `
            INSERT INTO ranking_points (user_id, points, session_id, action_type)
            VALUES 
                ($1, -$3, NULL, 'emergency_quit_penalty'),
                ($2, $3, NULL, 'duel_victory_reward');
        `;
        await client.query(logRankingsQuery, [loserId, winnerId, xpWager]);

        // Validate state consistency and commit changes
        await client.query('COMMIT;');

        console.log(`[DUEL RESOLVED] Duel ${duelId} closed. Winner: ${winnerRes.rows[0].nickname}. Loser: ${loserRes.rows[0].nickname}. Transferred: ${xpWager} XP.`);

        return {
            success: true,
            duelId,
            winner: {
                id: winnerId,
                nickname: winnerRes.rows[0].nickname,
                newXp: winnerRes.rows[0].current_xp
            },
            loser: {
                id: loserId,
                nickname: loserRes.rows[0].nickname,
                newXp: loserRes.rows[0].current_xp
            },
            xpTransferred: xpWager
        };

    } catch (e) {
        // Rollback current unit of work immediately if database failures occur
        await client.query('ROLLBACK;');
        console.error("[TRANSACTION FAIL] Duel resolution rollback sequence executed:", e.message);
        throw e;
    } finally {
        client.release();
    }
}

module.exports = {
    penalizeAndCloseDuel
};
```

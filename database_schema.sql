-- ====================================================================
-- FocusLock PostgreSQL Complete Database Schema
-- Designed by: Full-Stack Architect & Expert UX/UI DB Specialist
-- Description: Core relational database structure for FocusLock mobile app
-- ====================================================================

-- Enable UUID Extension for robust, non-predictable primary keys (optional, but highly recommended)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- 1. ENUM TYPES & CUSTOM DOMAINS
-- ==========================================
CREATE TYPE gender_type AS ENUM ('male', 'female', 'neutral');
CREATE TYPE level_tier AS ENUM ('Novice', 'Bronze Focus', 'Silver Focus', 'Gold Focus', 'Titan Mindset', 'Focus Deity');

-- ==========================================
-- 2. USERS TABLE
-- ==========================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_index INT DEFAULT 0 CHECK (avatar_index >= 0),
    gender gender_type NOT NULL DEFAULT 'neutral',
    birth_date DATE,
    theme_preference VARCHAR(30) DEFAULT 'dark', -- 'dark' / 'light' / 'feminine_soft'
    
    -- Gamification Engine Data
    current_xp INT NOT NULL DEFAULT 0 CHECK (current_xp >= 0),
    level INT NOT NULL DEFAULT 1 CHECK (level >= 1),
    tier level_tier NOT NULL DEFAULT 'Novice',
    
    -- Active System Overrides (Security / Anti-Cheat)
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    is_device_admin_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_device_override_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. GOALS / TASK LIST TABLE
-- ==========================================
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL CHECK (duration_minutes >= 1),
    
    -- Status Trackers
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 4. FOCUS SESSIONS TABLE
-- ==========================================
CREATE TABLE focus_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id UUID REFERENCES goals(id) ON DELETE SET NULL, -- Retain sessions even if specific goals are removed
    
    -- Timing
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_seconds INT CHECK (duration_seconds >= 0),
    
    -- Outcomes & Anti-Cheat Validation
    is_success BOOLEAN NOT NULL DEFAULT FALSE,
    cheat_flag_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    termination_reason VARCHAR(100), -- 'completed', 'emergency_button', 'unexpected_quit'
    earned_xp INT DEFAULT 0 CHECK (earned_xp >= 0),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 5. ACHIEVEMENTS CATALOG TABLE
-- ==========================================
CREATE TABLE achievements_catalog (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(100) UNIQUE NOT NULL, -- e.g. 'MENTE_TITAN_01'
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    badge_icon_url VARCHAR(255),
    req_type VARCHAR(100) NOT NULL, -- 'accumulated_hours', 'streak_days', 'completed_goals'
    req_value INT NOT NULL CHECK (req_value > 0),
    reward_xp INT NOT NULL CHECK (reward_xp >= 0),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 6. USER ACHIEVEMENTS MAPPING (Bridge Table)
-- ==========================================
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievements_catalog(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_user_achievement UNIQUE (user_id, achievement_id)
);

-- ==========================================
-- 7. RANKING POINTS HISTORY TABLE (Global Leaderboard Source)
-- ==========================================
CREATE TABLE ranking_points (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    points INT NOT NULL, -- Can be positive or negative (penalties!)
    session_id UUID REFERENCES focus_sessions(id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL, -- 'goal_completed', 'session_success', 'emergency_quit_penalty', 'admin_override'
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 8. INDEXES FOR EXTREME SEARCH PERFORMANCE
-- ==========================================
CREATE INDEX idx_users_xp ON users(current_xp DESC);
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_focus_sessions_user_id ON focus_sessions(user_id);
CREATE INDEX idx_ranking_points_user_rewards ON ranking_points(user_id, points);
CREATE INDEX idx_user_achievements_search ON user_achievements(user_id);

-- ==========================================
-- 9. DYNAMIC PROFILE VIEWS (Calculated fields)
-- ==========================================
CREATE OR REPLACE VIEW vista_perfil_profesional AS
SELECT 
    u.id AS user_id,
    u.nickname,
    u.email,
    u.birth_date,
    -- AGE GENERATOR FOR UX DASHBOARD (Calculated dynamically)
    EXTRACT(YEAR FROM AGE(u.birth_date)) AS age,
    u.current_xp,
    u.level,
    u.tier,
    u.gender,
    COALESCE(SUM(fs.duration_seconds) / 3600.0, 0.0) AS total_focused_hours,
    COUNT(DISTINCT ua.achievement_id) AS unlocked_achievements_count
FROM users u
LEFT JOIN focus_sessions fs ON u.id = fs.user_id AND fs.is_success = TRUE
LEFT JOIN user_achievements ua ON u.id = ua.user_id
GROUP BY u.id;

-- Global Live Leaderboard View (Compacts accumulated XP/Points)
CREATE OR REPLACE VIEW vista_ranking_global AS
SELECT 
    ROW_NUMBER() OVER(ORDER BY u.current_xp DESC) AS global_rank,
    u.id AS user_id,
    u.nickname,
    u.level,
    u.tier,
    u.avatar_index,
    u.current_xp AS total_points,
    COALESCE(SUM(fs.duration_seconds) / 60.0, 0.0) AS total_focus_minutes
FROM users u
LEFT JOIN focus_sessions fs ON u.id = fs.user_id AND fs.is_success = TRUE
GROUP BY u.id
ORDER BY total_points DESC;

-- ==========================================
-- 10. TRIGGER FUNCTIONS (AUTOMATIC GAMIFICATION UTILS)
-- ==========================================

-- A. Auto-Calculate Level and Tier relative to XP Increments
CREATE OR REPLACE FUNCTION fn_recalculate_user_level()
RETURNS TRIGGER AS $$
DECLARE
    calculated_level INT;
    new_tier level_tier;
BEGIN
    -- Standard MMO level calculation: Level = Base level + floor(sqrt(xp / 100))
    calculated_level := 1 + FLOOR(SQRT(NEW.current_xp / 100.0));
    
    -- Tier Allocation relative to XP levels
    IF calculated_level < 5 THEN
        new_tier := 'Novice';
    ELSIF calculated_level < 15 THEN
        new_tier := 'Bronze Focus';
    ELSIF calculated_level < 30 THEN
        new_tier := 'Silver Focus';
    ELSIF calculated_level < 50 THEN
        new_tier := 'Gold Focus';
    ELSIF calculated_level < 80 THEN
        new_tier := 'Titan Mindset';
    ELSE
        new_tier := 'Focus Deity';
    END IF;

    NEW.level := calculated_level;
    NEW.tier := new_tier;
    NEW.updated_at := CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_on_user_xp_update
BEFORE UPDATE OF current_xp ON users
FOR EACH ROW
EXECUTE FUNCTION fn_recalculate_user_level();

-- B. Auto-update timestamp helper
CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_timestamp BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();
CREATE TRIGGER trg_goals_timestamp BEFORE UPDATE ON goals FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- ====================================================================
-- Initial Seeds / Demo Data
-- ====================================================================
INSERT INTO achievements_catalog (code, title, description, req_type, req_value, reward_xp) VALUES
('F_FIRST_01', 'Primer Enfoque', 'Completa tu primera sesión de enfoque productivo', 'completed_goals', 1, 100),
('F_STU_05', 'Estudiante Dedicado', 'Completa 5 sesiones de enfoque sin interrupciones', 'completed_goals', 5, 250),
('F_GURU_10', 'Gurú del Enfoque', 'Acumula un total de 10 horas exitosas de enfoque', 'accumulated_hours', 10, 1000),
('F_R_HIERRO', 'Racha de Hierro', 'Entrena y mantén una racha activa de concentración por 3 días seguidos', 'streak_days', 3, 500);

-- ==========================================
-- 11. DUELS 1V1 MULTIPLAYER MODULE
-- ==========================================
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

-- Auto-update audit timestamps on update events
CREATE TRIGGER trg_duelos_active_timestamp BEFORE UPDATE ON duelos_activos FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();


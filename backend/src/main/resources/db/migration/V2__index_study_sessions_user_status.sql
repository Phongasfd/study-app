-- The ranking board and group details look up every member's RUNNING session and today's
-- COMPLETED durations in one query each (WHERE user_id IN (...) AND status = ... AND start_time ...).
CREATE INDEX IF NOT EXISTS idx_study_sessions_user_status_start
    ON study_sessions (user_id, status, start_time);

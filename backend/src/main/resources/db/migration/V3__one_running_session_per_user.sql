-- At most one RUNNING study session per user, enforced by the database.
--
-- Two "start" requests at the same moment (double click, two tabs) each saw no running
-- session and each inserted one; when both were later stopped, the overlapping time was
-- counted twice in the ranking. A check in Java can't stop that (both checks pass before
-- either insert commits), a unique index can: the second insert fails and the service
-- returns the session the first request created.

-- 1. Existing duplicates would make the index creation fail. Keep each user's latest
--    RUNNING session and close the older ones with zero duration: they overlap the kept
--    session, so crediting them would be exactly the double count this migration prevents.
with ranked as (
    select id,
           row_number() over (partition by user_id order by start_time desc nulls last, id desc) as rn
    from study_sessions
    where status = 'RUNNING'
)
update study_sessions s
set status = 'COMPLETED',
    end_time = s.start_time,
    duration_seconds = 0
from ranked r
where s.id = r.id
  and r.rn > 1;

-- 2. Partial index: only RUNNING rows are indexed, so a user still has any number of COMPLETED ones.
create unique index uq_study_sessions_one_running_per_user
    on study_sessions (user_id)
    where status = 'RUNNING';

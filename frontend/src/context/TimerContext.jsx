import { createContext, useContext, useEffect, useState } from "react";
import { createStudySession, completeStudySession, addSubjectToSession } from "../lib/api";

const TimerContext = createContext();

export const TimerProvider = ({ children }) => {
  // ================= STATE =================
  const [startTime, setStartTime] = useState(() => {
    const saved = localStorage.getItem("timer");
    if (saved) {
      const parsed = JSON.parse(saved);
      return parsed.startTime ? Number(parsed.startTime) : null;
    }
    return null;
  });

  const [accumulatedTime, setAccumulatedTime] = useState(() => {
    const saved = localStorage.getItem("timer");
    if (saved) {
      const parsed = JSON.parse(saved);
      return parsed.accumulatedTime ? Number(parsed.accumulatedTime) : 0;
    }
    return 0;
  });

  const [isActive, setIsActive] = useState(() => {
    const saved = localStorage.getItem("timer");
    return saved ? JSON.parse(saved).isActive : false;
  });

  const [activeSubject, setActiveSubject] = useState(() => {
    const saved = localStorage.getItem("timer");
    return saved ? JSON.parse(saved).activeSubject : null;
  });

  const [lastResetDate, setLastResetDate] = useState(() => {
    const saved = localStorage.getItem("timer");
    return saved ? JSON.parse(saved).lastResetDate : new Date().toDateString();
  });

  const [currentSessionId, setCurrentSessionId] = useState(() => {
    const saved = localStorage.getItem("timer");
    return saved ? JSON.parse(saved).currentSessionId : null;
  });

  const [now, setNow] = useState(Date.now()); // trigger re-render

  // save to localStorage on state change
  useEffect(() => {
    localStorage.setItem(
      "timer",
      JSON.stringify({
        startTime,
        accumulatedTime,
        isActive,
        activeSubject,
        lastResetDate,
        currentSessionId,
      })
    );
  }, [startTime, accumulatedTime, isActive, activeSubject, lastResetDate, currentSessionId]);

  // Check for midnight reset on mount (if page was closed during midnight)
  useEffect(() => {
    const today = new Date().toDateString();
    if (lastResetDate !== today) {
      stop();
      setLastResetDate(today);
    }
  }, [lastResetDate]);

  // Timer render loop 
  useEffect(() => {
    const interval = setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  // calculate elapsed time
  const timeElapsed = Math.max(0,
    (Number(accumulatedTime) || 0) +
    (isActive && startTime ? now - Number(startTime) : 0)
  ) || 0;
  // when active, timeElapsed = accumulatedTime + (now - startTime)
  // when paused, timeElapsed = accumulatedTime 
    

  // Actions

  const start = async (subject) => {
    setActiveSubject(subject);
    if (!isActive) {
      const nowTime = Date.now();
      setStartTime(nowTime);
      setIsActive(true);

      try {
        const session = await createStudySession(new Date(nowTime).toISOString());
        setCurrentSessionId(session.id);
        if (subject) {
          await addSubjectToSession(session.id, subject.id);
        }
      } catch (error) {
        console.error("Failed to start study session in DB:", error);
      }
    }
  };

  const pause = async () => {
    if (!isActive) return;
    const nowTime = Date.now();
    setAccumulatedTime((prev) => prev + (nowTime - startTime));
    setIsActive(false);

    if (currentSessionId) {
      try {
        await completeStudySession(currentSessionId, new Date(nowTime).toISOString());
        setCurrentSessionId(null);
      } catch (error) {
        console.error("Failed to complete study session in DB:", error);
      }
    }
  };

  const resume = async () => {
    if (isActive) return;
    const nowTime = Date.now();
    setStartTime(nowTime);
    setIsActive(true);

    try {
      const session = await createStudySession(new Date(nowTime).toISOString());
      setCurrentSessionId(session.id);
      if (activeSubject) {
        await addSubjectToSession(session.id, activeSubject.id);
      }
    } catch (error) {
      console.error("Failed to resume study session in DB:", error);
    }
  };

  const stop = async () => {
    if (isActive && currentSessionId) {
      const nowTime = Date.now();
      try {
        await completeStudySession(currentSessionId, new Date(nowTime).toISOString());
      } catch (error) {
        console.error("Failed to complete study session in DB:", error);
      }
    }
    setStartTime(null);
    setAccumulatedTime(0);
    setIsActive(false);
    setActiveSubject(null);
    setCurrentSessionId(null);
    localStorage.removeItem("timer");
  };

  // Midnight reset logic
  useEffect(() => {
    let timeoutId;

    const scheduleReset = () => {
      const nowTime = new Date();
      const nextMidnight = new Date(nowTime);
      nextMidnight.setHours(24, 0, 0, 0);

      const timeToMidnight = nextMidnight - nowTime;

      timeoutId = setTimeout(() => {
        stop(); // reset toàn bộ
        setLastResetDate(new Date().toDateString());
        scheduleReset();
      }, timeToMidnight);
    };

    scheduleReset();

    return () => {
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, []);

  // Format time from ms to hh:mm:ss
  const formatTime = (ms) => {
    if (isNaN(ms) || ms < 0) return "00:00:00";
    const totalSeconds = Math.floor(ms / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return `${hours.toString().padStart(2, "0")}:${mins.toString().padStart(2, "0")}:${secs
      .toString()
      .padStart(2, "0")}`;
  };

  // Circle progress logic (for 1 hour full circle)
  const circumference = 2 * Math.PI * 48; // For count up, we can make the circle fill every hour or just keep it full
  const strokeDashoffset = 0; // Full circle

  return (
    <TimerContext.Provider
      value={{
        timeElapsed,
        isActive,
        activeSubject,
        start,
        pause,
        resume,
        stop,
        formatTime,
        circumference,
        strokeDashoffset,
      }}
    >
      {children}
    </TimerContext.Provider>
  );
};

export const useTimer = () => useContext(TimerContext);
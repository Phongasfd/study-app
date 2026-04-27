import { createContext, useContext, useEffect, useState } from "react";

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
      })
    );
  }, [startTime, accumulatedTime, isActive, activeSubject]);

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

  const start = (subject) => {
    setActiveSubject(subject);
    if (!isActive) {
      setStartTime(Date.now());
      setIsActive(true);
    }
  };

  const pause = () => {
    if (!isActive) return;
    setAccumulatedTime((prev) => prev + (Date.now() - startTime));
    setIsActive(false);
  };

  const resume = () => {
    if (isActive) return;
    setStartTime(Date.now());
    setIsActive(true);
  };

  const stop = () => {
    setStartTime(null);
    setAccumulatedTime(0);
    setIsActive(false);
    setActiveSubject(null);
    localStorage.removeItem("timer");
  };

  // Midnight reset logic
  useEffect(() => {
    let timeoutId;

    const scheduleReset = () => {
      const now = new Date();
      const nextMidnight = new Date(now);
      nextMidnight.setHours(24, 0, 0, 0);

      timeoutId = setTimeout(() => {
        stop(); // reset toàn bộ
        scheduleReset();
      }, nextMidnight - now);
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
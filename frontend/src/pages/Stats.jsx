import { useState, useEffect } from 'react';
import './Stats.css';
import { getWeeklyStats, syncDailyStat, getMonthlyStats } from '../lib/api';

// Day labels aligned with Java's DayOfWeek Mon=1 … Sun=7
const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

const MONTHLY_DATA = [
  { label: 'Week 1', hours: 28.4 },
  { label: 'Week 2', hours: 34.1 },
  { label: 'Week 3', hours: 22.7 },
  { label: 'Week 4', hours: 39.2 },
];

const MONTHLY_SUBJECTS = [
  { name: 'Math', hours: 48.5, iconClass: 'bg-orange-light text-orange', barClass: 'bg-orange', pct: 42, icon: 'functions', desc: 'Algebra, Calculus & Statistics' },
  { name: 'Science', hours: 38.2, iconClass: 'bg-blue-light text-blue', barClass: 'bg-blue', pct: 33, icon: 'biotech', desc: 'Quantum Physics & Chemistry' },
  { name: 'Language', hours: 37.6, iconClass: 'bg-emerald-light text-emerald', barClass: 'bg-emerald', pct: 33, icon: 'translate', desc: 'Mandarin vocabulary & grammar' },
];

const WEEKLY_SUBJECTS = [
  { name: 'Math', hours: 12.5, iconClass: 'bg-orange-light text-orange', barClass: 'bg-orange', pct: 38, icon: 'functions', desc: 'Algebra & Calculus focuses' },
  { name: 'Science', hours: 10.2, iconClass: 'bg-blue-light text-blue', barClass: 'bg-blue', pct: 31, icon: 'biotech', desc: 'Quantum Physics modules' },
  { name: 'Language', hours: 9.7, iconClass: 'bg-emerald-light text-emerald', barClass: 'bg-emerald', pct: 29, icon: 'translate', desc: 'Mandarin vocabulary & grammar' },
];

const MONTHLY_TRENDS = [
  {
    iconClass: 'bg-secondary-container text-on-secondary-container',
    icon: 'trending_up',
    label: 'Most Productive Week',
    value: 'Week 4',
    sub: '39.2h studied — your personal best this month!',
  },
  {
    iconClass: 'bg-tertiary-fixed text-on-tertiary-fixed',
    icon: 'bolt',
    label: 'Peak Focus Day',
    value: 'Friday',
    sub: 'You consistently peak on Fridays.',
  },
  {
    iconClass: 'bg-blue-light text-blue',
    icon: 'workspace_premium',
    label: 'Goal Completion Rate',
    value: '78%',
    sub: '18 out of 30 days goal reached.',
  },
  {
    iconClass: 'bg-orange-light text-orange',
    icon: 'local_fire_department',
    label: 'Longest Streak',
    value: '9 Days',
    sub: 'Keep it going — streak record incoming!',
  },
];

const WEEKLY_TRENDS = [
  {
    iconClass: 'bg-secondary-container text-on-secondary-container',
    icon: 'bolt',
    label: 'Peak Focus Time',
    value: '8:00 AM — 11:00 AM',
    sub: "You're 24% more efficient in the morning.",
  },
  {
    iconClass: 'bg-tertiary-fixed text-on-tertiary-fixed',
    icon: 'restaurant',
    label: 'Suggested Break',
    value: 'In 45 minutes',
    sub: 'Schedule a 10-min hydration break soon.',
  },
];

const Stats = () => {
  const [viewType, setViewType] = useState('weekly');
  const [dailyGoal, setDailyGoal] = useState(() => {
    const stored = localStorage.getItem('dailyGoal');
    return stored ? parseFloat(stored) : 6;
  });
  const [isEditingGoal, setIsEditingGoal] = useState(false);
  const [goalInput, setGoalInput] = useState(String(dailyGoal));
  const [weeklyData, setWeeklyData] = useState(DAY_LABELS.map((label) => ({ label, hours: 0 })));
  const [monthlyData, setMonthlyData] = useState([]);
  const [weeklyLoading, setWeeklyLoading] = useState(true);
  const [studiedToday, setStudiedToday] = useState(0);

  useEffect(() => {
    // 1. Sync today's stat from completed sessions, then fetch the full week
    syncDailyStat()
      .then((data) => {
        // Update studied hours from today's daily stat
        const hours = parseFloat((data.totalDuration / 3600).toFixed(2));
        setStudiedToday(hours);
      })
      .catch((err) => console.warn('Sync skipped:', err))
      .finally(() => {
        getWeeklyStats()
          .then((data) => {
            const mapped = data.map((entry, i) => ({
              label: DAY_LABELS[i],
              hours: parseFloat((entry.totalDuration / 3600).toFixed(2)),
            }));
            setWeeklyData(mapped);
          })
          .catch((err) => console.error('Failed to fetch weekly stats:', err))
          .finally(() => setWeeklyLoading(false));
      });
  }, []);

  useEffect(() => {
    // Fetch monthly data when view switches to monthly
    if (viewType === 'monthly' && monthlyData.length === 0) {
      getMonthlyStats()
        .then((data) => {
          const mapped = data.map((entry) => ({
            label: entry.weekLabel,
            hours: parseFloat((entry.totalDuration / 3600).toFixed(2)),
          }));
          setMonthlyData(mapped);
        })
        .catch((err) => console.error('Failed to fetch monthly stats:', err));
    }
  }, [viewType]);

  const isMonthly = viewType === 'monthly';

  // Chart data
  const chartData = isMonthly ? monthlyData : weeklyData;
  const maxHours = Math.max(...chartData.map((d) => d.hours), 0.1);
  const totalHours = chartData.reduce((s, d) => s + d.hours, 0);

  // Goal ring
  const pct = Math.min(studiedToday / dailyGoal, 1);
  const R = 80;
  const CIRC = 2 * Math.PI * R;
  const dashOffset = CIRC * (1 - pct);

  const subjects = isMonthly ? MONTHLY_SUBJECTS : WEEKLY_SUBJECTS;
  const trends = isMonthly ? MONTHLY_TRENDS : WEEKLY_TRENDS;

  const handleOpenGoalEditor = () => {
    setGoalInput(String(dailyGoal));
    setIsEditingGoal(true);
  };

  const handleSaveGoal = () => {
    const val = parseFloat(goalInput);
    if (!isNaN(val) && val > 0 && val <= 24) {
      setDailyGoal(val);
      localStorage.setItem('dailyGoal', String(val));
    }
    setIsEditingGoal(false);
  };

  const handleGoalKeyDown = (e) => {
    if (e.key === 'Enter') handleSaveGoal();
    if (e.key === 'Escape') setIsEditingGoal(false);
  };

  return (
    <div className="stats-page">
      {/* Goal Edit Modal */}
      {isEditingGoal && (
        <div className="goal-modal-overlay" onClick={() => setIsEditingGoal(false)}>
          <div className="goal-modal" onClick={(e) => e.stopPropagation()}>
            <div className="goal-modal-header">
              <div className="goal-modal-icon">
                <span className="material-symbols-outlined">flag</span>
              </div>
              <div>
                <h3 className="h3 goal-modal-title">Set Daily Goal</h3>
                <p className="body-md goal-modal-sub">How many hours do you aim to study each day?</p>
              </div>
            </div>

            <div className="goal-input-row">
              <button
                className="goal-stepper-btn"
                onClick={() => setGoalInput(String(Math.max(0.5, parseFloat(goalInput || 1) - 0.5)))}
              >
                <span className="material-symbols-outlined">remove</span>
              </button>
              <div className="goal-input-wrapper">
                <input
                  type="number"
                  className="goal-input"
                  value={goalInput}
                  min="0.5"
                  max="24"
                  step="0.5"
                  onChange={(e) => setGoalInput(e.target.value)}
                  onKeyDown={handleGoalKeyDown}
                  autoFocus
                />
                <span className="goal-input-unit">hours</span>
              </div>
              <button
                className="goal-stepper-btn"
                onClick={() => setGoalInput(String(Math.min(24, parseFloat(goalInput || 0) + 0.5)))}
              >
                <span className="material-symbols-outlined">add</span>
              </button>
            </div>

            <div className="goal-presets">
              {[2, 4, 6, 8].map((h) => (
                <button
                  key={h}
                  className={`goal-preset-btn ${parseFloat(goalInput) === h ? 'active' : ''}`}
                  onClick={() => setGoalInput(String(h))}
                >
                  {h}h
                </button>
              ))}
            </div>

            <div className="goal-modal-actions">
              <button className="goal-cancel-btn" onClick={() => setIsEditingGoal(false)}>
                Cancel
              </button>
              <button className="goal-save-btn" onClick={handleSaveGoal}>
                <span className="material-symbols-outlined">check</span>
                Save Goal
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Statistics Header */}
      <div className="stats-header">
        <div className="stats-title-group">
          <h1 className="h1 text-primary">Performance Insights</h1>
          <p className="body-md text-on-surface-variant">
            {isMonthly ? (
              <>You've studied for <span className="text-primary font-bold">{totalHours.toFixed(1)} hours</span> this month.</>
            ) : (
              <>You've studied for <span className="text-primary font-bold">{totalHours.toFixed(1)} hours</span> this week.</>
            )}
          </p>
        </div>
        <div className="time-toggle">
          <button
            className={`toggle-btn ${!isMonthly ? 'active' : ''}`}
            onClick={() => setViewType('weekly')}
          >
            Week
          </button>
          <button
            className={`toggle-btn ${isMonthly ? 'active' : ''}`}
            onClick={() => setViewType('monthly')}
          >
            Month
          </button>
        </div>
      </div>

      {/* Monthly Summary Stat Chips */}
      {isMonthly && (
        <div className="monthly-stat-chips">
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">calendar_month</span>
            <div>
              <p className="stat-chip-value">{totalHours.toFixed(0)}h</p>
              <p className="stat-chip-label">Total This Month</p>
            </div>
          </div>
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">show_chart</span>
            <div>
              <p className="stat-chip-value">{(totalHours / 4).toFixed(1)}h</p>
              <p className="stat-chip-label">Avg Per Week</p>
            </div>
          </div>
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">emoji_events</span>
            <div>
              <p className="stat-chip-value">{Math.max(...chartData.map((d) => d.hours)).toFixed(1)}h</p>
              <p className="stat-chip-label">Best Week</p>
            </div>
          </div>
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">local_fire_department</span>
            <div>
              <p className="stat-chip-value">9 days</p>
              <p className="stat-chip-label">Longest Streak</p>
            </div>
          </div>
        </div>
      )}

      {/* Bento Grid Layout */}
      <div className="bento-grid">
        {/* Chart Card */}
        <div className="bento-card col-span-8">
          <div className="card-header">
            <h3 className="h3">{isMonthly ? 'Study Hours This Month' : 'Study Hours This Week'}</h3>
            <div className="legend">
              <div className="legend-dot"></div>
              <span className="label-sm text-on-surface-variant">Actual Hours</span>
            </div>
          </div>

          <div className={`chart-container ${isMonthly ? 'monthly-chart' : ''}`}>
            {!isMonthly && weeklyLoading ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', opacity: 0.5 }}>
                <span className="material-symbols-outlined" style={{ fontSize: '2rem', animation: 'spin 1s linear infinite' }}>progress_activity</span>
              </div>
            ) : (
              chartData.map((bar, i) => (
                <div className="bar-group" key={i}>
                  <div className="bar-track">
                    <div
                      className="bar-fill"
                      style={{ height: `${(bar.hours / maxHours) * 100}%` }}
                    >
                      <div className="tooltip">{bar.hours}h</div>
                    </div>
                  </div>
                  <span className="label-sm text-on-surface-variant">{bar.label}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Daily Goal Ring Card */}
        <div className="bento-card col-span-4 daily-goal-card">
          <div className="daily-goal-header">
            <h3 className="h3">Daily Goal</h3>
            <button className="edit-goal-btn" onClick={handleOpenGoalEditor} title="Change daily goal">
              <span className="material-symbols-outlined">edit</span>
            </button>
          </div>

          <div className="goal-ring-container">
            <svg className="goal-svg" viewBox="0 0 192 192">
              <circle className="goal-track" cx="96" cy="96" r={R} />
              <circle
                className="goal-progress"
                cx="96"
                cy="96"
                r={R}
                strokeDasharray={CIRC}
                strokeDashoffset={dashOffset}
              />
            </svg>
            <div className="goal-center">
              <span className="h1">{Math.round(pct * 100)}%</span>
              <span className="label-sm uppercase opacity-80">Complete</span>
            </div>
          </div>

          <div className="goal-text">
            <p className="body-md text-primary-fixed">
              {studiedToday}h / {dailyGoal}h studied today
            </p>
            <p className="label-sm text-primary-fixed-dim mt-1">
              {pct >= 1
                ? '🎉 Goal achieved! Amazing work.'
                : `Keep it up! ${(dailyGoal - studiedToday).toFixed(1)}h to go.`}
            </p>
          </div>
        </div>

        {/* Time per Subject */}
        <div className="bento-card col-span-12">
          <div className="card-header">
            <h3 className="h3">Time per Subject</h3>
          </div>

          <div className="subject-grid">
            {subjects.map((s, i) => (
              <div className="subject-item" key={i}>
                <div className="subject-header">
                  <div className="subject-title-wrapper">
                    <div className={`subject-icon-box ${s.iconClass}`}>
                      <span className="material-symbols-outlined">{s.icon}</span>
                    </div>
                    <span className="h3 subject-title">{s.name}</span>
                  </div>
                  <span className="font-bold text-on-surface-variant">{s.hours}h</span>
                </div>
                <div className="progress-bar-bg">
                  <div className={`progress-bar-fill ${s.barClass}`} style={{ width: `${s.pct}%` }}></div>
                </div>
                <p className="label-sm text-on-surface-variant">{s.desc}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Focus Trends */}
        <div className={`col-span-12 trends-grid ${isMonthly ? 'trends-grid-4' : ''}`}>
          {trends.map((t, i) => (
            <div className="trend-card" key={i}>
              <div className={`trend-icon-box ${t.iconClass}`}>
                <span className="material-symbols-outlined">{t.icon}</span>
              </div>
              <div>
                <p className="trend-label">{t.label}</p>
                <h4 className="h3 trend-value text-primary">{t.value}</h4>
                <p className="label-sm text-on-surface-variant">{t.sub}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Stats;

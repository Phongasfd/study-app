import { useState, useEffect } from 'react';
import './Stats.css';
import { useTranslation } from 'react-i18next';
import {
  getWeeklyStats,
  syncDailyStat,
  getMonthlyStats,
  getWeeklySubjectStats,
  getMonthlySubjectStats,
  getGoals,
  createGoal,
  deleteGoal,
  getSubjects,
} from '../lib/api';

// Day labels aligned with Java's DayOfWeek Mon=1 … Sun=7
const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

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
  const { t } = useTranslation();
  const [viewType, setViewType] = useState('weekly');
  const [dailyGoal, setDailyGoal] = useState(() => {
    const stored = localStorage.getItem('dailyGoal');
    return stored ? parseFloat(stored) : 6;
  });
  const [isEditingGoal, setIsEditingGoal] = useState(false);
  const [goalInput, setGoalInput] = useState(String(dailyGoal));
  const [weeklyData, setWeeklyData] = useState(DAY_LABELS.map((label) => ({ label, hours: 0 })));
  const [monthlyData, setMonthlyData] = useState([]);
  const [monthlySubjectData, setMonthlySubjectData] = useState([]);
  const [weeklyLoading, setWeeklyLoading] = useState(true);
  const [weeklySubjectData, setWeeklySubjectData] = useState([]);
  const [studiedToday, setStudiedToday] = useState(0);
  const [goals, setGoals] = useState([]);
  const [userSubjects, setUserSubjects] = useState([]);
  const [newGoalPeriod, setNewGoalPeriod] = useState('WEEKLY');
  const [newGoalSubjectId, setNewGoalSubjectId] = useState('');
  const [newGoalHours, setNewGoalHours] = useState('10');

  const loadGoals = async () => {
    try {
      const list = await getGoals();
      setGoals(list || []);
      const dailyOverall = (list || []).find((g) => g.period === 'DAILY' && !g.subjectId);
      if (dailyOverall) {
        const hrs = parseFloat((dailyOverall.targetSeconds / 3600).toFixed(2));
        setDailyGoal(hrs);
        localStorage.setItem('dailyGoal', String(hrs));
      }
    } catch {
      // unauthenticated or offline: fallback to localStorage
    }
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Sync today's stat
        const data = await syncDailyStat();
  
        const hours = parseFloat((data.totalDuration / 3600).toFixed(2));
        setStudiedToday(hours);
      } catch (err) {
        console.warn('Sync skipped:', err);
      }
  
      try {
        // 2. Fetch weekly stats
        const data = await getWeeklyStats();
        const subjectData = await getWeeklySubjectStats();

        setWeeklySubjectData(subjectData);
  
        const mapped = data.map((entry, i) => ({
          label: DAY_LABELS[i],
          hours: parseFloat((entry.totalDuration / 3600).toFixed(2)),
        }));
  
        setWeeklyData(mapped);
      } catch (err) {
        console.error('Failed to fetch weekly stats:', err);
      } finally {
        setWeeklyLoading(false);
      }
    };
  
    fetchData();
    loadGoals();
    getSubjects().then((s) => setUserSubjects(s || [])).catch(() => {});
  }, []);

  useEffect(() => {
    const fetchMonthlyData = async () => {
      if (viewType === 'monthly' && monthlyData.length === 0) {
        try {
          const data = await getMonthlyStats();
          const subjectData = await getMonthlySubjectStats();
          setMonthlySubjectData(subjectData);

          const mapped = data.map((entry) => ({
            label: entry.weekLabel,
            hours: parseFloat((entry.totalDuration / 3600).toFixed(2)),
          }));

          setMonthlyData(mapped);
        } catch (err) {
          console.error('Failed to fetch monthly stats:', err);
        }
      }
    };

    fetchMonthlyData();
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

  // If weekly subject data from API is available, use it to render the subject cards.
  const subjects = (() => {
    if (isMonthly) {
      if (monthlySubjectData && monthlySubjectData.length > 0) {
        const totalSeconds = monthlySubjectData.reduce((s, x) => s + (x.totalDurationSeconds || 0), 0) || 1;
        return monthlySubjectData.map((s) => ({
          name: s.subjectName || 'Unknown',
          hours: parseFloat(((s.totalDurationSeconds || 0) / 3600).toFixed(2)),
          pct: Math.round(((s.totalDurationSeconds || 0) / totalSeconds) * 100),
          iconClass: 'bg-indigo-light text-indigo',
          barClass: 'bg-indigo',
          icon: 'school',
          desc: '',
        }));
      }
      return MONTHLY_SUBJECTS;
    }

    if (weeklySubjectData && weeklySubjectData.length > 0) {
      const totalSeconds = weeklySubjectData.reduce((s, x) => s + (x.totalDurationSeconds || 0), 0) || 1;
      // count total seconds to calculate percentage

      // transform data to render 
      return weeklySubjectData.map((s) => ({
        name: s.subjectName || 'Unknown',
        hours: parseFloat(((s.totalDurationSeconds || 0) / 3600).toFixed(2)),
        pct: Math.round(((s.totalDurationSeconds || 0) / totalSeconds) * 100),
        iconClass: 'bg-indigo-light text-indigo',
        barClass: 'bg-indigo',
        icon: 'school',
        desc: '',
      }));
    }

    return WEEKLY_SUBJECTS;
  })();
  const trends = isMonthly ? MONTHLY_TRENDS : WEEKLY_TRENDS;

  const handleOpenGoalEditor = () => {
    setGoalInput(String(dailyGoal));
    setIsEditingGoal(true);
  };

  const handleSaveGoal = async () => {
    const val = parseFloat(goalInput);
    if (!isNaN(val) && val > 0 && val <= 24) {
      setDailyGoal(val);
      localStorage.setItem('dailyGoal', String(val));
      try {
        await createGoal({ subjectId: null, period: 'DAILY', targetMinutes: Math.round(val * 60) });
        await loadGoals();
      } catch (err) {
        console.warn('Could not persist daily goal to server:', err);
      }
    }
    setIsEditingGoal(false);
  };

  const handleCreateExtraGoal = async (e) => {
    e.preventDefault();
    const hrs = parseFloat(newGoalHours);
    if (isNaN(hrs) || hrs <= 0) return;
    try {
      await createGoal({
        subjectId: newGoalSubjectId || null,
        period: newGoalPeriod,
        targetMinutes: Math.round(hrs * 60),
      });
      await loadGoals();
    } catch (err) {
      alert(err.message || 'Failed to save goal');
    }
  };

  const handleDeleteGoal = async (id) => {
    try {
      await deleteGoal(id);
      await loadGoals();
    } catch (err) {
      console.error(err);
    }
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
                <h3 className="h3 goal-modal-title">{t('stats.setDailyGoal')}</h3>
                <p className="body-md goal-modal-sub">{t('stats.howManyHours')}</p>
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
                <span className="goal-input-unit">{t('stats.hoursUnit')}</span>
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
                  {h}{t('stats.hoursShort')}
                </button>
              ))}
            </div>

            <div className="goal-modal-actions">
              <button className="goal-cancel-btn" onClick={() => setIsEditingGoal(false)}>
                {t('auth.cancel')}
              </button>
              <button className="goal-save-btn" onClick={handleSaveGoal}>
                <span className="material-symbols-outlined">check</span>
                {t('stats.saveGoal')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Statistics Header */}
      <div className="stats-header">
        <div className="stats-title-group">
          <h1 className="h1 text-primary">{t('stats.title')}</h1>
          <p className="body-md text-on-surface-variant">
            {isMonthly ? (
              <>{t('stats.youStudiedMonth', { hours: totalHours.toFixed(1) })}</>
            ) : (
              <>{t('stats.youStudiedWeek', { hours: totalHours.toFixed(1) })}</>
            )}
          </p>
        </div>
        <div className="time-toggle">
          <button
            className={`toggle-btn ${!isMonthly ? 'active' : ''}`}
            onClick={() => setViewType('weekly')}
            >
            {t('stats.week')}
          </button>
          <button
            className={`toggle-btn ${isMonthly ? 'active' : ''}`}
            onClick={() => setViewType('monthly')}
            >
            {t('stats.month')}
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
              <p className="stat-chip-label">{t('stats.totalThisMonth')}</p>
            </div>
          </div>
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">show_chart</span>
            <div>
              <p className="stat-chip-value">{(totalHours / 4).toFixed(1)}h</p>
              <p className="stat-chip-label">{t('stats.avgPerWeek')}</p>
            </div>
          </div>
          <div className="stat-chip">
            <span className="material-symbols-outlined stat-chip-icon">emoji_events</span>
            <div>
              <p className="stat-chip-value">{Math.max(...chartData.map((d) => d.hours)).toFixed(1)}h</p>
              <p className="stat-chip-label">{t('stats.bestWeek')}</p>
            </div>
          </div>
        </div>
      )}

      {/* Bento Grid Layout */}
      <div className="bento-grid">
        {/* Chart Card */}
        <div className="bento-card col-span-8">
          <div className="card-header">
            <h3 className="h3">{isMonthly ? t('stats.studyThisMonth') : t('stats.studyThisWeek')}</h3>
            <div className="legend">
              <div className="legend-dot"></div>
              <span className="label-sm text-on-surface-variant">{t('stats.actualHours')}</span>
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
            <h3 className="h3">{t('stats.dailyGoal')}</h3>
            <button className="edit-goal-btn" onClick={handleOpenGoalEditor} title={t('stats.changeDailyGoal')}> 
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
              <span className="label-sm uppercase opacity-80">{t('stats.complete')}</span>
            </div>
          </div>

          <div className="goal-text">
            <p className="body-md text-primary-fixed">
              {t('stats.studiedToday', { studied: studiedToday, goal: dailyGoal })}
            </p>
            <p className="label-sm text-primary-fixed-dim mt-1">
              {pct >= 1
                ? t('stats.goalAchieved')
                : t('stats.keepItUp', { hours: (dailyGoal - studiedToday).toFixed(1) })}
            </p>
          </div>
        </div>

        {/* Multi-Period & Subject Study Goals */}
        <div className="bento-card col-span-12">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <h3 className="h3">{t('stats.studyGoals')}</h3>
            <form onSubmit={handleCreateExtraGoal} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
              <select
                className="filter-select"
                value={newGoalPeriod}
                onChange={(e) => setNewGoalPeriod(e.target.value)}
                style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid var(--color-outline-variant)' }}
              >
                <option value="DAILY">{t('stats.periodDaily')}</option>
                <option value="WEEKLY">{t('stats.periodWeekly')}</option>
                <option value="MONTHLY">{t('stats.periodMonthly')}</option>
              </select>
              <select
                className="filter-select"
                value={newGoalSubjectId}
                onChange={(e) => setNewGoalSubjectId(e.target.value)}
                style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid var(--color-outline-variant)' }}
              >
                <option value="">{t('stats.overallGoal')}</option>
                {userSubjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
              <input
                type="number"
                step="0.5"
                min="0.5"
                max="744"
                value={newGoalHours}
                onChange={(e) => setNewGoalHours(e.target.value)}
                style={{ width: 76, padding: '6px 10px', borderRadius: 8, border: '1px solid var(--color-outline-variant)' }}
              />
              <span className="label-sm">{t('stats.hoursShort')}</span>
              <button type="submit" className="btn btn-primary" style={{ padding: '6px 14px' }}>
                {t('stats.addGoal')}
              </button>
            </form>
          </div>

          {goals.length === 0 ? (
            <p className="body-md text-on-surface-variant" style={{ marginTop: 12 }}>{t('stats.noGoalsYet')}</p>
          ) : (
            <div className="subject-grid" style={{ marginTop: 16 }}>
              {goals.map((g) => (
                <div className="subject-item" key={g.id}>
                  <div className="subject-header">
                    <div className="subject-title-wrapper">
                      <span className="h3 subject-title">
                        {g.subjectName || t('stats.overallGoal')} ({g.period})
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="font-bold text-on-surface-variant">
                        {(g.progressSeconds / 3600).toFixed(1)}h / {(g.targetSeconds / 3600).toFixed(1)}h ({g.percent}%)
                      </span>
                      <button
                        onClick={() => handleDeleteGoal(g.id)}
                        style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: 'var(--color-outline)' }}
                        title="Remove goal"
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>close</span>
                      </button>
                    </div>
                  </div>
                  <div className="progress-bar-bg">
                    <div className="progress-bar-fill bg-indigo" style={{ width: `${g.percent}%` }}></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Time per Subject */}
        <div className="bento-card col-span-12">
          <div className="card-header">
            <h3 className="h3">{t('stats.timePerSubject')}</h3>
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
          {trends.map((trend, i) => (
            <div className="trend-card" key={i}>
              <div className={`trend-icon-box ${trend.iconClass}`}>
                <span className="material-symbols-outlined">{trend.icon}</span>
              </div>
              <div>
                <p className="trend-label">{trend.label}</p>
                <h4 className="h3 trend-value text-primary">{trend.value}</h4>
                <p className="label-sm text-on-surface-variant">{trend.sub}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Stats;

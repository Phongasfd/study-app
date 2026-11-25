import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { getProfileSummary, getAchievements } from '../lib/api';
import './Profile.css';

const Profile = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [summary, setSummary] = useState(null);
  const [achievements, setAchievements] = useState([]);

  useEffect(() => {
    if (!user) return;
    Promise.all([getProfileSummary(), getAchievements()])
      .then(([sum, list]) => {
        setSummary(sum);
        setAchievements(list || []);
      })
      .catch((err) => console.error('Failed to load profile:', err));
  }, [user]);

  const totalHours = summary ? (summary.totalSeconds / 3600).toFixed(1) : '0.0';

  return (
    <div className="profile-page">
      <header className="profile-hero-card">
        <div className="profile-identity">
          <img
            src={`https://ui-avatars.com/api/?name=${encodeURIComponent(user?.username || 'S')}&background=1a3c34&color=fff`}
            alt={user?.username}
            className="profile-hero-avatar"
          />
          <div>
            <h1 className="h2 text-primary">{summary?.username || user?.username || 'Scholar'}</h1>
            <p className="body-md text-on-surface-variant">{t('profile.subtitle')}</p>
          </div>
        </div>

        <div className="streak-banner">
          <div className="streak-pill active-streak">
            <span className="material-symbols-outlined filled">local_fire_department</span>
            <div>
              <div className="streak-num">{summary?.currentStreak ?? 0} {t('profile.days')}</div>
              <div className="streak-lbl">{t('profile.currentStreak')}</div>
            </div>
          </div>
          <div className="streak-pill">
            <span className="material-symbols-outlined">military_tech</span>
            <div>
              <div className="streak-num">{summary?.longestStreak ?? 0} {t('profile.days')}</div>
              <div className="streak-lbl">{t('profile.longestStreak')}</div>
            </div>
          </div>
        </div>
      </header>

      <section className="profile-metrics-grid">
        <div className="profile-metric-card">
          <span className="material-symbols-outlined">schedule</span>
          <div className="metric-num">{totalHours}h</div>
          <div className="metric-lbl">{t('profile.totalHours')}</div>
        </div>
        <div className="profile-metric-card">
          <span className="material-symbols-outlined">timelapse</span>
          <div className="metric-num">{summary?.completedSessions ?? 0}</div>
          <div className="metric-lbl">{t('profile.sessionsCompleted')}</div>
        </div>
        <div className="profile-metric-card">
          <span className="material-symbols-outlined">task_alt</span>
          <div className="metric-num">{summary?.tasksDone ?? 0}</div>
          <div className="metric-lbl">{t('profile.tasksCompleted')}</div>
        </div>
        <div className="profile-metric-card">
          <span className="material-symbols-outlined">emoji_events</span>
          <div className="metric-num">
            {summary?.achievementsUnlocked ?? 0} / {summary?.achievementsTotal ?? achievements.length}
          </div>
          <div className="metric-lbl">{t('profile.achievementsTitle')}</div>
        </div>
      </section>

      <section className="achievements-section">
        <h2 className="h3 text-primary">{t('profile.achievementsTitle')}</h2>
        <div className="achievements-grid">
          {achievements.map((ach) => (
            <div
              key={ach.code}
              className={`achievement-card ${ach.unlocked ? 'unlocked' : 'locked'}`}
            >
              <div className="achievement-icon-wrap">
                <span className={`material-symbols-outlined ${ach.unlocked ? 'filled' : ''}`}>
                  {ach.icon || 'emoji_events'}
                </span>
              </div>
              <div className="achievement-info">
                <div className="achievement-top">
                  <h4 className="achievement-name">{ach.name}</h4>
                  <span className={`achievement-badge ${ach.unlocked ? 'unlocked' : ''}`}>
                    {ach.unlocked ? t('profile.unlocked') : t('profile.locked')}
                  </span>
                </div>
                <p className="achievement-desc">{ach.description}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default Profile;

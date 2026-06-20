import { useState, useEffect } from 'react';
import { getSubjects, createSubject, deleteSubject } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import './Pages.css';
import { useTimer } from '../context/TimerContext';
import { useTranslation } from 'react-i18next';

const Home = () => {
  const {
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
  } = useTimer();

  const { user } = useAuth();
  const { t } = useTranslation();
  const [subjects, setSubjects] = useState([]); // for subjects
  const [isAdding, setIsAdding] = useState(false); // for adding subjects
  const [newSubjectName, setNewSubjectName] = useState(''); // for new subject name

  useEffect(() => {
    if (user) {
      fetchSubjects();
    } else {
      setSubjects([]);
    }
  }, [user]); // when user is change, call fetchSubjects or setSubjects to []

  const fetchSubjects = async () => {
    try {
      const data = await getSubjects();
      setSubjects(data);
    } catch (error) {
      console.error('Failed to fetch subjects:', error);
    }
  };

  const handleAddSubject = async (e) => {
    if (e.key === 'Enter' && newSubjectName.trim()) {
      try {
        await createSubject(newSubjectName.trim());
        // after adding subject 
        setNewSubjectName('');
        setIsAdding(false);
        fetchSubjects();
      } catch (error) {
        console.error('Failed to add subject:', error);
      }
    } else if (e.key === 'Escape') {
      setIsAdding(false);
      setNewSubjectName('');
    }
  };

  const handleDeleteSubject = async (id) => {
    try {
      await deleteSubject(id);
      fetchSubjects();
    } catch (error) {
      console.error('Failed to delete subject:', error);
    }
  };

  return (
    <div className="focus-container home-page">
      {/* Context Badge */}
      <div className="context-badge">
        <span className="badge-dot"></span>
        {t('home.contextBadge')}
      </div>

      {/* Central Timer Component */}
      <section className="timer-section">
        <div className="timer-circle-container">
          <div className="timer-track"></div>
          <svg className="timer-svg" viewBox="0 0 100 100">
            <circle 
              cx="50" 
              cy="50" 
              r="48" 
              className="timer-progress"
              style={{ strokeDashoffset }}
            ></circle>
          </svg>
          <div className="timer-display">
            <span className="timer-text">{formatTime(timeElapsed)}</span>
            <span className="timer-label">{activeSubject ? activeSubject.name : t('home.noSubject')}</span>
          </div>
        </div>

        {/* Interaction Controls */}
        <div className="timer-controls">
          <button className="btn-start-focus" onClick={() => {
            if (isActive) {
              pause();
            } else if (activeSubject) {
              resume();
            }
          }}>
            <span className="material-symbols-outlined filled">
              {isActive ? 'pause' : 'play_arrow'}
            </span>
            {isActive ? t('home.pauseFocus') : t('home.resumeFocus')}
          </button>
          
        </div>
      </section>

      {/* Subjects Section */}
      <section className="subjects-section">
        <div className="subjects-header">
          <h3 className="h3 section-title">{t('home.mySubjects')}</h3>
        </div>
        <div className="subjects-row">
          {subjects.map((subject) => (
            <div key={subject.id} className="subject-chip">
              <button
                className="delete-subject-btn"
                onClick={() => handleDeleteSubject(subject.id)}
              >
                <span className="material-symbols-outlined">delete</span>
              </button>
              <span className="subject-name">{subject.name}</span>
              <button 
                className="subject-play-btn"
                onClick={() => {
                  start(subject);
                }}
              >
                <span className="material-symbols-outlined filled">play_arrow</span>
              </button>
            </div>
          ))}

          {isAdding ? (
            <div className="subject-chip add-subject-input-container">
                <input
                autoFocus
                className="add-subject-input"
                placeholder={t('home.addSubjectPlaceholder')}
                value={newSubjectName}
                onChange={(e) => setNewSubjectName(e.target.value)}
                onKeyDown={handleAddSubject}
                onBlur={() => {
                  if (!newSubjectName.trim()) setIsAdding(false); // when user click outside the input field, close it
                }}
              />
            </div>
          ) : (
            <button
              className="subject-chip add-subject-btn"
              onClick={() => {
                if (!user) {
                  window.location.href = "/login";
                } else {
                  setIsAdding(true)
                }

              }}
            >
              <span className="material-symbols-outlined subject-icon">add</span>
              {t('home.addSubject')}
            </button>
          )}
        </div>
      </section>
    </div>
  );
};

export default Home;

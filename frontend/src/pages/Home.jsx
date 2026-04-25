import './Pages.css';

const Home = () => {
  return (
    <div className="focus-container home-page">
      {/* Context Badge */}
      <div className="context-badge">
        <span className="badge-dot"></span>
        Deep Focus Session
      </div>

      {/* Central Timer Component */}
      <section className="timer-section">
        <div className="timer-circle-container">
          <div className="timer-track"></div>
          <svg className="timer-svg" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="48" className="timer-progress"></circle>
          </svg>
          <div className="timer-display">
            <span className="timer-text">25:00</span>
            <span className="timer-label">Advanced Calculus</span>
          </div>
        </div>

        {/* Interaction Controls */}
        <div className="timer-controls">
          <button className="icon-btn-large">
            <span className="material-symbols-outlined">settings</span>
          </button>
          <button className="btn-start-focus">
            <span className="material-symbols-outlined filled">play_arrow</span>
            Start Focus
          </button>
          <button className="icon-btn-large">
            <span className="material-symbols-outlined">refresh</span>
          </button>
        </div>
      </section>

      {/* Subjects Section */}
      <section className="subjects-section">
        <div className="subjects-header">
          <h3 className="h3 section-title">My Subjects</h3>
        </div>
        <div className="subjects-row">
          <div className="subject-chip">
            <span className="material-symbols-outlined subject-icon">functions</span>
            <span className="subject-name">Advanced Calculus</span>
            <button className="subject-play-btn">
              <span className="material-symbols-outlined filled">play_arrow</span>
            </button>
          </div>
          <div className="subject-chip">
            <span className="material-symbols-outlined subject-icon">menu_book</span>
            <span className="subject-name">Molecular Biology</span>
            <button className="subject-play-btn">
              <span className="material-symbols-outlined filled">play_arrow</span>
            </button>
          </div>
          <div className="subject-chip">
            <span className="material-symbols-outlined subject-icon">code</span>
            <span className="subject-name">Algorithm Design</span>
            <button className="subject-play-btn">
              <span className="material-symbols-outlined filled">play_arrow</span>
            </button>
          </div>
          <button className="subject-chip add-subject-btn">
            <span className="material-symbols-outlined subject-icon">add</span>
            Add Subject
          </button>
        </div>
      </section>
    </div>
  );
};

export default Home;

import './Stats.css';

const Stats = () => {
  return (
    <div className="stats-page">
      {/* Statistics Header */}
      <div className="stats-header">
        <div className="stats-title-group">
          <h1 className="h1 text-primary">Performance Insights</h1>
          <p className="body-md text-on-surface-variant">
            You've studied for <span className="text-primary font-bold">32.4 hours</span> this week.
          </p>
        </div>
        <div className="time-toggle">
          <button className="toggle-btn active">Weekly</button>
          <button className="toggle-btn">Monthly</button>
        </div>
      </div>

      {/* Bento Grid Layout */}
      <div className="bento-grid">
        {/* Weekly Chart Card */}
        <div className="bento-card col-span-8">
          <div className="card-header">
            <h3 className="h3">Study Hours This Week</h3>
            <div className="legend">
              <div className="legend-dot"></div>
              <span className="label-sm text-on-surface-variant">Actual Hours</span>
            </div>
          </div>
          
          <div className="chart-container">
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '60%' }}>
                  <div className="tooltip">4.2h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Mon</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '85%' }}>
                  <div className="tooltip">6.0h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Tue</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '45%' }}>
                  <div className="tooltip">3.1h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Wed</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '70%' }}>
                  <div className="tooltip">4.9h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Thu</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '95%' }}>
                  <div className="tooltip">6.7h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Fri</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '40%' }}>
                  <div className="tooltip">2.8h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Sat</span>
            </div>
            <div className="bar-group">
              <div className="bar-track">
                <div className="bar-fill" style={{ height: '65%' }}>
                  <div className="tooltip">4.6h</div>
                </div>
              </div>
              <span className="label-sm text-on-surface-variant">Sun</span>
            </div>
          </div>
        </div>

        {/* Daily Goal Ring Card */}
        <div className="bento-card col-span-4 daily-goal-card">
          <h3 className="h3">Daily Goal</h3>
          <div className="goal-ring-container">
            <svg className="goal-svg">
              <circle className="goal-track" cx="96" cy="96" r="80"></circle>
              <circle className="goal-progress" cx="96" cy="96" r="80"></circle>
            </svg>
            <div className="goal-center">
              <span className="h1">75%</span>
              <span className="label-sm uppercase opacity-80">Complete</span>
            </div>
          </div>
          <div className="goal-text">
            <p className="body-md text-primary-fixed">4.5h / 6h studied today</p>
            <p className="label-sm text-primary-fixed-dim mt-1">Keep it up! 1.5h to go.</p>
          </div>
        </div>

        {/* Time per Subject Breakdown */}
        <div className="bento-card col-span-12">
          <div className="card-header">
            <h3 className="h3">Time per Subject</h3>
            <button className="view-report-btn">
              View Detailed Report
              <span className="material-symbols-outlined icon-sm">arrow_forward</span>
            </button>
          </div>
          
          <div className="subject-grid">
            {/* Math */}
            <div className="subject-item">
              <div className="subject-header">
                <div className="subject-title-wrapper">
                  <div className="subject-icon-box bg-orange-light text-orange">
                    <span className="material-symbols-outlined">functions</span>
                  </div>
                  <span className="h3 subject-title">Math</span>
                </div>
                <span className="font-bold text-on-surface-variant">12.5h</span>
              </div>
              <div className="progress-bar-bg">
                <div className="progress-bar-fill bg-orange" style={{ width: '38%' }}></div>
              </div>
              <p className="label-sm text-on-surface-variant">Algebra & Calculus focuses</p>
            </div>

            {/* Science */}
            <div className="subject-item">
              <div className="subject-header">
                <div className="subject-title-wrapper">
                  <div className="subject-icon-box bg-blue-light text-blue">
                    <span className="material-symbols-outlined">biotech</span>
                  </div>
                  <span className="h3 subject-title">Science</span>
                </div>
                <span className="font-bold text-on-surface-variant">10.2h</span>
              </div>
              <div className="progress-bar-bg">
                <div className="progress-bar-fill bg-blue" style={{ width: '31%' }}></div>
              </div>
              <p className="label-sm text-on-surface-variant">Quantum Physics modules</p>
            </div>

            {/* Language */}
            <div className="subject-item">
              <div className="subject-header">
                <div className="subject-title-wrapper">
                  <div className="subject-icon-box bg-emerald-light text-emerald">
                    <span className="material-symbols-outlined">translate</span>
                  </div>
                  <span className="h3 subject-title">Language</span>
                </div>
                <span className="font-bold text-on-surface-variant">9.7h</span>
              </div>
              <div className="progress-bar-bg">
                <div className="progress-bar-fill bg-emerald" style={{ width: '29%' }}></div>
              </div>
              <p className="label-sm text-on-surface-variant">Mandarin vocabulary & grammar</p>
            </div>
          </div>
        </div>

        {/* Focus Trends */}
        <div className="col-span-12 trends-grid">
          <div className="trend-card">
            <div className="trend-icon-box bg-secondary-container text-on-secondary-container">
              <span className="material-symbols-outlined">bolt</span>
            </div>
            <div>
              <p className="trend-label">Peak Focus Time</p>
              <h4 className="h3 trend-value text-primary">8:00 AM — 11:00 AM</h4>
              <p className="label-sm text-on-surface-variant">You're 24% more efficient in the morning.</p>
            </div>
          </div>
          <div className="trend-card">
            <div className="trend-icon-box bg-tertiary-fixed text-on-tertiary-fixed">
              <span className="material-symbols-outlined">restaurant</span>
            </div>
            <div>
              <p className="trend-label">Suggested Break</p>
              <h4 className="h3 trend-value text-primary">In 45 minutes</h4>
              <p className="label-sm text-on-surface-variant">Schedule a 10-min hydration break soon.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Stats;

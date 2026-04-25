import './Groups.css';

const Groups = () => {
  return (
    <div className="groups-page">
      {/* My Groups Section */}
      <section className="groups-section">
        <div className="section-header">
          <h2 className="h3 text-on-background">My Groups</h2>
          <button className="create-group-btn">
            <span className="material-symbols-outlined">add</span>
            Create New
          </button>
        </div>
        
        <div className="groups-grid">
          {/* Group Card 1 */}
          <div className="group-card">
            <div className="group-card-header">
              <div className="icon-badge bg-primary-container text-white">
                <span className="material-symbols-outlined">auto_stories</span>
              </div>
              <span className="status-badge bg-secondary-container text-on-secondary-container">Active</span>
            </div>
            <h3 className="body-lg font-h h3-margin">Advanced Calculus II</h3>
            <div className="group-stats">
              <div className="stat-item">
                <span className="material-symbols-outlined icon-sm">group</span>
                <span className="body-md text-sm">12 Members</span>
              </div>
              <div className="stat-item">
                <span className="material-symbols-outlined icon-sm">schedule</span>
                <span className="body-md text-sm">4.5h Daily Avg</span>
              </div>
            </div>
            <button className="btn-enter">
              Enter Group
              <span className="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>

          {/* Group Card 2 */}
          <div className="group-card">
            <div className="group-card-header">
              <div className="icon-badge bg-tertiary-container text-white">
                <span className="material-symbols-outlined">history_edu</span>
              </div>
            </div>
            <h3 className="body-lg font-h h3-margin">Renaissance Art History</h3>
            <div className="group-stats">
              <div className="stat-item">
                <span className="material-symbols-outlined icon-sm">group</span>
                <span className="body-md text-sm">8 Members</span>
              </div>
              <div className="stat-item">
                <span className="material-symbols-outlined icon-sm">schedule</span>
                <span className="body-md text-sm">2.8h Daily Avg</span>
              </div>
            </div>
            <button className="btn-enter">
              Enter Group
              <span className="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>

          {/* Group Card 3 (Bento Style Variation) */}
          <div className="group-card-featured">
            <div>
              <div className="featured-badge">
                <span className="material-symbols-outlined filled text-secondary-container">star</span>
                <span className="label-sm uppercase text-on-primary-container tracking-wider">Top Performing</span>
              </div>
              <h3 className="h3 h3-margin">Deep Work: LSAT Prep</h3>
              <p className="body-md text-sm text-on-primary-container">You're in the top 5% of contributors this week.</p>
            </div>
            <div className="featured-bottom">
              <div>
                <div className="featured-value text-secondary-container">6.2h</div>
                <div className="featured-label text-on-primary-container">Daily Avg</div>
              </div>
              <button className="btn-featured-enter">
                <span className="material-symbols-outlined">login</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Trending Groups Section */}
      <section className="groups-section">
        <div className="section-header-wrap">
          <h2 className="h3 text-on-background">Trending Groups</h2>
          <div className="filter-controls">
            <div className="search-bar-wrap">
              <span className="material-symbols-outlined search-icon">search</span>
              <input type="text" className="search-input" placeholder="Find by subject or interest..." />
            </div>
            <button className="filter-btn">
              <span className="material-symbols-outlined">tune</span>
              Filter
            </button>
          </div>
        </div>

        <div className="trending-grid">
          {/* Trending Card 1 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBquzG03V5HT1Uc3eqQkbeUKsiTtCUWt1cGIQu2Ci3LnFteI9GvrfBsHeNpRZ1OacHjrSHFHqrVGUSOnmc5_bpQglD0dF4t6ve1KsbIMo5r-uh8EwyBXQVYl-vN-QqG2-rQYCC94H-f00aoS-bRGeDe_LXvF-oAHRuU-XDu-xn7sEu37AA5oHT97O--WSNicbZF_IpO-5KVIYU6n3_Nt75H4ISOKauQ_SbOnmPFNBTdxOd9mgFL9WxEDlVgi-1DLSMdwI4x3o6vEYJY" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-tertiary-fixed text-on-tertiary-fixed">Science</span>
                  <span className="tag-text">• 1.2k members</span>
                </div>
                <h4 className="body-lg font-h trending-title">The Feynman Method Hive</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Explaining complex concepts simply. Join daily sprint sessions.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400"></div>
                  <div className="avatar-sm bg-slate-500 avatar-count">+1k</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 2 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuASucBs-xUAliFo3UUd-0EjlvfnEZVOzj_p-KhCBvK3DbYrmvDDNNUBbQlWD45ie5UZe0RDUqIzLxLLwP9ArjDc0lkAt3zsaWG67G3AIXNYNEMiau3R9FCG91vuhUEdB7YEJw9etckQSRRQLUp0Ga7eGYRlYqSo46O44lKbDZpnXBsbU63xjnI1erwhYdK3vhhKcDVAtxqbihxKx6JcqAx5SKu7sWv_QiJN-QmDehg1SgCVSohTYkntDxQDoUvON-41Lampu40TqioT" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-primary-fixed text-on-primary-fixed">Code</span>
                  <span className="tag-text">• 840 members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Algorithm Architects</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Daily LeetCode grind and system design discussions.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400 avatar-count">+2</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 3 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBRnF6sRy_st2k9PQv0NDjGR-X0QZP__iYh1DhFU4gPYcxg0FufoW_dLqfIKySsu5-_liib3D02ckUN7_8oEYe_dNku0TkAKkMHPDFeJZbvcnfQ0edWxet1rEgO4bdU67wDQiejRXTWT2Le1sJi4D8NWhE-ByPqz-FZ1NjY2wsqJCuQcqPotAcQhutrN2tD_QMOr74rMSKoVdaB7lpA4YcZJSbtjUGpEVtOC-IrC_B9o2Gbh64OYmEK6KTJ9DsluUq_yTEiOSYbwm9c" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-secondary-fixed text-on-secondary-fixed">Philosophy</span>
                  <span className="tag-text">• 256 members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Stoic Study Hall</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Reading through the meditations and practicing daily reflection.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>

          {/* Trending Card 4 */}
          <div className="trending-card">
            <div className="trending-img-wrap">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAy3I48OfwbMDmtr4gGtJPPWTcBn8_j9hu-xzuSauF_GXqdP_wTk0ArE29UwaU_ORcWNLYnPEoMJbD_Uf_4gE1Hsm1hf0tWHG0Yeuytbn--R5ixGDBEqpRxj08-wNkxT13KCYJ22WjZtUHTfJzBBrSEHPG1Eg3IDlU5OcvewphcTbqpV0pt-puSJgW1Pj6ux5SLiOVOh14HWKAHONIdSW4tCy0Nqy4T0C8Q6tFtywfQ8qnDUbSkbeba9oEzEgw5wfg3QwUyKcHgUq7n" alt="Trending Group" className="trending-img" />
            </div>
            <div className="trending-content">
              <div>
                <div className="trending-tags">
                  <span className="tag-badge bg-surface-variant text-on-surface-variant">Focus</span>
                  <span className="tag-text">• 4.5k members</span>
                </div>
                <h4 className="body-lg font-h trending-title">Pomodoro Global</h4>
                <p className="body-md text-sm text-on-surface-variant line-clamp-2">Synchronized pomodoro sessions with ambient soundscapes.</p>
              </div>
              <div className="trending-footer">
                <div className="avatar-group">
                  <div className="avatar-sm bg-slate-200"></div>
                  <div className="avatar-sm bg-slate-300"></div>
                  <div className="avatar-sm bg-slate-400"></div>
                  <div className="avatar-sm bg-slate-500 avatar-count">+4k</div>
                </div>
                <button className="btn-join">Join</button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Groups;

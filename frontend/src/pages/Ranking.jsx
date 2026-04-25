import './Ranking.css';

const Ranking = () => {
  return (
    <div className="ranking-page">
      {/* Header Section */}
      <div className="ranking-header">
        <h2 className="h2 text-primary ranking-title">Leaderboard</h2>
        <p className="body-md text-on-surface-variant">Celebrating the discipline of deep focus this week.</p>
      </div>

      {/* Ranking Toggle */}
      <div className="ranking-toggle-container">
        <button className="toggle-pill active">Group Ranking</button>
        <button className="toggle-pill">Global Ranking</button>
      </div>

      {/* Podium Section (Top 3) */}
      <div className="podium-section">
        {/* 2nd Place */}
        <div className="podium-item place-2">
          <div className="avatar-wrapper">
            <div className="avatar-circle border-slate">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBPOvCOETwtzsvhEEEWYWNxFbzCcTEeNEzZrhsTS2OI5fFV1mGGode6b0wLtby_HPjhgrkNSncf0X6qCbq_hqQiyjCvIPf9_kS09n4c6d2MXl4QSklFFGogpJNpBJCywseakPIBJmGmBMjYjCtyPEWwoXyCaV4TlIjU_Lxv8ggcSgNydj5h5QE3ZQjGdltiQ8ApDytiMAEb9CiUqxM-RDlm1sX0yc9DJcYPaXjrfgNeljYgfueI5NtJPsRrcmZvFyULmQf5x_CV6byP" alt="Sarah" />
            </div>
            <div className="rank-badge bg-slate-200">
              <span>2</span>
            </div>
          </div>
          <span className="label-sm text-primary name-truncate">Sarah L.</span>
          <span className="label-sm text-on-surface-variant text-xs">32.5h</span>
        </div>

        {/* 1st Place */}
        <div className="podium-item place-1">
          <div className="avatar-wrapper scale-110">
            <div className="avatar-circle border-secondary lg-avatar">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuA6JQrxIjN4tJ-h-d4nhh0a-YI9aWpJOJkVLvWzJ8uIMvEdABgWgqz26HHayJVxqDjn-LzPV5sBRLq3G5OWeq9XXuUGKQ4pnlFL9H3mNBkKoTTTUCsro2hsX7ByTFJrjBB9LE6C6Vf61V_grvCIpX6EcKf-oD-wtssT7VZVq_PvmCphgTXBOC9qSpSqwHfpcJyNlYDvS5CJHhW4wX5VCYMdR2xTs1Uf1I0BKPM14ujdFwov2dZbKiOHyV035xda72tZjjmacgB7ZNBS" alt="Alex" />
            </div>
            <div className="rank-badge bg-secondary-container lg-badge">
              <span className="material-symbols-outlined filled text-on-secondary-container icon-sm">workspace_premium</span>
            </div>
          </div>
          <span className="h3 text-primary name-truncate">Alex Rivers</span>
          <span className="score-badge bg-primary-container text-secondary-container">41.2h</span>
        </div>

        {/* 3rd Place */}
        <div className="podium-item place-3">
          <div className="avatar-wrapper">
            <div className="avatar-circle border-tertiary">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAN3xJ4y9Uld08a_ONU7xrLTqrL6nnEnwJH_LEgn_Hrkp_MMS_K89BeUAK30-YB4TncmCPiedqqS_fXyO0bxsVeMhUHOQ11OkB-TDTQSC6h2BClAtx1cSiF2GauKR1bmtu0FD9g9dgrskKI4iljTLDpmab4ZWLS68rJbuz0mNJP_earR1K1R5BkbAVqtbRfOJkNBTMArt5czGtiSvqkBs1YrmJct6EEjQXjgOqAuEarD_YzPgbhv8B-NnYpDr-DEv7fhkBlh1s_GzXw" alt="Maya" />
            </div>
            <div className="rank-badge bg-tertiary">
              <span className="text-on-tertiary-fixed-variant">3</span>
            </div>
          </div>
          <span className="label-sm text-primary name-truncate">Maya J.</span>
          <span className="label-sm text-on-surface-variant text-xs">30.8h</span>
        </div>
      </div>

      {/* List Section */}
      <div className="ranking-list">
        {/* User Rank 4 */}
        <div className="rank-list-item hover-scale">
          <div className="rank-item-left">
            <span className="h3 rank-number text-slate-300">4</span>
            <div className="list-avatar">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBpkeBnK36O6_Brl2R8udc_sGo1WZt4xE_1M87jjuv2sRG1OhWxhQpCvoL5YlEyL14vZesxqMxgMLN0lNkcN9o0Dl03MHVAB3IKl2LYXNdhj0y_-AkpLwh7XcaviNks1Rfy-mOaFqZobxjXrzpFLyOpli79cheSigTISiKcOPyUQN2u8a7WXMxbkJF5vFrEMars6WfC8TPwM5TQaqB8btdDuBcUQD83UhX2JAVxuyu6zs4E8DNPW4ADbF0d975KYrrUI2CqALTKBiiR" alt="James" />
            </div>
            <div>
              <p className="label-sm text-primary">James Wilson</p>
              <p className="text-xs text-on-surface-variant mt-1">Focus Streak: 5 days</p>
            </div>
          </div>
          <div className="rank-item-right">
            <p className="label-sm text-primary text-right">28.4h</p>
            <div className="mini-progress-bg mt-1">
              <div className="mini-progress-fill bg-primary w-75"></div>
            </div>
          </div>
        </div>

        {/* CURRENT USER HIGHLIGHT */}
        <div className="rank-list-item current-user-item">
          <div className="highlight-border"></div>
          <div className="rank-item-left relative-z">
            <span className="h3 rank-number text-secondary-container">5</span>
            <div className="list-avatar border-secondary-dim">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuCR_bPgtjCn-E0VOzUVO7zEnzICeswLIWiRaz2de-vksrCRqghhZL9B1RQ0X2MohlgK8mwxoLntHNxMXHeLroGMHF5lBbn_KPhd_fvGELof80h2QIr5uS9B5Qyft78iLx-93oLb06RYhh1QCAG2_1XsiP6dOXLUDSV67qewihSCx1p6YaUVcuLvt4iwtppRGGgEiBVeXCzdzaYZLd2yPw4nmXpUChTT5uC1-gkp_xvDyEsc9bZ6JzZgK-joFi4N4Gcmvx88GKw3UIJg" alt="You" />
            </div>
            <div>
              <p className="label-sm text-white flex-row-gap">
                You
                <span className="current-badge">CURRENT</span>
              </p>
              <p className="text-xs text-on-primary-container mt-1">Keep it up, you're in the top 10%!</p>
            </div>
          </div>
          <div className="rank-item-right relative-z">
            <p className="label-sm text-secondary-container text-lg font-bold text-right">26.1h</p>
            <p className="text-xxs text-on-primary-container uppercase tracking-wider text-right">Weekly Total</p>
          </div>
        </div>

        {/* User Rank 6 */}
        <div className="rank-list-item">
          <div className="rank-item-left">
            <span className="h3 rank-number text-slate-300">6</span>
            <div className="list-avatar">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuAuJBxg7DD0HPA61LcaTZhfCsHlMYOnPL-cYYq6I-5BX921Hgl5YqGaxo_IFdZ6Ir5pYxOEJWmwdnkxNhabCWwi-Y9JgqLoVtfIylAZ4AXZsBefVR99kqObn4ZjaEdR66RK_V1O23Jtvfo-iOoAeqXis9j6gOz7Yp26NCtCu7q6xuwA83Ao4jhz2WExWtrnOP4n5bT3EzlFbMZDFPCkcAreMzNxmR353kXcJ-GzH90SHZPJ11qstdN2Id22h_SKP5K29nUhOJxDHAze" alt="Chloe" />
            </div>
            <div>
              <p className="label-sm text-primary">Chloe Chen</p>
              <p className="text-xs text-on-surface-variant mt-1">Focus Streak: 2 days</p>
            </div>
          </div>
          <div className="rank-item-right">
            <p className="label-sm text-primary text-right">24.7h</p>
            <div className="mini-progress-bg mt-1">
              <div className="mini-progress-fill bg-primary w-66"></div>
            </div>
          </div>
        </div>

        {/* User Rank 7 */}
        <div className="rank-list-item opacity-80">
          <div className="rank-item-left">
            <span className="h3 rank-number text-slate-300">7</span>
            <div className="list-avatar">
              <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuBWLssV9feslvHOzNA1FRXPi4gLbyNYphrYLm0PyCXN5H0mQoyyBCSZm7mZxolwW6Xb5Zp0Tw8sx37S8TLmk1pWUYBm4rgrGl-S66Cgsxd0GOjA89vpiKcO4l686e8yiZZYp5yPIwFteIxO6lCC4r6P8pUihOdpz4L2DnEdpmvdlh5EmRJ0WXZKl-aFj7KizzlEA0KlxxkUnmqwlHvBy6e9OdUoqCgq12cj1botbWB3h_U0PsIos6pwJYQqWn_445Q105d1-PTyKZKl" alt="Daniel" />
            </div>
            <div>
              <p className="label-sm text-primary">Daniel Kim</p>
              <p className="text-xs text-on-surface-variant mt-1">Focus Streak: 0 days</p>
            </div>
          </div>
          <div className="rank-item-right">
            <p className="label-sm text-primary text-right">22.9h</p>
            <div className="mini-progress-bg mt-1">
              <div className="mini-progress-fill bg-primary w-50"></div>
            </div>
          </div>
        </div>
      </div>

      {/* Motivational Quote Footer */}
      <div className="quote-footer">
        <span className="material-symbols-outlined quote-icon">format_quote</span>
        <p className="body-md text-on-surface-variant quote-text">"Excellence is not a gift, but a skill that takes practice."</p>
      </div>
    </div>
  );
};

export default Ranking;

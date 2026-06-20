import { Link, useLocation } from 'react-router-dom';
import './NavBar.css';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

const TopAppBar = () => {
  const { user, setUser, loading, logout} = useAuth();
  const location = useLocation();
  const isAuthPage = location.pathname === '/login' || location.pathname === '/signup';
  const { t, i18n } = useTranslation();

  return (
    <header className="top-app-bar">
      <div className="top-app-bar-left">
          <Link to="/" className="app-title-link">
            <span className="app-title">{t('app.title')}</span>
          </Link>
      </div>
      <div className="top-app-bar-right">
        {loading ? null : (
          isAuthPage ? null : (
            user ? (
              <div className="user-profile-info">
                <span className="username-display">{user.username || 'Student'}</span>
                <button onClick={logout} className="btn btn-secondary auth-btn">{t('auth.logout')}</button>
              </div>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary auth-btn">{t('auth.login')}</Link>
                <Link to="/signup" className="btn btn-primary auth-btn">{t('auth.signup')}</Link>
                <div style={{display: 'inline-flex', gap: 8, marginLeft: 8}}>
                  <button className="btn" onClick={() => i18n.changeLanguage('vi')}>VI</button>
                  <button className="btn" onClick={() => i18n.changeLanguage('en')}>EN</button>
                </div>
              </>
            )
          )
        )}
      </div>
    </header>
  );
};

export default TopAppBar;

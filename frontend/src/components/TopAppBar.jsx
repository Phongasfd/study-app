import { Link, useLocation } from 'react-router-dom';
import './NavBar.css';
import { useAuth } from '../context/AuthContext';

const TopAppBar = () => {
  const { user, setUser, loading, logout} = useAuth();
  const location = useLocation();
  const isAuthPage = location.pathname === '/login' || location.pathname === '/signup';

  return (
    <header className="top-app-bar">
      <div className="top-app-bar-left">
          <Link to="/" className="app-title-link">
            <span className="app-title">Study Flow</span>
          </Link>
      </div>
      <div className="top-app-bar-right">
        {loading ? null : (
          isAuthPage ? null : (
            user ? (
              <div className="user-profile-info">
                <span className="username-display">{user.username || 'Student'}</span>
                <button onClick={logout} className="btn btn-secondary auth-btn">Log Out</button>
              </div>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary auth-btn">Log In</Link>
                <Link to="/signup" className="btn btn-primary auth-btn">Sign Up</Link>
              </>
            )
          )
        )}
      </div>
    </header>
  );
};

export default TopAppBar;

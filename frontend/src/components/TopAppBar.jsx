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
        {isAuthPage ? (
          <Link to="/" style={{ textDecoration: 'none', color: 'inherit', display: 'flex', alignItems: 'center' }}>
            <span className="app-title">Study Flow</span>
          </Link>
        ) : (
          <>
            <div className="profile-avatar">
              <img 
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuDltPkwl2_w_CfsMwRVeylg2mMlOc_1FSvnjC2PB7M5D96OKFJC0qzPIDQiir1vZQLLajNXF0XuSSCPyBBdzjGUvvK3KvnB9d6KeIFgJQs9HOvyNbx0MiIbpthk6A8s-7eiKKSdkh-8dd29XeKADoyZ63ncnWs0PUjxWNxjJDG_QsraxMCqYoraIL4fJ8xFo19905eotrh5KB60CHobmuLC6v4uys4FvRegWg2_97NZVnq_H1_Y2G8-AeNYkApKKtF0_oEpqtHyB2AS" 
                alt="Student Profile" 
              />
            </div>
            <span className="app-title">Study Flow</span>
          </>
        )}
      </div>
      <div className="top-app-bar-right" style={{ display: 'flex', gap: '8px' }}>
        {isAuthPage ? null : (
          <>
            <Link to="/login" className="btn btn-secondary" style={{ padding: '8px 16px', fontSize: '13px' }}>Log In</Link>
            <Link to="/signup" className="btn btn-primary" style={{ padding: '8px 16px', fontSize: '13px' }}>Sign Up</Link>
          </>
        )}
      </div>
    </header>
  );
};

export default TopAppBar;

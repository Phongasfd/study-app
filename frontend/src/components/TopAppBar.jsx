import { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import './NavBar.css';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
  deleteNotification,
} from '../lib/api';

const TopAppBar = () => {
  const { user, loading, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isAuthPage = location.pathname === '/login' || location.pathname === '/signup';
  const { t, i18n } = useTranslation();

  const [unreadCount, setUnreadCount] = useState(0);
  const [openBell, setOpenBell] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loadingNotifs, setLoadingNotifs] = useState(false);
  const bellRef = useRef(null);

  useEffect(() => {
    if (!user) {
      setUnreadCount(0);
      setNotifications([]);
      setOpenBell(false);
      return;
    }
    let active = true;
    const poll = async () => {
      try {
        const count = await getUnreadNotificationCount();
        if (active) setUnreadCount(count);
      } catch {
        // silent on background poll
      }
    };
    poll();
    const timer = setInterval(poll, 30000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [user, location.pathname]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) {
        setOpenBell(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggleBell = async () => {
    const next = !openBell;
    setOpenBell(next);
    if (next && user) {
      setLoadingNotifs(true);
      try {
        const page = await getNotifications(0, 15);
        setNotifications(page.items || []);
        setUnreadCount(page.unread ?? 0);
      } catch (err) {
        console.error('Failed to load notifications:', err);
      } finally {
        setLoadingNotifs(false);
      }
    }
  };

  const handleClickNotification = async (item) => {
    if (!item.read) {
      try {
        await markNotificationRead(item.id);
        setNotifications((prev) => prev.map((n) => (n.id === item.id ? { ...n, read: true } : n)));
        setUnreadCount((c) => Math.max(0, c - 1));
      } catch (err) {
        console.error(err);
      }
    }
    setOpenBell(false);
    if (item.link) {
      navigate(item.link);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteNotification = async (e, id) => {
    e.stopPropagation();
    try {
      const target = notifications.find((n) => n.id === id);
      await deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      if (target && !target.read) {
        setUnreadCount((c) => Math.max(0, c - 1));
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <header className="top-app-bar">
      <div className="top-app-bar-left">
        <Link to="/" className="app-title-link">
          <span className="app-title">{t('app.title')}</span>
        </Link>
      </div>
      <div className="top-app-bar-right">
        <div className="lang-switcher">
          <button
            className={`lang-btn ${i18n.language?.startsWith('vi') ? 'active' : ''}`}
            onClick={() => i18n.changeLanguage('vi')}
          >
            VI
          </button>
          <button
            className={`lang-btn ${i18n.language?.startsWith('en') ? 'active' : ''}`}
            onClick={() => i18n.changeLanguage('en')}
          >
            EN
          </button>
        </div>

        {loading ? null : (
          isAuthPage ? null : (
            user ? (
              <div className="user-profile-info">
                <div className="notif-bell-wrap" ref={bellRef}>
                  <button
                    className="notif-bell-btn"
                    onClick={handleToggleBell}
                    aria-label={t('notifications.title')}
                  >
                    <span className="material-symbols-outlined">notifications</span>
                    {unreadCount > 0 && (
                      <span className="notif-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                    )}
                  </button>

                  {openBell && (
                    <div className="notif-dropdown">
                      <div className="notif-dropdown-header">
                        <span className="notif-dropdown-title">{t('notifications.title')}</span>
                        {unreadCount > 0 && (
                          <button className="notif-mark-all-btn" onClick={handleMarkAllRead}>
                            {t('notifications.markAllRead')}
                          </button>
                        )}
                      </div>
                      <div className="notif-dropdown-list">
                        {loadingNotifs ? (
                          <div className="notif-empty">...</div>
                        ) : notifications.length === 0 ? (
                          <div className="notif-empty">{t('notifications.empty')}</div>
                        ) : (
                          notifications.map((item) => (
                            <div
                              key={item.id}
                              className={`notif-item ${item.read ? 'read' : 'unread'}`}
                              onClick={() => handleClickNotification(item)}
                            >
                              <div className="notif-item-body">
                                <p className="notif-item-title">{item.title}</p>
                                {item.message && <span className="notif-item-sub">{item.message}</span>}
                              </div>
                              <button
                                className="notif-delete-btn"
                                onClick={(e) => handleDeleteNotification(e, item.id)}
                                title="Delete"
                              >
                                <span className="material-symbols-outlined">close</span>
                              </button>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>

                <Link to="/profile" className="username-display" style={{ textDecoration: 'none' }}>
                  {user.username || 'Student'}
                </Link>
                <button onClick={logout} className="btn btn-secondary auth-btn">{t('auth.logout')}</button>
              </div>
            ) : (
              <>
                <Link to="/login" className="btn btn-secondary auth-btn">{t('auth.login')}</Link>
                <Link to="/signup" className="btn btn-primary auth-btn">{t('auth.signup')}</Link>
              </>
            )
          )
        )}
      </div>
    </header>
  );
};

export default TopAppBar;

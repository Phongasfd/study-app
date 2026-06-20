import { NavLink } from 'react-router-dom';
import './NavBar.css';
import { useTranslation } from 'react-i18next';

const BottomNavBar = () => {
  const { t } = useTranslation();
  const navItems = [
    { path: '/', icon: 'timer', label: t('nav.home') },
    // { path: '/assistant', icon: 'graphic_eq', label: 'AI Voice' },
    { path: '/groups', icon: 'group', label: t('nav.groups') },
    { path: '/stats', icon: 'bar_chart', label: t('nav.stats') },
  ];

  return (
    <nav className="bottom-nav-bar">
      {navItems.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          className={({ isActive }) => 
            `nav-item ${isActive ? 'nav-item-active' : ''}`
          }
        >
          {({ isActive }) => (
            <>
              <span 
                className={`material-symbols-outlined nav-icon ${isActive ? 'filled' : ''}`}
              >
                {item.icon}
              </span>
              <span className="nav-label">{item.label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
};

export default BottomNavBar;

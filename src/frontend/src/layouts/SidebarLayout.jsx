import { BsBoxArrowRight, BsCalendar2WeekFill, BsPeopleFill } from 'react-icons/bs'
import { NavLink, Outlet } from 'react-router-dom'
import keycloak from '../auth/keycloak.js'
import styles from './SidebarLayout.module.css'

const NAV_ITEMS = [
  { to: '/patients', label: 'Patients', icon: <BsPeopleFill aria-hidden="true" /> },
  { to: '/appointments', label: 'Appointments', icon: <BsCalendar2WeekFill aria-hidden="true" /> },
]

function SidebarLayout() {
  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandBadge}>S</span>
          <div>
            <strong>SES Clinic</strong>
            <p>Control Center</p>
          </div>
        </div>

        <nav className={styles.nav} aria-label="Main navigation">
          {NAV_ITEMS.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`.trim()
              }
            >
              <span className={styles.navIcon}>{icon}</span>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className={styles.sidebarFooter}>
          <span className={styles.username}>{keycloak.tokenParsed?.preferred_username}</span>
          <button
            type="button"
            className={styles.logoutBtn}
            onClick={() => keycloak.logout({ redirectUri: window.location.origin })}
            title="Log out"
          >
            <BsBoxArrowRight aria-hidden="true" />
            Log out
          </button>
        </div>
      </aside>

      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  )
}

export default SidebarLayout

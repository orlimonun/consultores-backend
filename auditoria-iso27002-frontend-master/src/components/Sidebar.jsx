import { NavLink, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

const links = [
    { to: '/app', label: 'Dashboard', icon: '◈', end: true },
    { to: '/app/organizaciones', label: 'Organizaciones', icon: '⌘' },
    { to: '/app/controles', label: 'Controles ISO', icon: '☰' },
    { to: '/app/auditorias', label: 'Auditorías', icon: '✓' },
    { to: '/app/resultados', label: 'Resultados', icon: '▤' },
    { to: '/app/historico', label: 'Histórico', icon: '↗' },
    { to: '/app/monitoreo', label: 'Monitoreo', icon: '●' },
    { to: '/app/monitoreo-real', label: 'Monitor Oracle', icon: '◎' },
    { to: '/app/respaldo/bases', label: 'Bases RMAN', icon: '◷', section: 'respaldo' },
    { to: '/app/respaldo/estrategias', label: 'Estrategias', icon: '⊞', section: 'respaldo' },
    { to: '/app/respaldo/historial', label: 'Historial RMAN', icon: '⊟', section: 'respaldo' },
    { to: '/app/respaldo/alertas', label: 'Alertas RMAN', icon: '⚠', section: 'respaldo' },
];

export default function Sidebar() {
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();

    return (
        <aside className="sidebar">
            <Link to="/" className="sidebar-brand mono">
                <span style={{ color: 'var(--green)' }}>&gt;_</span> ISO27002
            </Link>

            <nav className="sidebar-nav">
                {links.filter(l => !l.section).map((link) => (
                    <NavLink
                        key={link.to}
                        to={link.to}
                        end={link.end}
                        className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                    >
                        <span className="sidebar-icon mono">{link.icon}</span>
                        {link.label}
                    </NavLink>
                ))}

                <div className="sidebar-section-label mono" style={{
                    fontSize: '0.65rem', color: 'var(--muted)',
                    padding: '12px 16px 4px', letterSpacing: '0.08em', textTransform: 'uppercase'
                }}>
                    Respaldo RMAN
                </div>
                {links.filter(l => l.section === 'respaldo').map((link) => (
                    <NavLink
                        key={link.to}
                        to={link.to}
                        className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                    >
                        <span className="sidebar-icon mono">{link.icon}</span>
                        {link.label}
                    </NavLink>
                ))}

                {user?.rol === 'admin' && (
                    <NavLink
                        to="/app/usuarios"
                        className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                    >
                        <span className="sidebar-icon mono">◎</span>
                        Usuarios
                    </NavLink>
                )}
            </nav>

            <div className="sidebar-footer">
                <button className="theme-toggle mono" onClick={toggleTheme}>
                    {theme === 'dark' ? '☀ Modo claro' : '☾ Modo oscuro'}
                </button>

                <div className="sidebar-user">
                    <span className="mono">{user?.nombre}</span>
                    <span className="sidebar-role mono">{user?.rol}</span>
                </div>
                <button className="sidebar-logout mono" onClick={logout}>
                    Cerrar sesión
                </button>
            </div>
        </aside>
    );
}
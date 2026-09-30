import { useState, useEffect } from 'react';
import { getHistorial, getEjecucion } from '../../api/respaldo.js';

const RESULTADO_COLOR = {
    EXITOSO:          'var(--green)',
    CON_ADVERTENCIAS: '#f59e0b',
    FALLIDO:          'var(--risk-high)',
    EN_PROGRESO:      '#3b82d4',
    CANCELADO:        'var(--muted)',
};

function fmt(seg) {
    if (!seg) return '—';
    if (seg < 60) return `${seg}s`;
    const m = Math.floor(seg / 60), s = seg % 60;
    return `${m}m ${s}s`;
}

function fmtFecha(iso) {
    if (!iso) return '—';
    return iso.replace('T', ' ').slice(0, 16);
}

export default function HistorialRespaldo() {
    const [ejecuciones, setEjecuciones] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [detalle, setDetalle] = useState(null);
    const [cargandoDetalle, setCargandoDetalle] = useState(false);

    useEffect(() => {
        setLoading(true);
        getHistorial()
            .then(setEjecuciones)
            .catch(() => setError('No se pudo cargar el historial.'))
            .finally(() => setLoading(false));
    }, []);

    const verDetalle = async (id) => {
        setCargandoDetalle(true);
        try { setDetalle(await getEjecucion(id)); }
        catch { setDetalle(null); }
        finally { setCargandoDetalle(false); }
    };

    const badge = (resultado) => {
        const color = RESULTADO_COLOR[resultado] || 'var(--muted)';
        return (
            <span style={{ background: color + '22', color, border: `1px solid ${color}`,
                borderRadius: 4, padding: '2px 8px', fontSize: '0.72rem', fontFamily: 'monospace' }}>
                {resultado}
            </span>
        );
    };

    // KPIs rápidos
    const total    = ejecuciones.length;
    const exitosos = ejecuciones.filter(e => e.resultado === 'EXITOSO').length;
    const fallidos = ejecuciones.filter(e => e.resultado === 'FALLIDO').length;
    const enProg   = ejecuciones.filter(e => e.resultado === 'EN_PROGRESO').length;

    return (
        <div>
            <h1 style={{ marginBottom: 4 }}>Historial de Ejecuciones RMAN</h1>
            <p style={{ color: 'var(--muted)', marginBottom: 24 }}>Evidencia de control preventivo — últimas 50 ejecuciones</p>

            {error && <div className="chart-card" style={{ borderColor: 'var(--risk-high)', marginBottom: 16 }}>
                <p style={{ color: 'var(--risk-high)' }}>{error}</p>
            </div>}

            {/* KPIs */}
            <div className="kpi-grid" style={{ marginBottom: 24 }}>
                {[
                    { label: 'Total ejecuciones', value: total, color: 'var(--text)' },
                    { label: 'Exitosas', value: exitosos, color: 'var(--green)' },
                    { label: 'Fallidas', value: fallidos, color: 'var(--risk-high)' },
                    { label: 'En progreso', value: enProg, color: '#3b82d4' },
                ].map(k => (
                    <div key={k.label} className="kpi-card">
                        <span className="kpi-label mono">{k.label}</span>
                        <span className="kpi-value" style={{ color: k.color }}>{k.value}</span>
                    </div>
                ))}
            </div>

            {loading ? <p style={{ color: 'var(--muted)' }}>Cargando...</p> : (
                <div className="chart-card" style={{ padding: 0, overflow: 'hidden' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ borderBottom: '1px solid var(--border)' }}>
                                {['#', 'Estrategia', 'Base de datos', 'Tipo', 'Inicio', 'Duración', 'Resultado', 'Origen', ''].map(h => (
                                    <th key={h} style={{ padding: '10px 14px', textAlign: 'left', fontSize: '0.78rem', color: 'var(--muted)', fontFamily: 'monospace' }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {ejecuciones.length === 0 && (
                                <tr><td colSpan={9} style={{ padding: 24, color: 'var(--muted)', textAlign: 'center' }}>
                                    Sin ejecuciones registradas aún.
                                </td></tr>
                            )}
                            {ejecuciones.map(e => (
                                <tr key={e.id} style={{ borderBottom: '1px solid var(--border)' }}>
                                    <td style={{ padding: '8px 14px', fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--muted)' }}>#{e.id}</td>
                                    <td style={{ padding: '8px 14px', fontWeight: 600, fontSize: '0.88rem' }}>{e.estrategiaNombre}</td>
                                    <td style={{ padding: '8px 14px', fontSize: '0.82rem', color: 'var(--muted)' }}>{e.baseDatosNombre}</td>
                                    <td style={{ padding: '8px 14px', fontFamily: 'monospace', fontSize: '0.78rem' }}>{e.tipoRespaldo?.replace('_', ' ')}</td>
                                    <td style={{ padding: '8px 14px', fontFamily: 'monospace', fontSize: '0.78rem' }}>{fmtFecha(e.inicio)}</td>
                                    <td style={{ padding: '8px 14px', fontFamily: 'monospace', fontSize: '0.78rem' }}>{fmt(e.duracionSegundos)}</td>
                                    <td style={{ padding: '8px 14px' }}>{badge(e.resultado)}</td>
                                    <td style={{ padding: '8px 14px', fontSize: '0.78rem', color: 'var(--muted)' }}>{e.origen}</td>
                                    <td style={{ padding: '8px 14px' }}>
                                        <button className="btn-secondary" style={{ fontSize: '0.75rem', padding: '3px 10px' }}
                                            onClick={() => verDetalle(e.id)}>
                                            Ver
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal detalle */}
            {(detalle || cargandoDetalle) && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div className="chart-card" style={{ width: 720, maxHeight: '88vh', overflowY: 'auto' }}>
                        {cargandoDetalle ? <p>Cargando...</p> : (
                            <>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
                                    <div>
                                        <h2 style={{ marginBottom: 4 }}>Ejecución #{detalle.id}</h2>
                                        <div style={{ display: 'flex', gap: 8 }}>
                                            {badge(detalle.resultado)}
                                            <span style={{ fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--muted)' }}>{detalle.origen}</span>
                                        </div>
                                    </div>
                                    <button onClick={() => setDetalle(null)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontSize: '1.2rem', color: 'var(--muted)' }}>✕</button>
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 }}>
                                    {[
                                        ['Estrategia', detalle.estrategiaNombre],
                                        ['Base de datos', detalle.baseDatosNombre],
                                        ['Tipo de respaldo', detalle.tipoRespaldo],
                                        ['Inicio', fmtFecha(detalle.inicio)],
                                        ['Fin', fmtFecha(detalle.fin)],
                                        ['Duración', fmt(detalle.duracionSegundos)],
                                        ['Ubicación respaldo', detalle.ubicacionRespaldo || '—'],
                                        ['Agente RMAN', detalle.agenteId || '—'],
                                    ].map(([k, v]) => (
                                        <div key={k}>
                                            <div style={{ fontSize: '0.72rem', color: 'var(--muted)', fontFamily: 'monospace' }}>{k}</div>
                                            <div style={{ fontSize: '0.88rem' }}>{v}</div>
                                        </div>
                                    ))}
                                </div>

                                {detalle.mensajeError && (
                                    <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid var(--risk-high)', borderRadius: 6, padding: 12, marginBottom: 14 }}>
                                        <div style={{ fontSize: '0.75rem', color: 'var(--muted)', marginBottom: 4, fontFamily: 'monospace' }}>ERROR</div>
                                        <pre style={{ margin: 0, fontSize: '0.82rem', color: 'var(--risk-high)', whiteSpace: 'pre-wrap' }}>{detalle.mensajeError}</pre>
                                    </div>
                                )}

                                {detalle.salidaRman && (
                                    <div>
                                        <div style={{ fontSize: '0.75rem', color: 'var(--muted)', fontFamily: 'monospace', marginBottom: 6 }}>SALIDA RMAN (evidencia)</div>
                                        <pre style={{ background: '#0d1117', color: '#8b949e', padding: 16, borderRadius: 6, fontSize: '0.78rem', maxHeight: 300, overflowY: 'auto', border: '1px solid var(--border)' }}>
                                            {detalle.salidaRman}
                                        </pre>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

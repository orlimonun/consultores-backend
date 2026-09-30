import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAlertas } from '../../api/respaldo.js';

const SEV_STYLE = {
    CRITICA:    { bg: 'rgba(239,68,68,0.1)',   border: 'var(--risk-high)',   color: 'var(--risk-high)',   icon: '🔴' },
    ADVERTENCIA:{ bg: 'rgba(245,158,11,0.1)',  border: '#f59e0b',            color: '#f59e0b',            icon: '🟡' },
    INFO:       { bg: 'rgba(59,130,212,0.1)',  border: '#3b82d4',            color: '#3b82d4',            icon: 'ℹ' },
};

const TIPO_LABEL = {
    SIN_PROGRAMACION:    'Sin programación',
    INACTIVA:            'Estrategia inactiva',
    RESPALDO_VENCIDO:    'Respaldo vencido',
    EJECUCION_FALLIDA:   'Ejecución fallida',
    NOARCHIVELOG:        'NOARCHIVELOG',
    SIN_RESPALDO_RECIENTE: 'Sin respaldo reciente',
    SCRIPT_INCOMPLETO:   'Script incompleto',
    SIN_ESPACIO:         'Espacio en disco',
};

export default function AlertasRespaldo() {
    const nav = useNavigate();
    const [alertas, setAlertas] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [filtroSev, setFiltroSev] = useState('');

    const cargar = async () => {
        setLoading(true);
        try { setAlertas(await getAlertas()); }
        catch { setError('No se pudieron cargar las alertas.'); }
        finally { setLoading(false); }
    };

    useEffect(() => { cargar(); }, []);

    const alertasFiltradas = filtroSev ? alertas.filter(a => a.severidad === filtroSev) : alertas;
    const criticas    = alertas.filter(a => a.severidad === 'CRITICA').length;
    const advertencias = alertas.filter(a => a.severidad === 'ADVERTENCIA').length;
    const infos       = alertas.filter(a => a.severidad === 'INFO').length;

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
                <div>
                    <h1>Panel de Control Preventivo</h1>
                    <p style={{ color: 'var(--muted)', marginTop: 4 }}>
                        Alertas activas sobre estrategias de respaldo que requieren atención.
                    </p>
                </div>
                <button className="btn-secondary" onClick={cargar}>↺ Actualizar</button>
            </div>

            {error && <div className="chart-card" style={{ borderColor: 'var(--risk-high)', marginBottom: 16 }}>
                <p style={{ color: 'var(--risk-high)' }}>{error}</p>
            </div>}

            {/* KPIs */}
            <div className="kpi-grid" style={{ marginBottom: 24 }}>
                <div className="kpi-card">
                    <span className="kpi-label mono">Total alertas</span>
                    <span className="kpi-value">{alertas.length}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label mono">Críticas</span>
                    <span className="kpi-value" style={{ color: 'var(--risk-high)' }}>{criticas}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label mono">Advertencias</span>
                    <span className="kpi-value" style={{ color: '#f59e0b' }}>{advertencias}</span>
                </div>
                <div className="kpi-card">
                    <span className="kpi-label mono">Informativas</span>
                    <span className="kpi-value" style={{ color: '#3b82d4' }}>{infos}</span>
                </div>
            </div>

            {/* Filtro */}
            <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
                {['', 'CRITICA', 'ADVERTENCIA', 'INFO'].map(s => (
                    <button key={s} onClick={() => setFiltroSev(s)}
                        style={{ padding: '6px 14px', borderRadius: 4, cursor: 'pointer', fontSize: '0.8rem',
                            background: filtroSev === s ? 'var(--accent)' : 'var(--surface)',
                            border: `1px solid ${filtroSev === s ? 'var(--accent)' : 'var(--border)'}`,
                            color: filtroSev === s ? '#fff' : 'var(--text)' }}>
                        {s || 'Todas'}
                    </button>
                ))}
            </div>

            {loading ? <p style={{ color: 'var(--muted)' }}>Evaluando estrategias...</p> : (
                alertasFiltradas.length === 0 ? (
                    <div className="chart-card" style={{ textAlign: 'center', padding: 40 }}>
                        <div style={{ fontSize: '2rem', marginBottom: 12 }}>✓</div>
                        <p style={{ color: 'var(--green)', fontWeight: 600 }}>Sin alertas activas</p>
                        <p style={{ color: 'var(--muted)', fontSize: '0.85rem', marginTop: 4 }}>
                            Todas las estrategias están en orden.
                        </p>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                        {alertasFiltradas.map((a, i) => {
                            const s = SEV_STYLE[a.severidad] || SEV_STYLE.INFO;
                            return (
                                <div key={i} style={{ background: s.bg, border: `1px solid ${s.border}`, borderRadius: 8, padding: '14px 18px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                                            <span style={{ fontSize: '1.1rem', marginTop: 1 }}>{s.icon}</span>
                                            <div>
                                                <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 4 }}>
                                                    <span style={{ fontWeight: 700, fontSize: '0.88rem', color: s.color }}>
                                                        {TIPO_LABEL[a.tipo] || a.tipo}
                                                    </span>
                                                    <span style={{ fontSize: '0.72rem', color: 'var(--muted)', fontFamily: 'monospace' }}>
                                                        {a.severidad}
                                                    </span>
                                                </div>
                                                <p style={{ margin: 0, fontSize: '0.85rem' }}>{a.mensaje}</p>
                                                <div style={{ marginTop: 6, fontSize: '0.78rem', color: 'var(--muted)' }}>
                                                    {a.baseDatosNombre && <span>📦 {a.baseDatosNombre} · </span>}
                                                    {a.estrategiaNombre && <span>⊞ {a.estrategiaNombre}</span>}
                                                </div>
                                            </div>
                                        </div>
                                        {a.estrategiaId && (
                                            <button className="btn-secondary" style={{ fontSize: '0.78rem', padding: '4px 10px', flexShrink: 0 }}
                                                onClick={() => nav(`/app/respaldo/estrategias/${a.estrategiaId}/editar`)}>
                                                Ir a estrategia
                                            </button>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )
            )}

            {/* Leyenda de tipos */}
            <div className="chart-card" style={{ marginTop: 28, padding: '16px 20px' }}>
                <h4 style={{ marginBottom: 12, fontSize: '0.85rem', color: 'var(--muted)' }}>Tipos de alerta del control preventivo</h4>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    {Object.entries(TIPO_LABEL).map(([k, v]) => (
                        <div key={k} style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                            <span style={{ fontFamily: 'monospace', color: 'var(--text)' }}>{v}</span>
                            {' — '}
                            <span>{{
                                SIN_PROGRAMACION: 'Estrategia activa sin programación configurada',
                                INACTIVA: 'Estrategia marcada como inactiva',
                                RESPALDO_VENCIDO: 'La hora programada pasó y el agente no ejecutó el respaldo',
                                EJECUCION_FALLIDA: 'Fallo registrado en las últimas 24 horas',
                                NOARCHIVELOG: 'Base en modo NOARCHIVELOG: no se puede recuperar a punto en el tiempo',
                                SIN_RESPALDO_RECIENTE: 'Sin respaldo exitoso en 7 días',
                                SCRIPT_INCOMPLETO: 'Script RMAN aún no generado',
                                SIN_ESPACIO: 'Disco al 85%+ de uso en el servidor Oracle',
                            }[k]}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

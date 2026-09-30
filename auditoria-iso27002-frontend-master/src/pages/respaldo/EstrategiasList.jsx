import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getEstrategias, eliminarEstrategia, ejecutarManual, generarScript } from '../../api/respaldo.js';
import { getBases } from '../../api/respaldo.js';

const PRIORIDAD_COLOR = {
    ALTA:  'var(--risk-high)',
    MEDIA: 'var(--risk-medium)',
    BAJA:  'var(--green)',
};
const ESTADO_COLOR = {
    ACTIVA:               'var(--green)',
    BORRADOR:             'var(--muted)',
    INACTIVA:             'var(--risk-medium)',
    PENDIENTE_APROBACION: '#3b82d4',
};

export default function EstrategiasList() {
    const nav = useNavigate();
    const [estrategias, setEstrategias] = useState([]);
    const [bases, setBases] = useState([]);
    const [filtroBase, setFiltroBase] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [accionando, setAccionando] = useState(null);

    const cargar = async () => {
        setLoading(true);
        try {
            const [es, bs] = await Promise.all([
                getEstrategias(filtroBase || undefined),
                getBases(),
            ]);
            setEstrategias(es);
            setBases(bs);
        } catch { setError('No se pudieron cargar las estrategias.'); }
        finally { setLoading(false); }
    };

    useEffect(() => { cargar(); }, [filtroBase]);

    const borrar = async (id, nombre) => {
        if (!confirm(`¿Eliminar la estrategia "${nombre}"?`)) return;
        try { await eliminarEstrategia(id); cargar(); }
        catch (e) { setError(e.message); }
    };

    const generar = async (id) => {
        setAccionando(id + '_gen');
        try {
            await generarScript(id);
            cargar();
        } catch (e) { setError(e.message); }
        finally { setAccionando(null); }
    };

    const ejecutar = async (e) => {
        if (!e.scriptRman) {
            alert('Primero genera el script RMAN para esta estrategia.');
            return;
        }
        if (!confirm(`¿Ejecutar manualmente "${e.nombre}"?\nEsto creará un registro de ejecución.`)) return;
        setAccionando(e.id + '_run');
        try {
            const res = await ejecutarManual(e.id);
            alert(`Ejecución registrada (ID: ${res.ejecucionId}).\nEnvía el script al agente RMAN con agenteId: ${res.agenteId}`);
            cargar();
        } catch (err) { setError(err.message); }
        finally { setAccionando(null); }
    };

    const badge = (text, color) => (
        <span style={{ background: color + '22', color, border: `1px solid ${color}`, borderRadius: 4, padding: '2px 8px', fontSize: '0.72rem', fontFamily: 'monospace' }}>
            {text}
        </span>
    );

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
                <div>
                    <h1>Estrategias de Respaldo</h1>
                    <p style={{ color: 'var(--muted)', marginTop: 4 }}>
                        Define QUÉ respaldar, CÓMO respaldarlo y configura su programación.
                    </p>
                </div>
                <button className="btn-primary" onClick={() => nav('/app/respaldo/estrategias/nueva')}>+ Nueva estrategia</button>
            </div>

            {error && <div className="chart-card" style={{ borderColor: 'var(--risk-high)', marginBottom: 16 }}>
                <p style={{ color: 'var(--risk-high)' }}>{error}</p>
            </div>}

            {/* Filtro por base */}
            <div style={{ marginBottom: 16 }}>
                <select value={filtroBase} onChange={e => setFiltroBase(e.target.value)}
                    style={{ padding: '8px 12px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', minWidth: 220 }}>
                    <option value="">Todas las bases de datos</option>
                    {bases.map(b => <option key={b.id} value={b.id}>{b.nombre}</option>)}
                </select>
            </div>

            {loading ? <p style={{ color: 'var(--muted)' }}>Cargando...</p> : (
                estrategias.length === 0 ? (
                    <div className="chart-card" style={{ textAlign: 'center', padding: 40 }}>
                        <p style={{ color: 'var(--muted)' }}>Sin estrategias. Crea la primera.</p>
                    </div>
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {estrategias.map(e => (
                            <div key={e.id} className="chart-card" style={{ padding: '16px 20px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                    <div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                                            <span style={{ fontWeight: 700, fontSize: '1.05rem' }}>{e.nombre}</span>
                                            {badge(e.estado, ESTADO_COLOR[e.estado] || 'var(--muted)')}
                                            {badge(e.prioridad, PRIORIDAD_COLOR[e.prioridad] || 'var(--muted)')}
                                            {badge(e.tipoRespaldo?.replace('_', ' '), 'var(--muted)')}
                                        </div>
                                        <div style={{ fontSize: '0.82rem', color: 'var(--muted)', display: 'flex', gap: 16 }}>
                                            <span>📦 {e.baseDatosNombre}</span>
                                            <span style={{ fontFamily: 'monospace' }}>{e.modoArchivado}</span>
                                            {e.ultimoResultado && (
                                                <span style={{ color: e.ultimoResultado === 'EXITOSO' ? 'var(--green)' : 'var(--risk-high)' }}>
                                                    Última: {e.ultimoResultado}
                                                </span>
                                            )}
                                        </div>
                                        {e.descripcion && <p style={{ fontSize: '0.82rem', color: 'var(--muted)', marginTop: 4 }}>{e.descripcion}</p>}

                                        {/* Advertencias inline */}
                                        {e.advertencias?.length > 0 && (
                                            <div style={{ marginTop: 8 }}>
                                                {e.advertencias.map((a, i) => (
                                                    <div key={i} style={{ fontSize: '0.78rem', color: 'var(--risk-medium)', marginTop: 2 }}>
                                                        ⚠ {a}
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
                                        <button className="btn-secondary" style={{ fontSize: '0.78rem', padding: '4px 10px' }}
                                            onClick={() => nav(`/app/respaldo/estrategias/${e.id}/editar`)}>
                                            Editar
                                        </button>
                                        <button className="btn-secondary" style={{ fontSize: '0.78rem', padding: '4px 10px' }}
                                            disabled={accionando === e.id + '_gen'}
                                            onClick={() => generar(e.id)}>
                                            {accionando === e.id + '_gen' ? '…' : (e.scriptRman ? '↺ Script' : '⚙ Script')}
                                        </button>
                                        <button className="btn-primary" style={{ fontSize: '0.78rem', padding: '4px 10px' }}
                                            disabled={accionando === e.id + '_run' || !e.scriptRman}
                                            onClick={() => ejecutar(e)}>
                                            {accionando === e.id + '_run' ? '…' : '▶ Ejecutar'}
                                        </button>
                                        <button style={{ fontSize: '0.78rem', padding: '4px 10px', background: 'transparent', border: '1px solid var(--risk-high)', color: 'var(--risk-high)', borderRadius: 4, cursor: 'pointer' }}
                                            onClick={() => borrar(e.id, e.nombre)}>
                                            ✕
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )
            )}
        </div>
    );
}

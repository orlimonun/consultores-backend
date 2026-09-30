import { useState, useEffect } from 'react';
import { getBases, crearBase, actualizarBase, eliminarBase } from '../../api/respaldo.js';

const MODO_OPTS = ['ARCHIVELOG', 'NOARCHIVELOG', 'DESCONOCIDO'];

const empty = () => ({
    nombre: '', host: '', puerto: 1521, servicio: '',
    modoArchivado: 'DESCONOCIDO', descripcion: '', agenteId: '', activa: true,
});

export default function BasesDatosRespaldo() {
    const [bases, setBases] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState(null);   // null = cerrado
    const [editId, setEditId] = useState(null);
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    const cargar = async () => {
        setLoading(true);
        try { setBases(await getBases()); }
        catch { setError('No se pudieron cargar las bases de datos.'); }
        finally { setLoading(false); }
    };

    useEffect(() => { cargar(); }, []);

    const abrirNuevo = () => { setEditId(null); setForm(empty()); setError(''); };
    const abrirEditar = (b) => {
        setEditId(b.id);
        setForm({ ...b });
        setError('');
    };
    const cerrar = () => { setForm(null); setEditId(null); };

    const guardar = async () => {
        if (!form.nombre || !form.host || !form.servicio) {
            setError('Nombre, host y servicio son obligatorios.');
            return;
        }
        setSaving(true);
        setError('');
        try {
            if (editId) await actualizarBase(editId, form);
            else        await crearBase(form);
            cerrar();
            cargar();
        } catch (e) {
            setError(e.message);
        } finally {
            setSaving(false);
        }
    };

    const borrar = async (id) => {
        if (!confirm('¿Eliminar esta base de datos? Se eliminarán también sus estrategias.')) return;
        try { await eliminarBase(id); cargar(); }
        catch (e) { setError(e.message); }
    };

    const badgeModo = (modo) => {
        const colors = {
            ARCHIVELOG:   { bg: 'var(--green)',    c: '#fff' },
            NOARCHIVELOG: { bg: 'var(--risk-high)', c: '#fff' },
            DESCONOCIDO:  { bg: 'var(--muted)',     c: '#fff' },
        };
        const s = colors[modo] || colors.DESCONOCIDO;
        return (
            <span style={{
                background: s.bg, color: s.c, borderRadius: 4,
                padding: '2px 8px', fontSize: '0.72rem', fontFamily: 'monospace'
            }}>{modo}</span>
        );
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
                <div>
                    <h1>Bases de Datos Oracle</h1>
                    <p style={{ color: 'var(--muted)', marginTop: 4 }}>
                        Registra las instancias Oracle sobre las que gestionarás estrategias de respaldo RMAN.
                    </p>
                </div>
                <button className="btn-primary" onClick={abrirNuevo}>+ Nueva base</button>
            </div>

            {error && <div className="chart-card" style={{ borderColor: 'var(--risk-high)', marginBottom: 16 }}>
                <p style={{ color: 'var(--risk-high)' }}>{error}</p>
            </div>}

            {loading ? <p style={{ color: 'var(--muted)' }}>Cargando...</p> : (
                <div className="chart-card" style={{ padding: 0, overflow: 'hidden' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ borderBottom: '1px solid var(--border)' }}>
                                {['Nombre', 'Host', 'Servicio', 'Modo archivado', 'Agente', 'Estado', ''].map(h => (
                                    <th key={h} style={{ padding: '10px 16px', textAlign: 'left', fontSize: '0.78rem', color: 'var(--muted)', fontFamily: 'monospace' }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {bases.length === 0 && (
                                <tr><td colSpan={7} style={{ padding: 24, color: 'var(--muted)', textAlign: 'center' }}>
                                    Sin bases registradas. Agrega una para comenzar.
                                </td></tr>
                            )}
                            {bases.map(b => (
                                <tr key={b.id} style={{ borderBottom: '1px solid var(--border)' }}>
                                    <td style={{ padding: '10px 16px', fontWeight: 600 }}>{b.nombre}</td>
                                    <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: '0.85rem' }}>{b.host}:{b.puerto}</td>
                                    <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: '0.85rem' }}>{b.servicio}</td>
                                    <td style={{ padding: '10px 16px' }}>{badgeModo(b.modoArchivado)}</td>
                                    <td style={{ padding: '10px 16px', fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--muted)' }}>{b.agenteId || '—'}</td>
                                    <td style={{ padding: '10px 16px' }}>
                                        <span style={{ color: b.activa ? 'var(--green)' : 'var(--muted)', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                                            {b.activa ? 'Activa' : 'Inactiva'}
                                        </span>
                                    </td>
                                    <td style={{ padding: '10px 16px', display: 'flex', gap: 8 }}>
                                        <button className="btn-secondary" style={{ padding: '4px 12px', fontSize: '0.8rem' }} onClick={() => abrirEditar(b)}>Editar</button>
                                        <button style={{ padding: '4px 12px', fontSize: '0.8rem', background: 'transparent', border: '1px solid var(--risk-high)', color: 'var(--risk-high)', borderRadius: 4, cursor: 'pointer' }} onClick={() => borrar(b.id)}>Eliminar</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Modal formulario */}
            {form && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <div className="chart-card" style={{ width: 520, maxHeight: '90vh', overflowY: 'auto' }}>
                        <h2 style={{ marginBottom: 20 }}>{editId ? 'Editar base de datos' : 'Nueva base de datos Oracle'}</h2>

                        {[
                            { label: 'Nombre *', key: 'nombre', type: 'text' },
                            { label: 'Host / IP *', key: 'host', type: 'text' },
                            { label: 'Puerto', key: 'puerto', type: 'number' },
                            { label: 'Servicio / SID *', key: 'servicio', type: 'text' },
                            { label: 'ID del Agente RMAN', key: 'agenteId', type: 'text' },
                            { label: 'Descripción', key: 'descripcion', type: 'text' },
                        ].map(f => (
                            <div key={f.key} style={{ marginBottom: 14 }}>
                                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: 4 }}>{f.label}</label>
                                <input
                                    type={f.type}
                                    value={form[f.key] ?? ''}
                                    onChange={e => setForm(p => ({ ...p, [f.key]: f.type === 'number' ? Number(e.target.value) : e.target.value }))}
                                    style={{ width: '100%', padding: '8px 10px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)', boxSizing: 'border-box' }}
                                />
                            </div>
                        ))}

                        <div style={{ marginBottom: 14 }}>
                            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: 4 }}>Modo de archivado</label>
                            <select value={form.modoArchivado} onChange={e => setForm(p => ({ ...p, modoArchivado: e.target.value }))}
                                style={{ width: '100%', padding: '8px 10px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 4, color: 'var(--text)' }}>
                                {MODO_OPTS.map(o => <option key={o} value={o}>{o}</option>)}
                            </select>
                        </div>

                        {/* Aviso contextual según modo */}
                        {form.modoArchivado === 'NOARCHIVELOG' && (
                            <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid var(--risk-high)', borderRadius: 6, padding: 12, marginBottom: 14, fontSize: '0.82rem', color: 'var(--risk-high)' }}>
                                ⚠ <strong>Advertencia:</strong> La base en modo NOARCHIVELOG tiene posibilidades de recuperación más limitadas. Las estrategias incrementales no pueden recuperar hasta un punto en el tiempo.
                            </div>
                        )}
                        {form.modoArchivado === 'ARCHIVELOG' && (
                            <div style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid var(--green)', borderRadius: 6, padding: 12, marginBottom: 14, fontSize: '0.82rem', color: 'var(--green)' }}>
                                ✓ <strong>Recomendación:</strong> Modo ARCHIVELOG activo. Considere incluir el respaldo periódico de archived redo logs en sus estrategias.
                            </div>
                        )}

                        <div style={{ marginBottom: 20 }}>
                            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.85rem' }}>
                                <input type="checkbox" checked={form.activa} onChange={e => setForm(p => ({ ...p, activa: e.target.checked }))} />
                                Base activa (disponible para crear estrategias)
                            </label>
                        </div>

                        {error && <p style={{ color: 'var(--risk-high)', marginBottom: 12, fontSize: '0.85rem' }}>{error}</p>}

                        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                            <button className="btn-secondary" onClick={cerrar}>Cancelar</button>
                            <button className="btn-primary" onClick={guardar} disabled={saving}>
                                {saving ? 'Guardando…' : 'Guardar'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
    getBases, getEstrategia, crearEstrategia, actualizarEstrategia,
    generarScript, getProgramaciones, crearProgramacion, eliminarProgramacion,
} from '../../api/respaldo.js';

const PASOS = ['Información general', 'QUÉ respaldar', 'CÓMO respaldar', 'CUÁNDO y destino', 'Script RMAN'];

const TIPO_RESPALDO_OPTS = [
    { value: 'COMPLETO',                label: 'Completo',                  desc: 'Copia de todos los bloques. Mayor espacio, recuperación más simple.' },
    { value: 'INCREMENTAL_NIVEL_0',     label: 'Incremental Nivel 0',       desc: 'Base para una estrategia incremental. Funciona como completo.' },
    { value: 'INCREMENTAL_NIVEL_1',     label: 'Incremental Nivel 1 (Dif)', desc: 'Solo bloques modificados desde el último nivel 0 o 1.' },
    { value: 'INCREMENTAL_DIFERENCIAL', label: 'Incremental Diferencial',   desc: 'Igual que nivel 1 estándar. Explícito para claridad.' },
    { value: 'INCREMENTAL_ACUMULATIVO', label: 'Incremental Acumulativo',   desc: 'Bloques modificados desde el último nivel 0. Mayor tamaño, recuperación más rápida.' },
];

const emptyForm = () => ({
    nombre: '', descripcion: '', baseDatosId: '',
    responsableId: '', prioridad: 'MEDIA', estado: 'BORRADOR',
    incluirDatabase: true, tablespaces: '', datafiles: '',
    incluirControlFile: true, incluirSpfile: true, incluirArchivedLogs: false,
    tipoRespaldo: 'COMPLETO', compresion: false, algoritmoCompresion: 'BASIC',
    deleteArchivedLogs: false, canales: 1,
    destinoRuta: '', destinoDispositivo: 'DISK', formatoBackupset: '%d_%T_%U',
    notas: '',
});

const emptyCron = () => ({
    cronExpresion: '0 2 * * 0', descripcionCron: 'Cada domingo a las 02:00',
    fechaInicio: new Date().toISOString().slice(0, 16),
    ventanaMinutos: 120, mecanismo: 'AGENTE_RMAN',
});

export default function EstrategiaForm() {
    const { id } = useParams();
    const nav = useNavigate();
    const esEdicion = Boolean(id);

    const [paso, setPaso] = useState(0);
    const [form, setForm] = useState(emptyForm());
    const [cronForm, setCronForm] = useState(emptyCron());
    const [bases, setBases] = useState([]);
    const [estrategia, setEstrategia] = useState(null);
    const [programaciones, setProgramaciones] = useState([]);
    const [loading, setLoading] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [error, setError] = useState('');
    const [generando, setGenerando] = useState(false);

    useEffect(() => {
        getBases().then(setBases).catch(() => {});
        if (esEdicion) {
            setLoading(true);
            Promise.all([
                getEstrategia(id),
                getProgramaciones(id),
            ]).then(([e, ps]) => {
                setEstrategia(e);
                setProgramaciones(ps);
                setForm({
                    nombre: e.nombre || '',
                    descripcion: e.descripcion || '',
                    baseDatosId: e.baseDatosId || '',
                    responsableId: e.responsableId || '',
                    prioridad: e.prioridad || 'MEDIA',
                    estado: e.estado || 'BORRADOR',
                    incluirDatabase: e.incluirDatabase ?? true,
                    tablespaces: e.tablespaces || '',
                    datafiles: e.datafiles || '',
                    incluirControlFile: e.incluirControlFile ?? true,
                    incluirSpfile: e.incluirSpfile ?? true,
                    incluirArchivedLogs: e.incluirArchivedLogs ?? false,
                    tipoRespaldo: e.tipoRespaldo || 'COMPLETO',
                    compresion: e.compresion ?? false,
                    algoritmoCompresion: e.algoritmoCompresion || 'BASIC',
                    deleteArchivedLogs: e.deleteArchivedLogs ?? false,
                    canales: e.canales || 1,
                    destinoRuta: e.destinoRuta || '',
                    destinoDispositivo: e.destinoDispositivo || 'DISK',
                    formatoBackupset: e.formatoBackupset || '%d_%T_%U',
                    notas: e.notas || '',
                });
            }).catch(() => setError('No se pudo cargar la estrategia.'))
            .finally(() => setLoading(false));
        }
    }, [id]);

    const setF = (key, val) => setForm(p => ({ ...p, [key]: val }));
    const setC = (key, val) => setCronForm(p => ({ ...p, [key]: val }));

    const baseSel = bases.find(b => b.id === Number(form.baseDatosId));

    const guardar = async () => {
        if (!form.nombre) { setError('El nombre es obligatorio.'); return; }
        if (!form.baseDatosId) { setError('Selecciona una base de datos.'); return; }
        setGuardando(true); setError('');
        try {
            const payload = { ...form, baseDatosId: Number(form.baseDatosId) };
            if (esEdicion) await actualizarEstrategia(id, payload);
            else await crearEstrategia(payload);
            nav('/app/respaldo/estrategias');
        } catch (e) { setError(e.message); }
        finally { setGuardando(false); }
    };

    const generarScriptAction = async () => {
        if (!esEdicion) { setError('Guarda la estrategia primero antes de generar el script.'); return; }
        setGenerando(true); setError('');
        try {
            const e = await generarScript(id);
            setEstrategia(e);
            setPaso(4);
        } catch (e) { setError(e.message); }
        finally { setGenerando(false); }
    };

    const agregarProgramacion = async () => {
        if (!esEdicion) { setError('Guarda la estrategia primero.'); return; }
        try {
            await crearProgramacion({ ...cronForm, estrategiaId: Number(id), fechaInicio: cronForm.fechaInicio + ':00' });
            const ps = await getProgramaciones(id);
            setProgramaciones(ps);
            setCronForm(emptyCron());
        } catch (e) { setError(e.message); }
    };

    const borrarProgramacion = async (pid) => {
        if (!confirm('¿Eliminar esta programación?')) return;
        try {
            await eliminarProgramacion(pid);
            setProgramaciones(ps => ps.filter(p => p.id !== pid));
        } catch (e) { setError(e.message); }
    };

    const advertencias = estrategia?.advertencias || [];
    const scriptRman = estrategia?.scriptRman;

    if (loading) return <p style={{ color: 'var(--muted)' }}>Cargando...</p>;

    return (
        <div style={{ maxWidth: 760 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
                <div>
                    <h1>{esEdicion ? 'Editar estrategia' : 'Nueva estrategia de respaldo'}</h1>
                    <p style={{ color: 'var(--muted)', marginTop: 4 }}>Construye la estrategia paso a paso: QUÉ · CÓMO · CUÁNDO</p>
                </div>
                <button className="btn-secondary" onClick={() => nav('/app/respaldo/estrategias')}>← Volver</button>
            </div>

            {error && <div className="chart-card" style={{ borderColor: 'var(--risk-high)', marginBottom: 16 }}>
                <p style={{ color: 'var(--risk-high)' }}>{error}</p>
            </div>}

            {/* Stepper */}
            <div style={{ display: 'flex', gap: 0, marginBottom: 28, borderBottom: '1px solid var(--border)' }}>
                {PASOS.map((p, i) => (
                    <button key={i} onClick={() => setPaso(i)}
                        style={{ flex: 1, padding: '10px 4px', border: 'none', borderBottom: i === paso ? '2px solid var(--accent)' : '2px solid transparent',
                            background: 'transparent', color: i === paso ? 'var(--accent)' : 'var(--muted)',
                            fontFamily: 'monospace', fontSize: '0.75rem', cursor: 'pointer', transition: 'color 0.15s' }}>
                        {i + 1}. {p}
                    </button>
                ))}
            </div>

            {/* ── Paso 0: Información general ── */}
            {paso === 0 && (
                <div>
                    <Section title="Información general">
                        <Field label="Nombre de la estrategia *">
                            <input value={form.nombre} onChange={e => setF('nombre', e.target.value)} style={iStyle} placeholder="Ej: Respaldo nocturno producción" />
                        </Field>
                        <Field label="Descripción">
                            <textarea value={form.descripcion} onChange={e => setF('descripcion', e.target.value)} rows={2} style={{ ...iStyle, resize: 'vertical' }} />
                        </Field>
                        <Field label="Base de datos Oracle *">
                            <select value={form.baseDatosId} onChange={e => setF('baseDatosId', e.target.value)} style={iStyle}>
                                <option value="">-- Seleccionar --</option>
                                {bases.map(b => <option key={b.id} value={b.id}>{b.nombre} ({b.servicio})</option>)}
                            </select>
                        </Field>
                        {baseSel && baseSel.modoArchivado === 'NOARCHIVELOG' && (
                            <Warn>⚠ La base está en modo NOARCHIVELOG. Las posibilidades de recuperación puntual son limitadas.</Warn>
                        )}
                        {baseSel && baseSel.modoArchivado === 'ARCHIVELOG' && (
                            <Info>✓ Base en modo ARCHIVELOG. Considera incluir archived redo logs en la estrategia.</Info>
                        )}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                            <Field label="Prioridad">
                                <select value={form.prioridad} onChange={e => setF('prioridad', e.target.value)} style={iStyle}>
                                    <option value="ALTA">Alta — información crítica</option>
                                    <option value="MEDIA">Media — importante</option>
                                    <option value="BAJA">Baja — menor impacto</option>
                                </select>
                            </Field>
                            <Field label="Estado">
                                <select value={form.estado} onChange={e => setF('estado', e.target.value)} style={iStyle}>
                                    <option value="BORRADOR">Borrador</option>
                                    <option value="ACTIVA">Activa</option>
                                    <option value="INACTIVA">Inactiva</option>
                                </select>
                            </Field>
                        </div>
                        <Field label="Notas adicionales">
                            <textarea value={form.notas} onChange={e => setF('notas', e.target.value)} rows={2} style={{ ...iStyle, resize: 'vertical' }} />
                        </Field>
                    </Section>
                </div>
            )}

            {/* ── Paso 1: QUÉ respaldar ── */}
            {paso === 1 && (
                <Section title="QUÉ respaldar">
                    <p style={{ color: 'var(--muted)', fontSize: '0.85rem', marginBottom: 16 }}>
                        Selecciona los elementos que formarán parte del respaldo. Puedes combinar opciones.
                    </p>
                    <CheckRow label="Base de datos completa (DATABASE)" checked={form.incluirDatabase} onChange={v => setF('incluirDatabase', v)}
                        desc="Respalda todos los datafiles de la base de datos." />
                    {!form.incluirDatabase && (
                        <>
                            <Field label="Tablespaces (separados por coma)">
                                <input value={form.tablespaces} onChange={e => setF('tablespaces', e.target.value)} style={iStyle} placeholder="USERS, INDX, APP_DATA" />
                            </Field>
                            <Field label="Datafiles (rutas completas, separadas por coma)">
                                <input value={form.datafiles} onChange={e => setF('datafiles', e.target.value)} style={iStyle} placeholder="/u01/oradata/users01.dbf, /u01/oradata/indx01.dbf" />
                            </Field>
                        </>
                    )}
                    <div style={{ borderTop: '1px solid var(--border)', marginTop: 16, paddingTop: 16 }}>
                        <CheckRow label="Control File" checked={form.incluirControlFile} onChange={v => setF('incluirControlFile', v)}
                            desc="Incluir el control file en el respaldo." />
                        <CheckRow label="SPFILE (parámetros del servidor)" checked={form.incluirSpfile} onChange={v => setF('incluirSpfile', v)}
                            desc="Respalda el archivo de parámetros del servidor." />
                        <CheckRow label="Archived Redo Logs" checked={form.incluirArchivedLogs} onChange={v => setF('incluirArchivedLogs', v)}
                            desc="Solo disponible en modo ARCHIVELOG. Permite recuperación hasta un punto en el tiempo." />
                        {form.incluirArchivedLogs && (
                            <CheckRow label="Eliminar archived logs después de respaldar (DELETE INPUT)" checked={form.deleteArchivedLogs} onChange={v => setF('deleteArchivedLogs', v)}
                                desc="Libera espacio en el FRA después de respaldar los logs." />
                        )}
                    </div>
                </Section>
            )}

            {/* ── Paso 2: CÓMO respaldar ── */}
            {paso === 2 && (
                <Section title="CÓMO respaldar">
                    <Field label="Tipo de respaldo">
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                            {TIPO_RESPALDO_OPTS.map(t => (
                                <label key={t.value} style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer',
                                    padding: 12, borderRadius: 6, border: `1px solid ${form.tipoRespaldo === t.value ? 'var(--accent)' : 'var(--border)'}`,
                                    background: form.tipoRespaldo === t.value ? 'rgba(59,130,212,0.08)' : 'transparent' }}>
                                    <input type="radio" name="tipoRespaldo" value={t.value} checked={form.tipoRespaldo === t.value}
                                        onChange={() => setF('tipoRespaldo', t.value)} style={{ marginTop: 2 }} />
                                    <div>
                                        <div style={{ fontWeight: 600, fontSize: '0.88rem' }}>{t.label}</div>
                                        <div style={{ fontSize: '0.78rem', color: 'var(--muted)', marginTop: 2 }}>{t.desc}</div>
                                    </div>
                                </label>
                            ))}
                        </div>
                    </Field>
                    <div style={{ borderTop: '1px solid var(--border)', marginTop: 16, paddingTop: 16 }}>
                        <CheckRow label="Usar compresión (COMPRESSED BACKUPSET)" checked={form.compresion} onChange={v => setF('compresion', v)}
                            desc="Reduce el espacio de almacenamiento a costa de mayor CPU." />
                        {form.compresion && (
                            <Field label="Algoritmo de compresión">
                                <select value={form.algoritmoCompresion} onChange={e => setF('algoritmoCompresion', e.target.value)} style={iStyle}>
                                    <option value="BASIC">BASIC (sin licencia adicional)</option>
                                    <option value="LOW">LOW (Advanced Compression)</option>
                                    <option value="MEDIUM">MEDIUM (Advanced Compression)</option>
                                    <option value="HIGH">HIGH (Advanced Compression)</option>
                                </select>
                            </Field>
                        )}
                        <Field label="Canales paralelos RMAN">
                            <input type="number" min={1} max={8} value={form.canales} onChange={e => setF('canales', Number(e.target.value))} style={{ ...iStyle, width: 80 }} />
                            <span style={{ color: 'var(--muted)', fontSize: '0.8rem', marginLeft: 8 }}>canal(es)</span>
                        </Field>
                    </div>
                </Section>
            )}

            {/* ── Paso 3: CUÁNDO y destino ── */}
            {paso === 3 && (
                <div>
                    <Section title="Destino del respaldo">
                        <Field label="Ruta de destino">
                            <input value={form.destinoRuta} onChange={e => setF('destinoRuta', e.target.value)} style={iStyle}
                                placeholder="/backup/rman  (vacío = usa Fast Recovery Area)" />
                        </Field>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                            <Field label="Dispositivo">
                                <select value={form.destinoDispositivo} onChange={e => setF('destinoDispositivo', e.target.value)} style={iStyle}>
                                    <option value="DISK">DISK</option>
                                    <option value="SBT_TAPE">SBT_TAPE</option>
                                </select>
                            </Field>
                            <Field label="Formato del backupset">
                                <input value={form.formatoBackupset} onChange={e => setF('formatoBackupset', e.target.value)} style={iStyle} placeholder="%d_%T_%U" />
                            </Field>
                        </div>
                        {!form.destinoRuta && (
                            <Warn>Sin ruta definida se usará la Fast Recovery Area. Asegúrate de que esté configurada en Oracle.</Warn>
                        )}
                    </Section>

                    {esEdicion && (
                        <Section title="Programación (CUÁNDO ejecutar)">
                            <p style={{ color: 'var(--muted)', fontSize: '0.82rem', marginBottom: 12 }}>
                                Configura cuándo se ejecutará automáticamente esta estrategia.
                            </p>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                                <Field label="Expresión cron">
                                    <input value={cronForm.cronExpresion} onChange={e => setC('cronExpresion', e.target.value)} style={iStyle} placeholder="0 2 * * 0" />
                                </Field>
                                <Field label="Descripción legible">
                                    <input value={cronForm.descripcionCron} onChange={e => setC('descripcionCron', e.target.value)} style={iStyle} placeholder="Cada domingo a las 02:00" />
                                </Field>
                                <Field label="Inicio">
                                    <input type="datetime-local" value={cronForm.fechaInicio} onChange={e => setC('fechaInicio', e.target.value)} style={iStyle} />
                                </Field>
                                <Field label="Ventana máxima (min)">
                                    <input type="number" value={cronForm.ventanaMinutos} onChange={e => setC('ventanaMinutos', Number(e.target.value))} style={iStyle} />
                                </Field>
                            </div>
                            <button className="btn-primary" style={{ marginTop: 8 }} onClick={agregarProgramacion}>+ Agregar programación</button>

                            {programaciones.length > 0 && (
                                <div style={{ marginTop: 16 }}>
                                    <h4 style={{ marginBottom: 8, fontSize: '0.85rem' }}>Programaciones activas</h4>
                                    {programaciones.map(p => (
                                        <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                                            padding: '8px 12px', background: 'var(--surface)', borderRadius: 4, marginBottom: 6, border: '1px solid var(--border)' }}>
                                            <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{p.cronExpresion}</span>
                                            <span style={{ color: 'var(--muted)', fontSize: '0.8rem' }}>{p.descripcionCron}</span>
                                            <button onClick={() => borrarProgramacion(p.id)} style={{ background: 'transparent', border: 'none', color: 'var(--risk-high)', cursor: 'pointer', fontSize: '0.85rem' }}>✕</button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </Section>
                    )}
                    {!esEdicion && (
                        <Info>Guarda la estrategia primero para poder configurar la programación.</Info>
                    )}
                </div>
            )}

            {/* ── Paso 4: Script RMAN ── */}
            {paso === 4 && (
                <Section title="Script RMAN generado">
                    {advertencias.length > 0 && (
                        <div style={{ marginBottom: 16 }}>
                            {advertencias.map((a, i) => <Warn key={i}>{a}</Warn>)}
                        </div>
                    )}
                    {scriptRman ? (
                        <>
                            <p style={{ color: 'var(--muted)', fontSize: '0.82rem', marginBottom: 10 }}>
                                Revisa el script antes de aprobarlo. Fue generado a las {estrategia?.scriptGeneradoEn?.replace('T', ' ')?.slice(0, 16)}.
                            </p>
                            <pre style={{ background: '#0d1117', color: '#58a6ff', padding: 20, borderRadius: 6,
                                fontSize: '0.82rem', overflowX: 'auto', lineHeight: 1.6, border: '1px solid var(--border)',
                                maxHeight: 480, overflowY: 'auto' }}>
                                {scriptRman}
                            </pre>
                            <div style={{ marginTop: 12, display: 'flex', gap: 10 }}>
                                <button className="btn-secondary" disabled={generando} onClick={generarScriptAction}>
                                    {generando ? 'Generando…' : '↺ Regenerar script'}
                                </button>
                                <button className="btn-primary" onClick={() => navigator.clipboard?.writeText(scriptRman)}>
                                    Copiar script
                                </button>
                            </div>
                        </>
                    ) : (
                        <div style={{ textAlign: 'center', padding: 32 }}>
                            <p style={{ color: 'var(--muted)', marginBottom: 16 }}>
                                {esEdicion
                                    ? 'El script aún no ha sido generado. Guarda los cambios y luego genera el script.'
                                    : 'Guarda la estrategia primero para poder generar el script RMAN.'}
                            </p>
                            {esEdicion && (
                                <button className="btn-primary" disabled={generando} onClick={generarScriptAction}>
                                    {generando ? 'Generando…' : '⚙ Generar script RMAN'}
                                </button>
                            )}
                        </div>
                    )}
                </Section>
            )}

            {/* Navegación entre pasos */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 28 }}>
                <button className="btn-secondary" disabled={paso === 0} onClick={() => setPaso(p => p - 1)}>← Anterior</button>
                <div style={{ display: 'flex', gap: 10 }}>
                    {paso < PASOS.length - 1 && (
                        <button className="btn-primary" onClick={() => setPaso(p => p + 1)}>Siguiente →</button>
                    )}
                    <button className="btn-primary" disabled={guardando} onClick={guardar}>
                        {guardando ? 'Guardando…' : (esEdicion ? '✓ Guardar cambios' : '✓ Crear estrategia')}
                    </button>
                </div>
            </div>
        </div>
    );
}

// ── Componentes auxiliares ───────────────────────────────────────────────────

function Section({ title, children }) {
    return (
        <div className="chart-card" style={{ marginBottom: 20, padding: '20px 24px' }}>
            <h3 style={{ marginBottom: 16, fontSize: '0.95rem', color: 'var(--accent)' }}>{title}</h3>
            {children}
        </div>
    );
}

function Field({ label, children }) {
    return (
        <div style={{ marginBottom: 14 }}>
            <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--muted)', marginBottom: 4 }}>{label}</label>
            {children}
        </div>
    );
}

function CheckRow({ label, checked, onChange, desc }) {
    return (
        <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer' }}>
                <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} style={{ marginTop: 2 }} />
                <div>
                    <div style={{ fontSize: '0.88rem', fontWeight: 600 }}>{label}</div>
                    {desc && <div style={{ fontSize: '0.78rem', color: 'var(--muted)', marginTop: 1 }}>{desc}</div>}
                </div>
            </label>
        </div>
    );
}

function Warn({ children }) {
    return (
        <div style={{ background: 'rgba(239,68,68,0.08)', border: '1px solid var(--risk-high)', borderRadius: 6,
            padding: '8px 12px', marginBottom: 10, fontSize: '0.82rem', color: 'var(--risk-high)' }}>
            {children}
        </div>
    );
}

function Info({ children }) {
    return (
        <div style={{ background: 'rgba(34,197,94,0.08)', border: '1px solid var(--green)', borderRadius: 6,
            padding: '8px 12px', marginBottom: 10, fontSize: '0.82rem', color: 'var(--green)' }}>
            {children}
        </div>
    );
}

const iStyle = {
    width: '100%', padding: '8px 10px',
    background: 'var(--surface)', border: '1px solid var(--border)',
    borderRadius: 4, color: 'var(--text)', boxSizing: 'border-box',
};

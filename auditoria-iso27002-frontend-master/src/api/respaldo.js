import { api } from './client.js';

// ── Bases de datos ────────────────────────────────────────────────────────
export const getBases         = ()       => api.get('/respaldo/bases');
export const crearBase        = (data)   => api.post('/respaldo/bases', data);
export const actualizarBase   = (id, d)  => api.put(`/respaldo/bases/${id}`, d);
export const eliminarBase     = (id)     => api.delete(`/respaldo/bases/${id}`);

// ── Estrategias ───────────────────────────────────────────────────────────
export const getEstrategias        = (baseDatosId) =>
    api.get('/respaldo/estrategias' + (baseDatosId ? `?baseDatosId=${baseDatosId}` : ''));
export const getEstrategia         = (id)   => api.get(`/respaldo/estrategias/${id}`);
export const crearEstrategia       = (data) => api.post('/respaldo/estrategias', data);
export const actualizarEstrategia  = (id, d) => api.put(`/respaldo/estrategias/${id}`, d);
export const eliminarEstrategia    = (id)   => api.delete(`/respaldo/estrategias/${id}`);
export const generarScript         = (id)   => api.post(`/respaldo/estrategias/${id}/generar-script`);
export const ejecutarManual        = (id)   => api.post(`/respaldo/estrategias/${id}/ejecutar`);

// ── Programaciones ────────────────────────────────────────────────────────
export const getProgramaciones      = (estrategiaId) =>
    api.get('/respaldo/programaciones' + (estrategiaId ? `?estrategiaId=${estrategiaId}` : ''));
export const crearProgramacion      = (data)  => api.post('/respaldo/programaciones', data);
export const actualizarProgramacion = (id, d) => api.put(`/respaldo/programaciones/${id}`, d);
export const eliminarProgramacion   = (id)    => api.delete(`/respaldo/programaciones/${id}`);

// ── Ejecuciones / Historial ───────────────────────────────────────────────
export const getHistorial           = ()    => api.get('/respaldo/ejecuciones');
export const getEjecucion           = (id)  => api.get(`/respaldo/ejecuciones/${id}`);
export const getHistorialEstrategia = (id)  => api.get(`/respaldo/ejecuciones/estrategia/${id}`);

// ── Alertas ───────────────────────────────────────────────────────────────
export const getAlertas = () => api.get('/respaldo/alertas');

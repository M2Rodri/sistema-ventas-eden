import { ReporteReciente } from '@/types/reporte';

const CLAVE = 'reportes:recientes';
const MAXIMO = 8;

/**
 * "Reportes recientes": los reportes personalizados que generó el administrador, guardados en este
 * navegador. No viajan al servidor: cada navegador ve los suyos. Si el navegador no deja guardar
 * (modo privado, datos bloqueados), la lista simplemente queda vacía.
 */
export function leerRecientes(): ReporteReciente[] {
  try {
    const crudo = window.localStorage.getItem(CLAVE);
    const lista = crudo ? JSON.parse(crudo) : [];
    return Array.isArray(lista) ? lista : [];
  } catch {
    return [];
  }
}

function guardar(lista: ReporteReciente[]) {
  try {
    window.localStorage.setItem(CLAVE, JSON.stringify(lista.slice(0, MAXIMO)));
  } catch {
    // sin almacenamiento: no pasa nada
  }
}

const mismaConfiguracion = (a: ReporteReciente, b: ReporteReciente) =>
  a.tipo === b.tipo &&
  a.fechaInicio === b.fechaInicio &&
  a.fechaFin === b.fechaFin &&
  a.limite === b.limite &&
  JSON.stringify(a.criterios) === JSON.stringify(b.criterios);

/** Agrega el reporte arriba de la lista; si ya había uno igual, se reemplaza (sube y se actualiza su fecha). */
export function agregarReciente(nuevo: ReporteReciente): ReporteReciente[] {
  const lista = [nuevo, ...leerRecientes().filter((r) => !mismaConfiguracion(r, nuevo))].slice(0, MAXIMO);
  guardar(lista);
  return lista;
}

export function quitarReciente(id: string): ReporteReciente[] {
  const lista = leerRecientes().filter((r) => r.id !== id);
  guardar(lista);
  return lista;
}

export function limpiarRecientes(): ReporteReciente[] {
  guardar([]);
  return [];
}

// Reglas de entrega de una venta, en un solo lugar para que la tabla, el detalle
// y las tarjetas de la web digan lo mismo (y coincidan con el backend y la app).
import { EstadoEntrega, EstadoVenta, ModalidadEntrega, Venta } from '@/types/venta';

/**
 * "Por entregar": estado de entrega distinto de ENTREGADO y venta no cancelada.
 * Es la misma definición que usan el backend y la app. Retiro en tienda no
 * aparece porque nace ENTREGADO.
 */
export const esPorEntregar = (venta: Venta): boolean =>
  venta.estado !== EstadoVenta.CANCELADA && venta.estadoEntrega !== EstadoEntrega.ENTREGADO;

/**
 * Una venta por transportadora sin el nombre de la transportadora o sin la
 * guía. No bloquea nada: solo se muestra la etiqueta "Falta completar".
 */
export const faltaCompletarEnvio = (venta: Venta): boolean =>
  venta.estado !== EstadoVenta.CANCELADA &&
  venta.modalidadEntrega === ModalidadEntrega.TRANSPORTADORA &&
  (!venta.transportadora?.trim() || !venta.guiaRemision?.trim());

export const etiquetaEstadoEntrega = (estado?: EstadoEntrega): string => {
  switch (estado) {
    case EstadoEntrega.ENTREGADO:
      return 'Entregado';
    case EstadoEntrega.DESPACHADO:
      return 'Despachado';
    default:
      return 'Pendiente';
  }
};

export const etiquetaModalidad = (modalidad?: ModalidadEntrega): string => {
  switch (modalidad) {
    case ModalidadEntrega.DOMICILIO:
      return 'Entrega a domicilio';
    case ModalidadEntrega.TRANSPORTADORA:
      return 'Envío por transportadora';
    default:
      return 'En tienda';
  }
};

/** Clases de color del badge de cada estado de entrega. */
export const claseBadgeEstadoEntrega = (estado?: EstadoEntrega): string => {
  switch (estado) {
    case EstadoEntrega.ENTREGADO:
      return 'bg-green-100 text-green-800 border-green-200';
    case EstadoEntrega.DESPACHADO:
      return 'bg-amber-100 text-amber-800 border-amber-200';
    default:
      return 'bg-gray-100 text-gray-800 border-gray-200';
  }
};

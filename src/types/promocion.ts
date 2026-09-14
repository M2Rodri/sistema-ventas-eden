// types/promocion.ts
//
// El módulo se llamaba "Oferta" en el frontend y "ofertas" en la API, pero la
// tabla siempre se llamó 'promociones'. Se unificó el nombre en las tres capas.

export interface Promocion {
  id: number;
  /** Nombre de la promoción ("Semana del Descanso"). Es obligatorio en la base. */
  nombre: string;
  descripcion?: string;
  /** Porcentaje de descuento. */
  descuento: number;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
  vigente: boolean;
  productos: ProductoSimple[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface ProductoSimple {
  id: number;
  nombre: string;
  sku: string;
}

export interface PromocionRequest {
  nombre: string;
  descripcion?: string;
  descuento: number;
  fechaInicio: string;
  fechaFin: string;
  idsProductos: number[];
  activo: boolean;
}

export type EstadoPromocion = 'ACTIVA' | 'PROGRAMADA' | 'VENCIDA' | 'INACTIVA';

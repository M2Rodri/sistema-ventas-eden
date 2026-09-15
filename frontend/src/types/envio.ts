// types/envio.ts

export type EstadoEnvio = 
  | 'PENDIENTE' 
  | 'EN_PREPARACION' 
  | 'EN_CAMINO' 
  | 'ENTREGADO' 
  | 'DEVUELTO' 
  | 'CANCELADO';

export interface Envio {
  id: number;
  idVenta: number | null;
  nombreCliente: string;
  telefonoCliente: string;
  direccionDestino: string;
  ciudad: string | null;
  departamento: string | null;
  fechaEntregaEstimada: string; // LocalDate
  fechaEntregaReal: string | null; // LocalDate
  estadoSeguimiento: EstadoEnvio;
  guiaRemision: string | null;
  costoEnvio: number;
  idTransportadora: number | null;
  nombreTransportadora: string | null;
  notas: string | null;
  fechaCreacion: string; // LocalDateTime
  fechaActualizacion: string; // LocalDateTime
}

export interface EnvioRequest {
  idVenta?: number | null;
  direccionDestino: string;
  ciudad?: string;
  departamento?: string;
  fechaEntregaEstimada: string;
  guiaRemision?: string;
  costoEnvio: number;
  idTransportadora?: number | null;
  notas?: string;
}

export interface EnvioEstadisticas {
  pendientes: number;
  enPreparacion: number;
  enCamino: number;
  entregados: number;
  devueltos: number;
  cancelados: number;
}
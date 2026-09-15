// types/transportadora.ts

export interface Transportadora {
  id: number;
  nombre: string;
  telefono: string | null;
  email: string | null;
  tarifaBase: number;
  tiempoEstimadoDias: number;
  activo: boolean;
  fechaRegistro: string; // LocalDateTime
  fechaActualizacion: string; // LocalDateTime
}

export interface TransportadoraRequest {
  nombre: string;
  telefono?: string;
  email?: string;
  tarifaBase: number;
  tiempoEstimadoDias: number;
  activo: boolean;
}
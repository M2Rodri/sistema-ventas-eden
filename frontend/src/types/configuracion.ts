// types/configuracion.ts
export interface Configuracion {
  id: number;
  clave: string;
  valor: string;
  descripcion?: string;
  fechaActualizacion: string;
}

export interface ConfiguracionRequest {
  clave: string;
  valor: string;
  descripcion?: string;
}

export interface Auditoria {
  id: number;
  idUsuario: number | null;
  nombreUsuario: string;
  accion: string;
  tablaAfectada: string;
  idRegistro: string;
  detalles: string;
  fechaHora: string;
  ipDispositivo: string;
}

// Configuraciones agrupadas por categoría
export interface ConfiguracionesGenerales {
  nombreSistema: string;
  zonaHoraria: string;
  moneda: string;
  stockMinimo: string;
  modoMantenimiento: string;
  permitirRegistro: string;
  mostrarPreciosSinLogin: string;
}

export interface ConfiguracionesEmpresa {
  razonSocial: string;
  nit: string;
  direccion: string;
  telefono: string;
  email: string;
  sitioWeb: string;
}

export interface ConfiguracionesEmail {
  servidorSMTP: string;
  puertoSMTP: string;
  usuarioSMTP: string;
  passwordSMTP: string;
  emailRemitente: string;
  nombreRemitente: string;
  enviarBienvenida: string;
  enviarNotificacionEnvio: string;
}

export type TipoAccion = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT';
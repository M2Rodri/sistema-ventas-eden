// frontend/src/types/producto.ts
export type TipoProducto = 'CAMA' | 'COLCHON' | 'ALMOHADA' | 'ACCESORIO';

export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  descripcion?: string;
  marca?: string;
  modelo?: string;
  idCategoria: number;
  nombreCategoria: string;
  calidad?: string;
  /** Costo de referencia para calcular margen. Antes se llamaba precioUnitario. */
  costoReferencial: number;
  precioVenta: number;
  peso?: number;
  dimensiones?: string;
  stockMinimo: number;
  tipoProducto: TipoProducto;
  activo: boolean;

  /**
   * Atributos propios del rubro. En la base son texto libre, no una lista
   * cerrada: "Firme", "Resortes ensacados", "Viscoelástica". Antes estaban
   * tipados como uniones en minúscula que ningún dato real cumplía.
   * tipoCama se eliminó: duplicaba tipoProducto y la columna ya no existe.
   */
  firmeza?: string;
  materialNucleo?: string;

  imagenes: ImagenProducto[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface ProductoRequest {
  sku: string;
  nombre: string;
  descripcion?: string;
  marca?: string;
  modelo?: string;
  idCategoria: number;
  calidad?: string;
  costoReferencial: number;
  precioVenta: number;
  peso?: number;
  dimensiones?: string;
  stockMinimo: number;
  tipoProducto: TipoProducto;
  activo: boolean;

  firmeza?: string;
  materialNucleo?: string;
}

export interface ImagenProducto {
  id: number;
  urlImagen: string;
  esPrincipal: boolean;
  orden: number;
}

export interface Categoria {
  id: number;
  nombre: string;
  descripcion?: string;
  activo: boolean;
  cantidadProductos: number;
  fechaCreacion: string;
  fechaActualizacion: string;
}

// frontend/src/types/producto.ts (agregar al final)

export interface MultimediaProducto {
  id: number;
  productoId: number;
  urlModelo3d: string;
  urlVistaPrevia?: string;
  habilitadoRa: boolean;
  activo: boolean;
}
// frontend/src/types/imagenProducto.ts

export interface ImagenProducto {
  id: number;
  urlImagen: string;
  esPrincipal: boolean;
  orden?: number; 
}
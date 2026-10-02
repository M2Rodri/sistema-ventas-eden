import { TipoReporte } from '@/types/reporte';

/**
 * Criterios del reporte personalizado. Cada tipo de reporte admite un conjunto distinto
 * (ver CAMPOS_POR_TIPO); el generador solo muestra los que corresponden al tipo elegido.
 */
export interface CriteriosReporte {
  /** Estado de pago de la venta tal como lo devuelve el reporte: COMPLETADA, PENDIENTE_PAGO, CANCELADA. */
  estadoPago?: string;
  metodoPago?: string;
  cliente?: string;
  producto?: string;
  categoria?: string;
  minimoCompras?: number;
  minimoDias?: number;
  proveedor?: string;
  soloActivos?: boolean;
  /** Importes mínimo y máximo, en bolivianos (según el reporte: monto de la venta o total comprado). */
  montoMinimo?: number;
  montoMaximo?: number;
  saldoMinimo?: number;
  cantidadMinima?: number;
  valorMinimo?: number;
  /** Cómo se ordenan las filas; vacío es el orden normal del reporte. */
  orden?: string;
}

export type CampoCriterio = keyof CriteriosReporte;

/** Qué criterios se ofrecen para cada tipo de reporte, además de fechas y límite. */
export const CAMPOS_POR_TIPO: Record<TipoReporte, CampoCriterio[]> = {
  VENTAS: ['estadoPago', 'metodoPago', 'cliente', 'montoMinimo', 'montoMaximo', 'orden'],
  PRODUCTOS_MAS_VENDIDOS: ['categoria', 'cantidadMinima'],
  CLIENTES_FRECUENTES: ['cliente', 'minimoCompras', 'montoMinimo'],
  INVENTARIO_VALORIZADO: ['categoria', 'orden'],
  VENTAS_POR_CATEGORIA: ['categoria'],
  VENTAS_POR_METODO_PAGO: ['metodoPago'],
  VENTAS_POR_PRODUCTO: ['producto', 'categoria'],
  INVENTARIO_STOCK_BAJO: ['categoria'],
  PROVEEDORES: ['proveedor', 'soloActivos'],
  TRANSPORTADORAS: [],
  FINANCIERO: [],
  // Cuentas por cobrar ya es una lista corta y ordenada por antigüedad: solo se busca por cliente.
  CUENTAS_POR_COBRAR: ['cliente'],
};

/** Criterios que se escriben como número. */
export const CAMPOS_NUMERICOS: CampoCriterio[] = [
  'minimoCompras', 'minimoDias', 'montoMinimo', 'montoMaximo', 'saldoMinimo', 'cantidadMinima', 'valorMinimo',
];

export const ETIQUETA_CRITERIO: Record<CampoCriterio, string> = {
  estadoPago: 'Estado de pago',
  metodoPago: 'Método de pago',
  cliente: 'Cliente',
  producto: 'Producto o SKU',
  categoria: 'Categoría',
  minimoCompras: 'Mínimo de compras',
  minimoDias: 'Días de atraso desde',
  proveedor: 'Proveedor',
  soloActivos: 'Solo proveedores activos',
  montoMinimo: 'Monto desde (Bs)',
  montoMaximo: 'Monto hasta (Bs)',
  saldoMinimo: 'Saldo pendiente desde (Bs)',
  cantidadMinima: 'Unidades vendidas desde',
  valorMinimo: 'Valor en stock desde (Bs)',
  orden: 'Ordenar por',
};

/** Etiqueta del criterio en un reporte puntual (el mismo campo significa cosas distintas según el reporte). */
export const etiquetaDeCriterio = (tipo: TipoReporte, campo: CampoCriterio): string =>
  tipo === 'CLIENTES_FRECUENTES' && campo === 'montoMinimo' ? 'Total comprado desde (Bs)' : ETIQUETA_CRITERIO[campo];

/** Opciones de orden de cada reporte; la primera es el orden normal. */
export const ORDENES_POR_TIPO: Partial<Record<TipoReporte, { valor: string; texto: string }[]>> = {
  VENTAS: [{ valor: '', texto: 'Orden normal' }, { valor: 'monto', texto: 'Mayor monto primero' }],
  INVENTARIO_VALORIZADO: [{ valor: '', texto: 'Orden normal' }, { valor: 'valor', texto: 'Mayor valor primero' }],
  CUENTAS_POR_COBRAR: [{ valor: '', texto: 'Más antiguas primero' }, { valor: 'saldo', texto: 'Mayor saldo primero' }],
};

export const ETIQUETA_ESTADO_PAGO: Record<string, string> = {
  COMPLETADA: 'Completada',
  PENDIENTE_PAGO: 'Pendiente',
  CANCELADA: 'Cancelada',
};

const normalizar = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

const contiene = (valor: unknown, buscado?: string) =>
  !buscado || !buscado.trim() || normalizar(String(valor ?? '')).includes(normalizar(buscado));

/** True si el criterio tiene algún valor puesto (los campos vacíos no filtran). */
const puesto = (c: CriteriosReporte, campo: CampoCriterio) => {
  const v = c[campo];
  return v !== undefined && v !== '' && v !== false && !(typeof v === 'number' && Number.isNaN(v));
};

/** Solo los criterios que corresponden al tipo y tienen valor, para guardarlos y mostrarlos. */
export function criteriosVigentes(tipo: TipoReporte, c: CriteriosReporte): CriteriosReporte {
  const salida: CriteriosReporte = {};
  for (const campo of CAMPOS_POR_TIPO[tipo]) {
    if (puesto(c, campo)) (salida as Record<string, unknown>)[campo] = c[campo];
  }
  return salida;
}

/** Texto corto de cada criterio aplicado, para los chips de "Reportes recientes". */
export function resumenCriterios(c: CriteriosReporte): string[] {
  const resumen: string[] = [];
  (Object.keys(c) as CampoCriterio[]).forEach((campo) => {
    if (!puesto(c, campo)) return;
    if (campo === 'soloActivos') resumen.push('Solo activos');
    else if (campo === 'estadoPago') resumen.push(`Pago: ${ETIQUETA_ESTADO_PAGO[c.estadoPago!] ?? c.estadoPago}`);
    else if (campo === 'minimoCompras') resumen.push(`${c.minimoCompras}+ compras`);
    else if (campo === 'minimoDias') resumen.push(`${c.minimoDias}+ días`);
    else if (campo === 'orden') resumen.push(c.orden === 'monto' ? 'Mayor monto primero' : c.orden === 'valor' ? 'Mayor valor primero' : 'Mayor saldo primero');
    else resumen.push(`${ETIQUETA_CRITERIO[campo]}: ${c[campo]}`);
  });
  return resumen;
}

/** Cuántos resultados hay que pedirle al servidor para poder filtrar y todavía llegar al límite pedido. */
export function limiteAPedir(tipo: TipoReporte, limite: number, c: CriteriosReporte): number {
  const filtra = CAMPOS_POR_TIPO[tipo].some((campo) => puesto(c, campo));
  return filtra ? Math.max(limite, 50) : limite;
}

const suma = (valores: number[]) => valores.reduce((a, b) => a + (Number(b) || 0), 0);

/**
 * Aplica los criterios al resultado que devolvió el servidor y recalcula los totales que
 * se muestran arriba de cada tabla, para que coincidan con las filas que quedaron.
 */
export function aplicarCriterios(tipo: TipoReporte, data: any, c: CriteriosReporte, limite?: number): any {
  if (!data) return data;
  const v = (campo: CampoCriterio) => puesto(c, campo);

  switch (tipo) {
    case 'VENTAS': {
      const ventas = (data.ventas ?? []).filter(
        (x: any) =>
          (!v('estadoPago') || x.estado === c.estadoPago) &&
          (!v('metodoPago') || x.metodoPago === c.metodoPago) &&
          contiene(x.nombreCliente, c.cliente) &&
          (!v('montoMinimo') || x.montoTotal >= (c.montoMinimo ?? 0)) &&
          (!v('montoMaximo') || x.montoTotal <= (c.montoMaximo ?? 0))
      );
      if (c.orden === 'monto') ventas.sort((p: any, q: any) => q.montoTotal - p.montoTotal);
      const monto = suma(ventas.map((x: any) => x.montoTotal));
      return {
        ...data,
        ventas,
        totalVentas: ventas.length,
        montoTotalVentas: monto,
        ticketPromedio: ventas.length ? monto / ventas.length : 0,
      };
    }
    case 'PRODUCTOS_MAS_VENDIDOS': {
      const productos = (data.productos ?? [])
        .filter(
          (x: any) =>
            contiene(x.categoria, c.categoria) &&
            (!v('cantidadMinima') || x.cantidadVendida >= (c.cantidadMinima ?? 0))
        )
        .slice(0, limite ?? undefined);
      return { ...data, productos };
    }
    case 'CLIENTES_FRECUENTES': {
      const clientes = (data.clientes ?? [])
        .filter((x: any) => contiene(x.nombreCliente, c.cliente) && (!v('minimoCompras') || x.cantidadCompras >= (c.minimoCompras ?? 0)) && (!v('montoMinimo') || x.montoTotalCompras >= (c.montoMinimo ?? 0)))
        .slice(0, limite ?? undefined);
      return { ...data, clientes };
    }
    case 'INVENTARIO_VALORIZADO': {
      const inventarios = (data.inventarios ?? []).filter(
        (x: any) =>
          contiene(x.categoria, c.categoria) &&
          (!v('valorMinimo') || x.valorTotal >= (c.valorMinimo ?? 0))
      );
      if (c.orden === 'valor') inventarios.sort((p: any, q: any) => q.valorTotal - p.valorTotal);
      return {
        ...data,
        inventarios,
        totalProductos: inventarios.length,
        valorTotal: suma(inventarios.map((x: any) => x.valorTotal)),
      };
    }
    case 'INVENTARIO_STOCK_BAJO': {
      const productos = (data.productos ?? []).filter((x: any) => contiene(x.categoria, c.categoria));
      return { ...data, productos, totalProductos: productos.length };
    }
    case 'VENTAS_POR_CATEGORIA':
      return Array.isArray(data) ? data.filter((x: any) => contiene(x.categoria, c.categoria)) : data;
    case 'VENTAS_POR_PRODUCTO':
      return Array.isArray(data)
        ? data.filter(
            (x: any) =>
              (contiene(x.nombreProducto, c.producto) || contiene(x.skuProducto, c.producto)) &&
              contiene(x.categoria, c.categoria)
          )
        : data;
    case 'VENTAS_POR_METODO_PAGO':
      return Array.isArray(data) ? data.filter((x: any) => !v('metodoPago') || x.metodoPago === c.metodoPago) : data;
    case 'PROVEEDORES': {
      const proveedores = (data.proveedores ?? []).filter(
        (x: any) => contiene(x.nombreProveedor, c.proveedor) && (!c.soloActivos || x.activo)
      );
      return {
        ...data,
        proveedores,
        totalProveedores: proveedores.length,
        proveedoresActivos: proveedores.filter((x: any) => x.activo).length,
        montoTotalCompras: suma(proveedores.map((x: any) => x.montoTotalCompras)),
      };
    }
    case 'CUENTAS_POR_COBRAR': {
      const ventas = (data.ventas ?? []).filter(
        (x: any) =>
          contiene(x.nombreCliente, c.cliente) &&
          (!v('minimoDias') || x.diasTranscurridos >= (c.minimoDias ?? 0)) &&
          (!v('saldoMinimo') || x.saldoPendiente >= (c.saldoMinimo ?? 0))
      );
      if (c.orden === 'saldo') ventas.sort((p: any, q: any) => q.saldoPendiente - p.saldoPendiente);
      return {
        ...data,
        ventas,
        cantidadVentasPendientes: ventas.length,
        totalPorCobrar: suma(ventas.map((x: any) => x.saldoPendiente)),
      };
    }
    default:
      return data;
  }
}

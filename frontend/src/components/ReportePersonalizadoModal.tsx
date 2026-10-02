'use client';

import { useState } from 'react';
import { X, Eye, AlertCircle } from 'lucide-react';
import { TipoReporte, ReporteReciente } from '@/types/reporte';
import {
  CAMPOS_POR_TIPO,
  ETIQUETA_CRITERIO,
  ETIQUETA_ESTADO_PAGO,
  CriteriosReporte,
  CampoCriterio,
  criteriosVigentes,
} from '@/lib/reporteCriterios';
import ReporteVistaPrevia from './ReporteVistaPrevia';

/** Lo que se puede consultar, con las fechas y el límite que pide cada tipo. */
export const TIPOS_PERSONALIZABLES: {
  id: TipoReporte;
  titulo: string;
  descripcion: string;
  fechas: boolean;
  limite: boolean;
}[] = [
  { id: 'VENTAS', titulo: 'Ventas', descripcion: 'Cada venta del período, con sus totales', fechas: true, limite: false },
  { id: 'VENTAS_POR_CATEGORIA', titulo: 'Ventas por categoría', descripcion: 'Cuánto se vendió de cada categoría', fechas: true, limite: false },
  { id: 'VENTAS_POR_METODO_PAGO', titulo: 'Ventas por método de pago', descripcion: 'Efectivo, transferencia y QR', fechas: true, limite: false },
  { id: 'PRODUCTOS_MAS_VENDIDOS', titulo: 'Productos más vendidos', descripcion: 'Los productos que más salen', fechas: false, limite: true },
  { id: 'CLIENTES_FRECUENTES', titulo: 'Clientes frecuentes', descripcion: 'Quiénes compran más seguido', fechas: false, limite: true },
  { id: 'CUENTAS_POR_COBRAR', titulo: 'Cuentas por cobrar', descripcion: 'Ventas con saldo pendiente', fechas: false, limite: false },
  { id: 'INVENTARIO_VALORIZADO', titulo: 'Inventario valorizado', descripcion: 'Cuánto vale el stock actual', fechas: false, limite: false },
  { id: 'INVENTARIO_STOCK_BAJO', titulo: 'Stock bajo', descripcion: 'Productos que hay que reponer', fechas: false, limite: false },
  { id: 'PROVEEDORES', titulo: 'Proveedores', descripcion: 'Compras por proveedor', fechas: false, limite: false },
  { id: 'FINANCIERO', titulo: 'Financiero', descripcion: 'Ingresos, gastos y ganancia del período', fechas: true, limite: false },
];

const aFechaInput = (fecha: Date) => {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
};

interface ReportePersonalizadoModalProps {
  /** Un reporte reciente que se vuelve a abrir: arranca con sus mismos datos y directo en el resultado. */
  reciente?: ReporteReciente;
  onClose: () => void;
  /** Se llama al ver un reporte nuevo, para sumarlo a "Reportes recientes". */
  onGenerado: (reporte: ReporteReciente) => void;
}

const inputClase =
  'w-full px-3 py-1.5 border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-primary-500';

export default function ReportePersonalizadoModal({ reciente, onClose, onGenerado }: ReportePersonalizadoModalProps) {
  const hoy = new Date();
  const [tipo, setTipo] = useState<TipoReporte>(reciente?.tipo ?? 'VENTAS');
  const [fechaInicio, setFechaInicio] = useState(
    reciente?.fechaInicio ?? aFechaInput(new Date(hoy.getFullYear(), hoy.getMonth(), 1))
  );
  const [fechaFin, setFechaFin] = useState(reciente?.fechaFin ?? aFechaInput(hoy));
  const [limite, setLimite] = useState(reciente?.limite ?? 10);
  const [criterios, setCriterios] = useState<CriteriosReporte>(reciente?.criterios ?? {});
  const [verResultado, setVerResultado] = useState(!!reciente);
  const [error, setError] = useState<string | null>(null);

  const config = TIPOS_PERSONALIZABLES.find((t) => t.id === tipo)!;
  const campos = CAMPOS_POR_TIPO[tipo];

  const cambiarTipo = (nuevo: TipoReporte) => {
    setTipo(nuevo);
    setCriterios({});
    setError(null);
  };

  const poner = (campo: CampoCriterio, valor: unknown) =>
    setCriterios((actuales) => ({ ...actuales, [campo]: valor }));

  const vigentes = criteriosVigentes(tipo, criterios);

  const verReporte = () => {
    if (config.fechas && (!fechaInicio || !fechaFin)) {
      setError('Elegí el rango de fechas.');
      return;
    }
    if (config.fechas && fechaInicio > fechaFin) {
      setError('La fecha de inicio no puede ser posterior a la fecha de fin.');
      return;
    }
    setError(null);
    if (!reciente) {
      onGenerado({
        id: `${Date.now()}`,
        tipo,
        titulo: config.titulo,
        fechaInicio: config.fechas ? fechaInicio : undefined,
        fechaFin: config.fechas ? fechaFin : undefined,
        limite: config.limite ? limite : undefined,
        criterios: vigentes,
        generado: new Date().toISOString(),
      });
    }
    setVerResultado(true);
  };

  const campo = (c: CampoCriterio) => {
    const etiqueta = ETIQUETA_CRITERIO[c];
    switch (c) {
      case 'estadoPago':
        return (
          <select className={inputClase} value={criterios.estadoPago ?? ''} onChange={(e) => poner(c, e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ETIQUETA_ESTADO_PAGO).map(([valor, texto]) => (
              <option key={valor} value={valor}>{texto}</option>
            ))}
          </select>
        );
      case 'metodoPago':
        return (
          <select className={inputClase} value={criterios.metodoPago ?? ''} onChange={(e) => poner(c, e.target.value)}>
            <option value="">Todos</option>
            <option value="EFECTIVO">Efectivo</option>
            <option value="TRANSFERENCIA">Transferencia</option>
            <option value="QR">QR</option>
            <option value="VARIOS">Varios</option>
          </select>
        );
      case 'minimoCompras':
      case 'minimoDias':
        return (
          <input
            type="number"
            min={0}
            className={inputClase}
            value={criterios[c] ?? ''}
            onChange={(e) => poner(c, e.target.value === '' ? undefined : Number(e.target.value))}
            placeholder="Sin mínimo"
          />
        );
      case 'soloActivos':
        return (
          <label className="flex items-center gap-2 py-1.5 text-sm text-gray-700">
            <input
              type="checkbox"
              checked={!!criterios.soloActivos}
              onChange={(e) => poner(c, e.target.checked)}
              className="h-4 w-4 text-primary-600 border-gray-300 rounded"
            />
            {etiqueta}
          </label>
        );
      default:
        return (
          <input
            type="text"
            className={inputClase}
            value={(criterios[c] as string | undefined) ?? ''}
            onChange={(e) => poner(c, e.target.value)}
            placeholder="Todos"
          />
        );
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-3">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden">
        <div className="flex items-center justify-between px-5 py-2.5 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100 shrink-0">
          <h2 className="text-base font-bold text-gray-900">
            {verResultado ? `Reporte de ${config.titulo.toLowerCase()}` : 'Generar reporte personalizado'}
          </h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600" title="Cerrar">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {verResultado ? (
            <ReporteVistaPrevia
              tipoReporte={tipo}
              fechaInicio={fechaInicio}
              fechaFin={fechaFin}
              limite={limite}
              criterios={vigentes}
              onVolver={reciente ? onClose : () => setVerResultado(false)}
              textoVolver={reciente ? 'Volver a reportes' : 'Cambiar criterios'}
            />
          ) : (
            <div className="space-y-4">
              {error && (
                <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                  <AlertCircle className="text-red-600 flex-shrink-0" size={18} />
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">¿Qué querés consultar?</label>
                <select className={inputClase} value={tipo} onChange={(e) => cambiarTipo(e.target.value as TipoReporte)}>
                  {TIPOS_PERSONALIZABLES.map((t) => (
                    <option key={t.id} value={t.id}>{t.titulo}</option>
                  ))}
                </select>
                <p className="text-xs text-gray-500 mt-1">{config.descripcion}</p>
              </div>

              {(config.fechas || config.limite || campos.length > 0) && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  {config.fechas && (
                    <>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Desde</label>
                        <input type="date" className={inputClase} value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Hasta</label>
                        <input type="date" className={inputClase} value={fechaFin} onChange={(e) => setFechaFin(e.target.value)} />
                      </div>
                    </>
                  )}
                  {config.limite && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Cantidad de resultados</label>
                      <select className={inputClase} value={limite} onChange={(e) => setLimite(Number(e.target.value))}>
                        <option value={5}>Los 5 primeros</option>
                        <option value={10}>Los 10 primeros</option>
                        <option value={20}>Los 20 primeros</option>
                        <option value={50}>Los 50 primeros</option>
                      </select>
                    </div>
                  )}
                  {campos.map((c) => (
                    <div key={c}>
                      {c !== 'soloActivos' && (
                        <label className="block text-sm font-medium text-gray-700 mb-1">{ETIQUETA_CRITERIO[c]}</label>
                      )}
                      {campo(c)}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {!verResultado && (
          <div className="flex justify-end gap-3 px-5 py-2.5 border-t border-gray-200 bg-white shrink-0">
            <button
              onClick={onClose}
              className="px-4 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium"
            >
              Cancelar
            </button>
            <button
              onClick={verReporte}
              className="inline-flex items-center gap-2 px-4 py-1.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
            >
              <Eye size={18} />
              Ver reporte
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

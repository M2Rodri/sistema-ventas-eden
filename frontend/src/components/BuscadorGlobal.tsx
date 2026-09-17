'use client';

import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { Search, Package, User, Truck, X } from 'lucide-react';
import { searchProductos, searchClientes, searchProveedores } from '@/lib/api';

/**
 * Buscador de la cabecera.
 *
 * Antes era un <input> suelto, sin onChange ni handler: se podía escribir y no
 * pasaba absolutamente nada.
 *
 * Busca en las tres entidades que uno quiere encontrar rápido desde cualquier
 * pantalla, usando los endpoints de búsqueda que el backend ya tenía. Al elegir
 * un resultado se va a la pantalla que corresponde, con el filtro ya puesto en
 * lo que se buscó (parámetro ?q=), así el elemento queda a la vista y no hay
 * que volver a buscarlo ahí adentro.
 */

/**
 * Espera antes de consultar, en milisegundos.
 *
 * Sin esto se dispararían tres pedidos al backend por cada tecla. Con 350 ms
 * se consulta cuando la persona deja de escribir.
 */
const ESPERA_ANTES_DE_BUSCAR = 350;

/** Mínimo de caracteres para que valga la pena consultar. */
const MINIMO_CARACTERES = 2;

/** Cuántos resultados mostrar por tipo, para que el desplegable no se dispare. */
const MAXIMO_POR_TIPO = 4;

interface Resultado {
  clave: string;
  titulo: string;
  detalle: string;
  destino: string;
  tipo: 'producto' | 'cliente' | 'proveedor';
}

export default function BuscadorGlobal() {
  const router = useRouter();
  const [texto, setTexto] = useState('');
  const [resultados, setResultados] = useState<Resultado[]>([]);
  const [buscando, setBuscando] = useState(false);
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const consulta = texto.trim();

    if (consulta.length < MINIMO_CARACTERES) {
      setResultados([]);
      setBuscando(false);
      return;
    }

    setBuscando(true);
    // Se guarda el temporizador para cancelarlo si la persona sigue escribiendo.
    const temporizador = setTimeout(async () => {
      // allSettled: que falle la búsqueda de proveedores no debe dejar sin
      // resultados a productos y clientes.
      const [rProductos, rClientes, rProveedores] = await Promise.allSettled([
        searchProductos(consulta),
        searchClientes(consulta),
        searchProveedores(consulta),
      ]);

      const encontrados: Resultado[] = [];

      if (rProductos.status === 'fulfilled') {
        rProductos.value.slice(0, MAXIMO_POR_TIPO).forEach((p) =>
          encontrados.push({
            clave: `producto-${p.id}`,
            titulo: p.nombre,
            detalle: p.sku ? `SKU ${p.sku}` : 'Producto',
            destino: `/dashboard/productos?q=${encodeURIComponent(consulta)}`,
            tipo: 'producto',
          })
        );
      }

      if (rClientes.status === 'fulfilled') {
        rClientes.value.slice(0, MAXIMO_POR_TIPO).forEach((c) =>
          encontrados.push({
            clave: `cliente-${c.id}`,
            titulo: c.nombreCompleto,
            detalle: c.telefono || c.email || 'Cliente',
            destino: `/dashboard/clientes?q=${encodeURIComponent(consulta)}`,
            tipo: 'cliente',
          })
        );
      }

      if (rProveedores.status === 'fulfilled') {
        rProveedores.value.slice(0, MAXIMO_POR_TIPO).forEach((p) =>
          encontrados.push({
            clave: `proveedor-${p.id}`,
            titulo: p.nombreEmpresa,
            detalle: p.telefono || 'Proveedor',
            destino: `/dashboard/proveedores?q=${encodeURIComponent(consulta)}`,
            tipo: 'proveedor',
          })
        );
      }

      setResultados(encontrados);
      setBuscando(false);
      setAbierto(true);
    }, ESPERA_ANTES_DE_BUSCAR);

    return () => clearTimeout(temporizador);
  }, [texto]);

  // Cerrar el desplegable al hacer clic afuera.
  useEffect(() => {
    const alClicar = (e: MouseEvent) => {
      if (contenedor.current && !contenedor.current.contains(e.target as Node)) {
        setAbierto(false);
      }
    };
    document.addEventListener('mousedown', alClicar);
    return () => document.removeEventListener('mousedown', alClicar);
  }, []);

  const ir = (destino: string) => {
    setAbierto(false);
    setTexto('');
    router.push(destino);
  };

  const limpiar = () => {
    setTexto('');
    setResultados([]);
    setAbierto(false);
  };

  const iconoDe = (tipo: Resultado['tipo']) => {
    if (tipo === 'producto') return <Package size={16} className="text-primary-600" />;
    if (tipo === 'cliente') return <User size={16} className="text-green-600" />;
    return <Truck size={16} className="text-orange-600" />;
  };

  const consultaValida = texto.trim().length >= MINIMO_CARACTERES;

  return (
    <div className="relative w-96" ref={contenedor}>
      <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-4 py-2">
        <Search size={18} className="flex-shrink-0 text-gray-400" />
        <input
          type="text"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onFocus={() => resultados.length > 0 && setAbierto(true)}
          onKeyDown={(e) => e.key === 'Escape' && limpiar()}
          placeholder="Buscar producto, cliente o proveedor…"
          className="w-full bg-transparent text-sm text-gray-700 outline-none placeholder:text-gray-400"
        />
        {texto && (
          <button
            onClick={limpiar}
            className="flex-shrink-0 text-gray-400 transition-colors hover:text-gray-600"
            title="Limpiar"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {abierto && consultaValida && (
        <div className="absolute left-0 right-0 z-50 mt-2 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
          {buscando ? (
            <p className="px-4 py-6 text-center text-sm text-gray-500">Buscando…</p>
          ) : resultados.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-gray-500">
              Nada coincide con «{texto.trim()}»
            </p>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              {resultados.map((r) => (
                <button
                  key={r.clave}
                  onClick={() => ir(r.destino)}
                  className="flex w-full items-center gap-3 border-b border-gray-100 px-4 py-3 text-left transition-colors hover:bg-gray-50"
                >
                  <span className="flex-shrink-0">{iconoDe(r.tipo)}</span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium text-gray-900">
                      {r.titulo}
                    </span>
                    <span className="block truncate text-xs text-gray-500">{r.detalle}</span>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Boxes, ScanLine, Truck } from 'lucide-react';
import { getDatosNegocio, DatosNegocio } from '@/lib/api';

/**
 * Página "Nosotros".
 *
 * El contenido descriptivo se limita a lo que el sistema puede sostener: qué
 * vende el negocio, dónde está y cómo trabaja. Los datos de contacto no están
 * escritos acá: salen de configuracion_sistema, para que se corrijan desde la
 * pantalla de Configuración sin tocar el código.
 */
export default function NosotrosPage() {
  const [negocio, setNegocio] = useState<DatosNegocio>({});

  useEffect(() => {
    let activo = true;
    getDatosNegocio()
      .then((d) => activo && setNegocio(d))
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, []);

  const nombre = negocio.negocio_razon_social || 'Mueblería Edén';
  const ciudad = negocio.negocio_ciudad || 'Santa Cruz de la Sierra';

  return (
    <div className="bg-gradient-to-b from-gray-50 to-gray-100 min-h-[calc(100vh-16rem)]">
      {/* Encabezado */}
      <section className="bg-gradient-to-r from-gray-900 to-gray-700 text-white">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
          <h1 className="text-3xl md:text-5xl font-bold mb-4">Sobre nosotros</h1>
          <p className="text-lg md:text-xl max-w-2xl mx-auto opacity-90">
            {nombre} · {ciudad}, Bolivia
          </p>
        </div>
      </section>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-12">
        {/* Quiénes somos */}
        <section className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8 md:p-10">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Quiénes somos</h2>
          <div className="space-y-4 text-gray-700 leading-relaxed max-w-3xl">
            <p>
              {nombre} es un negocio familiar de {ciudad} dedicado a la venta de
              camas, colchones y accesorios de descanso. Trabajamos con
              proveedores nacionales y atendemos tanto a familias como a
              hoteles y hospedajes de la ciudad.
            </p>
            <p>
              Creemos que elegir dónde dormir no debería ser una decisión
              apurada. Por eso mostramos de cada producto lo que realmente
              importa —medidas, firmeza y material del núcleo— y ofrecemos la
              posibilidad de verlo en 3D antes de venir al local.
            </p>
          </div>
        </section>

        {/* Cómo trabajamos */}
        <section>
          <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">Cómo trabajamos</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
              <Boxes className="text-primary-600 mb-3" size={28} />
              <h3 className="font-semibold text-gray-900 mb-2">Producto que se explica solo</h3>
              <p className="text-sm text-gray-600">
                Cada artículo publica sus medidas, su firmeza y de qué está
                hecho por dentro, para que compares sin depender de una foto.
              </p>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
              <ScanLine className="text-primary-600 mb-3" size={28} />
              <h3 className="font-semibold text-gray-900 mb-2">Visualización 3D y realidad aumentada</h3>
              <p className="text-sm text-gray-600">
                Podés ver los modelos en tres dimensiones desde tu celular y
                hacerte una idea del tamaño real antes de acercarte al local.
              </p>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
              <Truck className="text-primary-600 mb-3" size={28} />
              <h3 className="font-semibold text-gray-900 mb-2">Entrega coordinada</h3>
              <p className="text-sm text-gray-600">
                Coordinamos la entrega a domicilio con transportadoras de la
                ciudad y te informamos la fecha estimada al cerrar la compra.
              </p>
            </div>
          </div>
        </section>

        {/*
          Aca habia un bloque "Donde encontrarnos" con direccion, horario,
          telefono, WhatsApp y correo: exactamente los mismos datos que ya
          muestra la pagina de Contacto, y ahi con mas detalle.
          Tener la informacion de contacto repartida en dos paginas obliga al
          visitante a decidir cual mirar, y deja sin proposito claro a la
          seccion de Contacto. "Nosotros" cuenta quienes somos; los datos para
          escribirnos viven en un solo lugar.
        */}
        <section className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8 md:p-10">
          <h2 className="text-2xl font-bold text-gray-900 mb-3">Pasá a vernos</h2>
          <p className="text-gray-700">
            Atendemos en nuestro local de {ciudad}.
            La dirección, los horarios y todas las formas de escribirnos están en la
            página de contacto.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row gap-3">
            <Link
              href="/tienda/contacto"
              className="inline-flex items-center justify-center bg-primary-600 hover:bg-primary-700 text-white font-medium py-3 px-6 rounded-lg transition-colors"
            >
              Ver dirección y contacto
            </Link>
            <Link
              href="/tienda/productos"
              className="inline-flex items-center justify-center bg-white border border-gray-300 hover:bg-gray-50 text-gray-800 font-medium py-3 px-6 rounded-lg transition-colors"
            >
              Ver el catálogo
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}

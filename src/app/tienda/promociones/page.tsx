'use client';

import { useState, useEffect } from 'react';
import { getPromocionesVigentes } from '@/lib/api';
import { Promocion } from '@/types/promocion';
import { Tag, Clock, Percent, Sparkles, TrendingDown, Calendar, AlertCircle, ArrowRight, Gift, Zap } from 'lucide-react';
import Link from 'next/link';

export default function PromocionesPage() {
  const [promociones, setPromocions] = useState<Promocion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadPromocions();
  }, []);

  const loadPromocions = async () => {
    try {
      setLoading(true);
      const data = await getPromocionesVigentes();
      setPromocions(data);
    } catch (err: any) {
      setError(err.message || 'Error al cargar promociones');
    } finally {
      setLoading(false);
    }
  };

  const calcularDiasRestantes = (fechaFin: string) => {
    const hoy = new Date();
    const fin = new Date(fechaFin);
    const diff = Math.ceil((fin.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 0;
  };

  const formatearFecha = (fecha: string) => {
    return new Date(fecha).toLocaleDateString('es-BO', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-secondary-50 flex items-center justify-center">
        <div className="text-center">
          <div className="relative">
            <div className="animate-spin rounded-full h-20 w-20 border-t-4 border-b-4 border-primary-600 mx-auto"></div>
            <Gift className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-8 h-8 text-primary-600 animate-pulse" />
          </div>
          <p className="text-gray-600 font-semibold mt-6 text-lg">Cargando promociones increíbles...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-secondary-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl shadow-2xl p-10 max-w-md text-center border-2 border-red-100">
          <div className="bg-red-100 rounded-full w-20 h-20 flex items-center justify-center mx-auto mb-6">
            <AlertCircle className="w-12 h-12 text-red-600" />
          </div>
          <h2 className="text-3xl font-extrabold text-gray-900 mb-3">¡Ups! Algo salió mal</h2>
          <p className="text-gray-600 mb-8 leading-relaxed">{error}</p>
          <button
            onClick={loadPromocions}
            className="bg-gradient-to-r from-primary-600 to-primary-700 text-white px-8 py-4 rounded-2xl hover:from-primary-700 hover:to-primary-800 transition-all duration-300 font-bold shadow-lg hover:shadow-xl transform hover:scale-105"
          >
            Reintentar Ahora
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-secondary-50">
      {/* Hero Section Mejorado */}
      <div className="relative bg-gradient-to-r from-primary-600 via-primary-700 to-primary-800 text-white py-20 px-4 sm:px-6 lg:px-8 overflow-hidden">
        {/* Efectos de fondo animados */}
        <div className="absolute inset-0 bg-black opacity-5"></div>
        <div className="absolute inset-0">
          <div className="absolute top-10 left-10 w-40 h-40 bg-yellow-300 opacity-20 rounded-full blur-3xl animate-pulse"></div>
          <div className="absolute bottom-10 right-10 w-56 h-56 bg-orange-400 opacity-20 rounded-full blur-3xl animate-pulse delay-75"></div>
          <div className="absolute top-1/2 left-1/3 w-32 h-32 bg-white opacity-10 rounded-full blur-2xl"></div>
        </div>
        
        <div className="max-w-7xl mx-auto relative z-10">
          <div className="text-center">
            {/* Badge animado */}
            <div className="inline-flex items-center gap-3 bg-white/20 backdrop-blur-lg px-6 py-3 rounded-full mb-6 border border-white/30 shadow-xl animate-bounce-slow">
              <Sparkles className="w-6 h-6 text-yellow-300 animate-pulse" />
              <span className="text-base font-bold tracking-wide">Promociones Exclusivas del Mes</span>
              <Zap className="w-6 h-6 text-yellow-300 animate-pulse" />
            </div>
            
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-black mb-6 leading-tight">
              ¡Promociones
              <span className="block text-yellow-300 mt-2">Imperdibles!</span>
            </h1>
            
            <p className="text-xl md:text-2xl lg:text-3xl text-primary-100 max-w-4xl mx-auto font-medium leading-relaxed">
              Descubre las mejores promociones en <span className="text-yellow-300 font-bold">camas</span>, 
              <span className="text-yellow-300 font-bold"> colchones</span> y 
              <span className="text-yellow-300 font-bold"> almohadas</span> premium
            </p>
            
            {/* Contador de promociones */}
            {promociones.length > 0 && (
              <div className="mt-8 inline-block">
                <div className="bg-white/10 backdrop-blur-md border-2 border-white/30 rounded-2xl px-8 py-4 shadow-2xl">
                  <p className="text-sm font-semibold text-primary-100 mb-1">Promociones Activas</p>
                  <p className="text-5xl font-black text-white">{promociones.length}</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Decoración de ondas */}
        <div className="absolute bottom-0 left-0 right-0">
          <svg viewBox="0 0 1440 120" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full">
            <path d="M0,64L80,69.3C160,75,320,85,480,80C640,75,800,53,960,48C1120,43,1280,53,1360,58.7L1440,64L1440,120L1360,120C1280,120,1120,120,960,120C800,120,640,120,480,120C320,120,160,120,80,120L0,120Z" fill="rgb(249, 250, 251)" fillOpacity="1"></path>
          </svg>
        </div>
      </div>

      {/* Promociones Section Mejorado */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 -mt-8 relative z-10">
        {promociones.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-3xl shadow-2xl border-2 border-gray-100">
            <div className="bg-gray-100 rounded-full w-32 h-32 flex items-center justify-center mx-auto mb-6">
              <Tag className="w-16 h-16 text-gray-400" />
            </div>
            <h3 className="text-3xl font-bold text-gray-800 mb-3">No hay promociones disponibles</h3>
            <p className="text-gray-500 text-lg mb-8">Vuelve pronto para ver nuestras próximas promociones</p>
            <Link 
              href="/tienda/productos"
              className="inline-flex items-center gap-3 bg-primary-600 text-white px-8 py-4 rounded-2xl hover:bg-primary-700 transition-all duration-300 font-bold shadow-lg hover:shadow-xl transform hover:scale-105"
            >
              Explorar Catálogo
              <ArrowRight className="w-5 h-5" />
            </Link>
          </div>
        ) : (
          <div className="space-y-16">
            {promociones.map((promocion, index) => (
              <div 
                key={promocion.id} 
                className="bg-white rounded-3xl shadow-2xl overflow-hidden border-2 border-primary-100 hover:border-primary-300 transition-all duration-500 transform hover:scale-[1.02] hover:shadow-3xl"
                style={{
                  animationDelay: `${index * 100}ms`,
                  animation: 'slideUp 0.6s ease-out'
                }}
              >
                {/* Header de la Promocion Mejorado */}
                <div className="bg-gradient-to-r from-primary-600 via-primary-700 to-primary-800 p-8 relative overflow-hidden">
                  {/* Efectos decorativos */}
                  <div className="absolute top-0 right-0 w-64 h-64 bg-white opacity-5 rounded-full blur-3xl"></div>
                  <div className="absolute bottom-0 left-0 w-48 h-48 bg-yellow-300 opacity-10 rounded-full blur-2xl"></div>
                  
                  <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6 relative z-10">
                    <div className="flex-1">
                      <div className="flex items-start gap-4 mb-4">
                        <div className="bg-white/20 backdrop-blur-sm p-3 rounded-2xl border border-white/30 shadow-lg">
                          <TrendingDown className="w-8 h-8 text-white" />
                        </div>
                        <div>
                          {/*
                            El título es el nombre de la promoción. Antes se
                            usaba la descripción porque la entidad no tenía
                            nombre, aunque la columna existía y era obligatoria.
                          */}
                          <h2 className="text-3xl md:text-4xl font-black text-white mb-2 leading-tight">
                            {promocion.nombre}
                          </h2>
                          {promocion.descripcion && (
                            <p className="text-primary-100 mb-3 max-w-2xl">
                              {promocion.descripcion}
                            </p>
                          )}
                          <div className="flex flex-wrap items-center gap-4 text-sm text-primary-100">
                            <div className="flex items-center gap-2 bg-white/10 backdrop-blur-sm px-4 py-2 rounded-full border border-white/20">
                              <Calendar className="w-4 h-4" />
                              <span className="font-semibold">
                                {formatearFecha(promocion.fechaInicio)} - {formatearFecha(promocion.fechaFin)}
                              </span>
                            </div>
                            <div className="flex items-center gap-2 bg-yellow-400/20 backdrop-blur-sm px-4 py-2 rounded-full border border-yellow-300/30">
                              <Clock className="w-4 h-4 text-yellow-300" />
                              <span className="font-bold text-yellow-300">
                                ¡{calcularDiasRestantes(promocion.fechaFin)} días restantes!
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    {/* Badge de Descuento Mejorado */}
                    <div className="flex-shrink-0">
                      <div className="relative">
                        <div className="absolute inset-0 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-3xl blur-xl opacity-50 animate-pulse"></div>
                        <div className="relative bg-gradient-to-br from-yellow-400 via-orange-500 to-red-500 text-white rounded-3xl px-8 py-6 text-center shadow-2xl transform hover:scale-110 transition-all duration-300 border-4 border-white">
                          <div className="flex items-center justify-center gap-3 mb-2">
                            <Percent className="w-10 h-10 font-black" />
                            <span className="text-6xl font-black">{promocion.descuento}</span>
                          </div>
                          <p className="text-base font-black uppercase tracking-widest">OFF</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Productos de la Promocion Mejorado */}
                <div className="p-8 bg-gradient-to-br from-gray-50 to-white">
                  <div className="flex items-center gap-3 mb-8">
                    <div className="bg-primary-100 p-2 rounded-xl">
                      <Tag className="w-6 h-6 text-primary-600" />
                    </div>
                    <h3 className="text-2xl font-bold text-gray-800">
                      Productos en Promoción
                      <span className="text-primary-600 ml-2">({promocion.productos.length})</span>
                    </h3>
                  </div>
                  
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {promocion.productos.map((producto) => (
                      <div 
                        key={producto.id} 
                        className="group bg-white rounded-2xl border-2 border-gray-200 hover:border-primary-400 transition-all duration-300 overflow-hidden shadow-lg hover:shadow-2xl transform hover:-translate-y-2"
                      >
                        {/* Contenido del Producto */}
                        <div className="p-6 relative">
                          {/* Badge de descuento */}
                          <div className="absolute top-4 right-4 z-10">
                            <div className="bg-gradient-to-r from-red-500 to-red-600 text-white px-4 py-2 rounded-full text-sm font-black shadow-lg transform rotate-3 hover:rotate-0 transition-transform">
                              -{promocion.descuento}% OFF
                            </div>
                          </div>

                          {/* Info del producto */}
                          <div className="mb-4">
                            <h4 className="font-bold text-gray-900 text-lg mb-2 line-clamp-2 group-hover:text-primary-600 transition-colors leading-tight">
                              {producto.nombre}
                            </h4>
                            <div className="flex items-center gap-2">
                              <span className="text-xs text-gray-500 bg-gray-100 px-3 py-1 rounded-full font-medium">
                                SKU: {producto.sku}
                              </span>
                            </div>
                          </div>

                          {/* Botón */}
                          <button className="w-full bg-gradient-to-r from-primary-600 to-primary-700 hover:from-primary-700 hover:to-primary-800 text-white font-bold py-4 px-6 rounded-xl transition-all duration-300 transform group-hover:scale-105 shadow-md hover:shadow-xl flex items-center justify-center gap-3 relative overflow-hidden">
                            <span className="absolute inset-0 bg-white opacity-0 group-hover:opacity-20 transition-opacity"></span>
                            <span className="relative z-10">Ver Producto</span>
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform relative z-10" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* CTA Final Mejorado */}
      <div className="relative bg-gradient-to-r from-secondary-600 via-secondary-700 to-secondary-800 py-16 px-4 sm:px-6 lg:px-8 mt-20 overflow-hidden">
        {/* Efectos decorativos */}
        <div className="absolute inset-0 bg-black opacity-10"></div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-orange-400 opacity-10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-yellow-300 opacity-10 rounded-full blur-3xl"></div>
        
        <div className="max-w-5xl mx-auto text-center relative z-10">
          <div className="bg-white/10 backdrop-blur-sm rounded-full w-20 h-20 flex items-center justify-center mx-auto mb-6 border-2 border-white/30">
            <Gift className="w-10 h-10 text-white" />
          </div>
          <h3 className="text-4xl md:text-5xl font-black text-white mb-4 leading-tight">
            ¿No encontraste lo que buscabas?
          </h3>
          {/*
            Antes decía "más de 100 productos", una cifra que el catálogo no
            respalda. Se reemplazó por una invitación que no afirma cantidades.
          */}
          <p className="text-xl md:text-2xl text-secondary-100 mb-10 font-medium max-w-3xl mx-auto leading-relaxed">
            Explora nuestro <span className="text-yellow-300 font-bold">catálogo completo</span> de camas, colchones y accesorios de descanso
          </p>
          <Link 
            href="/tienda/productos"
            className="inline-flex items-center gap-3 bg-white text-secondary-700 font-black py-5 px-10 rounded-2xl hover:bg-secondary-50 transition-all duration-300 transform hover:scale-105 shadow-2xl text-lg group"
          >
            <span>Ver Catálogo Completo</span>
            <ArrowRight className="w-6 h-6 group-hover:translate-x-2 transition-transform" />
          </Link>
        </div>
      </div>

      {/* Animaciones CSS */}
      <style jsx>{`
        @keyframes slideUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        @keyframes bounce-slow {
          0%, 100% {
            transform: translateY(0);
          }
          50% {
            transform: translateY(-5px);
          }
        }

        .animate-bounce-slow {
          animation: bounce-slow 2s infinite;
        }
      `}</style>
    </div>
  );
}
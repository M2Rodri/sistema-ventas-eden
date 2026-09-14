'use client';

import React, { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { BACKEND_URL } from '@/lib/api';
import Script from 'next/script';

interface MultimediaProducto {
  id: number;
  productoId: number;
  urlModelo3d: string | null;
  urlVistaPrevia: string | null;
  habilitadoRa: boolean;
  activo: boolean;
}

interface Producto {
  id: number;
  nombre: string;
  descripcion: string;
  precioVenta: number;
  nombreCategoria: string;
}

interface ProductoConMultimedia extends Producto {
  multimedia: MultimediaProducto;
}

// Componente para mostrar galería de productos con 3D
function GaleriaProductos3D({ productos }: { productos: ProductoConMultimedia[] }) {

  if (productos.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
          <div className="mx-auto bg-yellow-100 rounded-full w-16 h-16 flex items-center justify-center">
            <svg className="h-8 w-8 text-yellow-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900">Aún no hay productos en 3D</h2>
          <p className="text-gray-600">
            Estamos trabajando en agregar modelos 3D a nuestros productos.
          </p>
          <Link href="/tienda/productos" className="inline-block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
            Ver catálogo completo
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <Link href="/tienda/productos" className="inline-flex items-center text-blue-600 hover:text-blue-700 mb-4">
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Volver al catálogo
        </Link>
        <h1 className="text-4xl font-bold text-gray-900 mb-2">Explora en 3D y Realidad Aumentada</h1>
        <p className="text-lg text-gray-600">
          Productos disponibles con visualización 3D interactiva ({productos.length})
        </p>
      </div>

      {/* Grid de productos */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        {productos.map((producto) => {
          const posterUrl = producto.multimedia.urlVistaPrevia || '/images/placeholder-imagen.jpg';
          
          return (
            <Link 
              key={producto.id}
              href={`/tienda/visualizacion-3d-ar?id=${producto.id}`}
              className="group bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2"
            >
              {/* Imagen */}
              <div className="relative h-64 bg-gray-100 overflow-hidden">
                <img
                  src={producto.multimedia.urlVistaPrevia ? `${BACKEND_URL}${producto.multimedia.urlVistaPrevia}` : '/images/placeholder-imagen.jpg'}
                  alt={producto.nombre}
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300"
                />
                
                {/* Badge de AR */}
                {producto.multimedia.habilitadoRa && (
                  <div className="absolute top-4 right-4 bg-green-500 text-white px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                    </svg>
                    AR
                  </div>
                )}

                {/* Overlay con ícono 3D */}
                <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-40 transition-all duration-300 flex items-center justify-center">
                  <div className="transform scale-0 group-hover:scale-100 transition-transform duration-300">
                    <div className="bg-white rounded-full p-4">
                      <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              {/* Información */}
              <div className="p-6">
                <h3 className="text-xl font-bold text-gray-900 mb-2 group-hover:text-blue-600 transition">
                  {producto.nombre}
                </h3>
                <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                  {producto.descripcion}
                </p>
                <div className="flex items-center justify-between">
                  <span className="text-2xl font-bold text-blue-600">
                    Bs. {producto.precioVenta.toFixed(2)}
                  </span>
                  <span className="text-sm text-gray-500 bg-gray-100 px-3 py-1 rounded-full">
                    {producto.nombreCategoria}
                  </span>
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}

// Componente para visor individual
function VisorProducto({ producto, multimedia }: { producto: Producto; multimedia: MultimediaProducto }) {
  const [modelLoaded, setModelLoaded] = useState(false);
  const [scriptLoaded, setScriptLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loadingProgress, setLoadingProgress] = useState(0);
  const modelViewerRef = React.useRef<any>(null);

  const modelUrl = `${BACKEND_URL}${multimedia.urlModelo3d}`;
  const posterUrl = multimedia.urlVistaPrevia ? `${BACKEND_URL}${multimedia.urlVistaPrevia}` : undefined;

  // 🔍 DEBUG 1: Mostrar URLs en consola
  useEffect(() => {
    console.log('═══════════════════════════════════════════════════════');
    console.log('🔍 DEBUG - INICIO DE DIAGNÓSTICO DEL MODELO 3D');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📋 Información del Producto:');
    console.log('  - ID Producto:', producto.id);
    console.log('  - Nombre:', producto.nombre);
    console.log('  - ID Multimedia:', multimedia.id);
    console.log('');
    console.log('🌐 URLs Configuradas:');
    console.log('  - BACKEND_URL:', BACKEND_URL);
    console.log('  - multimedia.urlModelo3d (ruta relativa):', multimedia.urlModelo3d);
    console.log('  - URL COMPLETA del modelo:', modelUrl);
    console.log('  - multimedia.urlVistaPrevia:', multimedia.urlVistaPrevia);
    console.log('  - URL del poster:', posterUrl);
    console.log('');
    console.log('⚙️ Configuración AR:');
    console.log('  - AR habilitado:', multimedia.habilitadoRa);
    console.log('  - Multimedia activo:', multimedia.activo);
    console.log('═══════════════════════════════════════════════════════');
  }, []);

  // 🔍 DEBUG 2: Detectar cuando model-viewer está disponible
  useEffect(() => {
    console.log('⏳ Esperando que model-viewer se cargue...');
    
    const checkModelViewer = setInterval(() => {
      if (typeof window !== 'undefined' && (window as any).customElements?.get('model-viewer')) {
        console.log('✅ model-viewer está disponible en el DOM');
        console.log('✅ Web Component registrado correctamente');
        setScriptLoaded(true);
        clearInterval(checkModelViewer);
      }
    }, 100);

    const timeout = setTimeout(() => {
      clearInterval(checkModelViewer);
      if (!scriptLoaded) {
        console.error('⏱️ TIMEOUT: model-viewer no se cargó en 10 segundos');
        console.error('❌ Posible problema con la CDN o el script');
      }
    }, 10000);

    return () => {
      clearInterval(checkModelViewer);
      clearTimeout(timeout);
    };
  }, [scriptLoaded]);

  // 🔍 DEBUG 3: Verificar si la URL del modelo es accesible
  useEffect(() => {
    if (modelUrl) {
      console.log('');
      console.log('═══════════════════════════════════════════════════════');
      console.log('🔍 VERIFICANDO ACCESIBILIDAD DEL MODELO');
      console.log('═══════════════════════════════════════════════════════');
      console.log('🌐 Intentando conectar a:', modelUrl);
      console.log('⏳ Haciendo petición HEAD al servidor...');
      console.log('⚠️ NOTA: Este fetch puede fallar por CORS, pero model-viewer lo cargará igual');
      
      fetch(modelUrl, { method: 'HEAD' })
        .then(response => {
          console.log('');
          console.log('📊 RESPUESTA DEL SERVIDOR:');
          console.log('  - Status Code:', response.status);
          console.log('  - Status Text:', response.statusText);
          console.log('  - OK:', response.ok);
          console.log('  - Redirected:', response.redirected);
          console.log('');
          console.log('📄 HEADERS DE RESPUESTA:');
          console.log('  - Content-Type:', response.headers.get('Content-Type'));
          console.log('  - Content-Length:', response.headers.get('Content-Length'), 'bytes');
          console.log('  - Accept-Ranges:', response.headers.get('Accept-Ranges'));
          console.log('  - Last-Modified:', response.headers.get('Last-Modified'));
          console.log('  - ETag:', response.headers.get('ETag'));
          console.log('  - Access-Control-Allow-Origin:', response.headers.get('Access-Control-Allow-Origin'));
          console.log('');
          
          if (!response.ok) {
            console.error('❌ ERROR: El servidor respondió con código de error:', response.status);
            if (response.status === 404) {
              console.error('❌ 404 NOT FOUND - El archivo NO existe en la ruta especificada');
              console.error('💡 SOLUCIÓN: Verifica que el archivo se subió correctamente al servidor');
            } else if (response.status === 403) {
              console.error('❌ 403 FORBIDDEN - No tienes permisos para acceder al archivo');
              console.error('💡 SOLUCIÓN: Revisa los permisos del archivo en el servidor');
            } else if (response.status === 500) {
              console.error('❌ 500 SERVER ERROR - Error interno del servidor');
              console.error('💡 SOLUCIÓN: Revisa los logs del backend');
            }
          } else {
            console.log('✅ EL MODELO ES ACCESIBLE DESDE EL SERVIDOR');
            
            const contentType = response.headers.get('Content-Type');
            if (contentType) {
              if (contentType.includes('model/gltf-binary') || contentType.includes('application/octet-stream')) {
                console.log('✅ Content-Type correcto para GLB');
              } else if (contentType.includes('model/gltf+json')) {
                console.log('✅ Content-Type correcto para GLTF');
              } else {
                console.warn('⚠️ Content-Type inesperado:', contentType);
                console.warn('💡 Debería ser "model/gltf-binary" o "model/gltf+json"');
              }
            }
            
            const contentLength = response.headers.get('Content-Length');
            if (contentLength) {
              const sizeMB = (parseInt(contentLength) / 1024 / 1024).toFixed(2);
              console.log(`📦 Tamaño del modelo: ${sizeMB} MB`);
              if (parseFloat(sizeMB) > 10) {
                console.warn('⚠️ ADVERTENCIA: Modelo muy grande (>10MB)');
                console.warn('💡 Puede tardar varios segundos en cargar');
              }
            }
          }
          console.log('═══════════════════════════════════════════════════════');
        })
        .catch(error => {
          console.warn('');
          console.warn('═══════════════════════════════════════════════════════');
          console.warn('⚠️ FETCH HEAD FALLÓ (Esto es normal si hay CORS)');
          console.warn('═══════════════════════════════════════════════════════');
          console.warn('Error:', error.message);
          console.warn('');
          console.warn('✅ NO TE PREOCUPES: model-viewer puede cargar el archivo');
          console.warn('   aunque este fetch falle por CORS.');
          console.warn('');
          console.warn('💡 Para eliminar este warning, configura CORS en tu backend:');
          console.warn('   @CrossOrigin(origins = "*") en tu controlador Spring Boot');
          console.warn('═══════════════════════════════════════════════════════');
          // NO establecer error aquí, porque model-viewer puede cargar igual
        });
    }
  }, [modelUrl]);

  // 🔍 DEBUG 4: Monitor de eventos del model-viewer CON LISTENERS NATIVOS
  useEffect(() => {
    if (!scriptLoaded) return;

    console.log('');
    console.log('═══════════════════════════════════════════════════════');
    console.log('👂 CONFIGURANDO LISTENERS NATIVOS DEL MODEL-VIEWER');
    console.log('═══════════════════════════════════════════════════════');
    
    // Esperar a que el elemento esté en el DOM
    const checkAndAttach = () => {
      const modelViewer = document.querySelector('model-viewer');
      
      if (!modelViewer) {
        console.warn('⚠️ model-viewer aún no está en el DOM, reintentando...');
        return false;
      }

      console.log('✅ Elemento <model-viewer> encontrado en el DOM');
      console.log('📋 Atributos del elemento:');
      console.log('  - src:', modelViewer.getAttribute('src'));
      console.log('  - poster:', modelViewer.getAttribute('poster'));
      console.log('  - ar:', modelViewer.getAttribute('ar'));
      
      // ✅ EVENTOS NATIVOS DEL DOM (más confiables que props de React)
      const handleLoad = () => {
        console.log('');
        console.log('🎉🎉🎉 EVENTO NATIVO: load - ¡Modelo cargado!');
        console.log('✅✅✅ Actualizando estado modelLoaded a TRUE');
        setModelLoaded(true);
        setLoadingProgress(100);
      };
      
      const handleError = (e: any) => {
        console.error('');
        console.error('💥 EVENTO NATIVO: error');
        console.error('Detalles:', e);
        setError('Error al cargar el modelo 3D');
      };
      
      const handleProgress = (e: any) => {
        const progress = (e.detail?.totalProgress || 0) * 100;
        setLoadingProgress(progress);
        if (progress % 10 < 1) { // Log cada 10%
          console.log(`📥 Progreso: ${progress.toFixed(1)}%`);
        }
      };
      
      const handleModelVisibility = (e: any) => {
        console.log('👁️👁️👁️ EVENTO NATIVO: model-visibility -', e.detail.visible ? 'VISIBLE' : 'oculto');
        if (e.detail.visible) {
          console.log('✅✅✅ El modelo está VISIBLE, forzando actualización de estado');
          setModelLoaded(true);
        }
      };
      
      // Agregar listeners nativos
      modelViewer.addEventListener('load', handleLoad);
      modelViewer.addEventListener('error', handleError);
      modelViewer.addEventListener('progress', handleProgress);
      modelViewer.addEventListener('model-visibility', handleModelVisibility);
      
      console.log('✅ Event listeners nativos registrados correctamente');
      
      // ⏰ TIMEOUT DE SEGURIDAD: Si después de 15 segundos el modelo no carga, ocultar spinner
      const safetyTimeout = setTimeout(() => {
        console.warn('');
        console.warn('⏰⏰⏰ TIMEOUT: 15 segundos pasaron');
        console.warn('   Ocultando spinner aunque no haya confirmación de carga');
        console.warn('   El modelo probablemente ya está cargado');
        setModelLoaded(true);
      }, 15000);
      
      // Cleanup
      return () => {
        console.log('🧹 Limpiando event listeners');
        modelViewer.removeEventListener('load', handleLoad);
        modelViewer.removeEventListener('error', handleError);
        modelViewer.removeEventListener('progress', handleProgress);
        modelViewer.removeEventListener('model-visibility', handleModelVisibility);
        clearTimeout(safetyTimeout);
      };
    };

    // Intentar inmediatamente
    const cleanup = checkAndAttach();
    if (cleanup) return cleanup;

    // Si no está listo, reintentar cada 100ms hasta 5 segundos
    let attempts = 0;
    const maxAttempts = 50; // 5 segundos
    const interval = setInterval(() => {
      attempts++;
      const cleanup = checkAndAttach();
      if (cleanup) {
        clearInterval(interval);
        console.log('✅ Listeners registrados después de', attempts, 'intentos');
      }
      if (attempts >= maxAttempts) {
        console.error('❌ No se pudo encontrar model-viewer después de 5 segundos');
        clearInterval(interval);
      }
    }, 100);

    return () => {
      clearInterval(interval);
    };
  }, [scriptLoaded]);

  return (
    <>
      {/* Script de model-viewer */}
      <Script
        type="module"
        src="https://cdn.jsdelivr.net/npm/@google/model-viewer@3.4.0/dist/model-viewer.min.js"
        strategy="lazyOnload"
        onReady={() => {
          console.log('✅ Script model-viewer cargado y listo');
        }}
        onError={(e) => {
          console.error('❌ Error fatal cargando script de model-viewer:', e);
          console.error('💡 Verifica tu conexión a internet o la disponibilidad de la CDN');
        }}
      />

      <div className="container mx-auto px-4 py-8 max-w-7xl">
        <div className="mb-6">
          <Link href="/tienda/visualizacion-3d-ar" className="inline-flex items-center text-blue-600 hover:text-blue-700 mb-4">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Volver a galería 3D
          </Link>
          
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900">{producto.nombre}</h1>
          <p className="text-lg text-gray-600 mt-2">{producto.nombreCategoria}</p>
        </div>

        {/* Mostrar error si hay */}
        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <svg className="w-6 h-6 text-red-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <h3 className="font-semibold text-red-900">Error al cargar el modelo 3D</h3>
                <p className="text-sm text-red-700 mt-1">{error}</p>
                <p className="text-xs text-red-600 mt-2">Abre la consola del navegador (F12) para más detalles</p>
              </div>
            </div>
          </div>
        )}

        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          <div className="relative w-full" style={{ height: '70vh', minHeight: '500px' }}>
            {/* Loading overlay */}
            {!modelLoaded && (
              <div className="absolute inset-0 flex items-center justify-center bg-gray-100 z-10">
                <div className="text-center">
                  <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                  <p className="mt-4 text-gray-600">
                    {scriptLoaded ? 'Cargando modelo 3D...' : 'Preparando visor 3D...'}
                  </p>
                  {loadingProgress > 0 && (
                    <div className="mt-2">
                      <div className="w-64 bg-gray-200 rounded-full h-2 mx-auto">
                        <div 
                          className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${loadingProgress}%` }}
                        ></div>
                      </div>
                      <p className="text-xs text-gray-500 mt-1">{loadingProgress.toFixed(0)}%</p>
                    </div>
                  )}
                  <p className="mt-2 text-xs text-gray-400">
                    Esto puede tardar unos segundos
                  </p>
                  <p className="mt-1 text-xs text-gray-400">
                    Abre la consola (F12) para ver logs detallados
                  </p>
                </div>
              </div>
            )}
            
            {/* Renderizar model-viewer solo cuando script esté listo */}
            {scriptLoaded ? (
              <model-viewer
                ref={modelViewerRef}
                src={modelUrl}
                poster={posterUrl}
                alt={`Modelo 3D de ${producto.nombre}`}
                ar={multimedia.habilitadoRa}
                ar-modes="webxr scene-viewer quick-look"
                camera-controls
                auto-rotate
                shadow-intensity="1"
                {...({
                  'environment-image': 'neutral',
                  'exposure': '1',
                  'shadow-softness': '0.5',
                  'camera-orbit': '0deg 75deg 105%',
                  'min-camera-orbit': 'auto auto 10%',
                  'max-camera-orbit': 'auto auto 400%'
                } as any)}
                style={{ width: '100%', height: '100%', backgroundColor: '#f0f0f0' }}
                loading="eager"
                reveal="auto"
              >
                {multimedia.habilitadoRa && (
                  <button
                    slot="ar-button"
                    style={{
                      position: 'absolute',
                      bottom: '16px',
                      right: '16px',
                      backgroundColor: '#2563eb',
                      color: 'white',
                      padding: '12px 24px',
                      borderRadius: '8px',
                      border: 'none',
                      cursor: 'pointer',
                      fontWeight: '600',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                    }}
                  >
                    <svg style={{ width: '24px', height: '24px' }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                    </svg>
                    Ver en tu espacio (AR)
                  </button>
                )}
              </model-viewer>
            ) : (
              <div className="absolute inset-0 flex items-center justify-center bg-gray-50">
                <p className="text-gray-500">Inicializando visor...</p>
              </div>
            )}
          </div>

          {/* Controles e Información */}
          <div className="p-6 bg-gray-50 border-t">
            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <h3 className="font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Controles del visor
                </h3>
                <ul className="space-y-2 text-sm text-gray-600">
                  <li>🖱️ <strong>Arrastrar:</strong> Rotar el modelo</li>
                  <li>🔍 <strong>Scroll:</strong> Hacer zoom</li>
                  <li>📱 <strong>Dos dedos:</strong> Rotar y zoom (móvil)</li>
                  <li>🔄 <strong>Auto-rotate:</strong> Activado automáticamente</li>
                </ul>
              </div>

              <div>
                <h3 className="font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                  </svg>
                  Realidad Aumentada
                </h3>
                {multimedia.habilitadoRa ? (
                  <div className="space-y-2 text-sm text-gray-600">
                    <p className="flex items-center gap-2">
                      <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                      <strong>Disponible</strong>
                    </p>
                    <p>📱 iOS: Safari (iOS 12+)</p>
                    <p>🤖 Android: Chrome (ARCore)</p>
                    <p className="text-xs text-gray-500 mt-2">
                      Toca el botón &quot;Ver en tu espacio&quot; para activar AR
                    </p>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">No disponible para este producto</p>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-8 bg-white rounded-xl shadow-lg p-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Sobre este producto</h2>
          <p className="text-gray-700 mb-4">{producto.descripcion}</p>
          <div className="flex items-center justify-between">
            <span className="text-3xl font-bold text-blue-600">
              Bs. {producto.precioVenta.toFixed(2)}
            </span>
            <Link 
              href={`/tienda/productos/${producto.id}`}
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-semibold"
            >
              Ver detalles completos
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}

// Componente principal con lógica de carga
function Visualizacion3DARContent() {
  const searchParams = useSearchParams();
  const productoId = searchParams.get('id');
  
  const [multimedia, setMultimedia] = useState<MultimediaProducto | null>(null);
  const [producto, setProducto] = useState<Producto | null>(null);
  const [productosConRA, setProductosConRA] = useState<ProductoConMultimedia[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        console.log('');
        console.log('═══════════════════════════════════════════════════════');
        console.log('🚀 INICIANDO CARGA DE DATOS');
        console.log('═══════════════════════════════════════════════════════');
        console.log('Backend URL:', BACKEND_URL);
        console.log('Producto ID:', productoId || 'No especificado (modo galería)');

        // CASO 1: Hay productoId → Cargar producto específico
        if (productoId) {
          console.log('📦 Modo: Carga de producto individual');
          console.log('');
          console.log('🌐 Endpoint producto:', `${BACKEND_URL}/api/productos/${productoId}`);
          console.log('🌐 Endpoint multimedia:', `${BACKEND_URL}/api/multimedia-productos/producto/${productoId}`);
          
          // allSettled y no all: mas abajo la multimedia ya se trata como
          // opcional, pero con Promise.all esa intencion no se cumplia. Si el
          // endpoint de multimedia no respondia a nivel de red, fetch lanzaba y
          // el producto tampoco se mostraba.
          const [rProducto, rMultimedia] = await Promise.allSettled([
            fetch(`${BACKEND_URL}/api/productos/${productoId}`),
            fetch(`${BACKEND_URL}/api/multimedia-productos/producto/${productoId}`)
          ]);

          if (rProducto.status === 'rejected' || !rProducto.value.ok) {
            console.error('Error al cargar producto:',
              rProducto.status === 'rejected' ? rProducto.reason : rProducto.value.status);
            throw new Error('Producto no encontrado');
          }

          const productoData: Producto = await rProducto.value.json();
          setProducto(productoData);

          if (rMultimedia.status === 'fulfilled' && rMultimedia.value.ok) {
            const multimediaData: MultimediaProducto = await rMultimedia.value.json();
            setMultimedia(multimediaData);
          } else {
            // Se muestra la ficha del producto sin el visor 3D.
            console.warn('Sin multimedia para este producto');
            setMultimedia(null);
          }
        } 
        // CASO 2: NO hay productoId → Cargar galería de productos con RA
        else {
          console.log('📦 Modo: Galería de productos 3D');
          console.log('');
          console.log('🌐 Endpoint productos:', `${BACKEND_URL}/api/productos/activos`);
          console.log('🌐 Endpoint multimedia RA:', `${BACKEND_URL}/api/multimedia-productos/ra-habilitados`);
          
          const [productosRes, multimediaListRes] = await Promise.all([
            fetch(`${BACKEND_URL}/api/productos/activos`),
            fetch(`${BACKEND_URL}/api/multimedia-productos/ra-habilitados`)
          ]);

          console.log('📊 Respuesta productos:', productosRes.status, productosRes.statusText);
          console.log('📊 Respuesta multimedia list:', multimediaListRes.status, multimediaListRes.statusText);

          if (productosRes.ok && multimediaListRes.ok) {
            const productos: Producto[] = await productosRes.json();
            const multimediaList: MultimediaProducto[] = await multimediaListRes.json();

            console.log('✅ Total productos activos:', productos.length);
            console.log('✅ Total productos con 3D:', multimediaList.length);

            // Combinar productos con su multimedia
            const productosCon3D: ProductoConMultimedia[] = multimediaList
              .map(m => {
                const prod = productos.find(p => p.id === m.productoId);
                if (prod) {
                  return { ...prod, multimedia: m };
                }
                return null;
              })
              .filter((p): p is ProductoConMultimedia => p !== null);

            setProductosConRA(productosCon3D);
          } else {
            // Acá se usa Promise.all a propósito: la galería combina las dos
            // listas, así que sin una de ellas no hay nada que mostrar. Un
            // fallo de red cae en el catch, pero un 500 dejaba la galería
            // vacía y en silencio, sin distinguirse de "no hay productos 3D".
            throw new Error('No se pudo cargar la galería de productos 3D');
          }
        }

        console.log('═══════════════════════════════════════════════════════');
        console.log('✅ CARGA DE DATOS COMPLETADA');
        console.log('═══════════════════════════════════════════════════════');

      } catch (err) {
        console.error('');
        console.error('═══════════════════════════════════════════════════════');
        console.error('❌ ERROR AL CARGAR DATOS');
        console.error('═══════════════════════════════════════════════════════');
        console.error('Error:', err);
        console.error('═══════════════════════════════════════════════════════');
        setError((err as Error).message || 'Error al cargar los datos');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [productoId, BACKEND_URL]);

  // Loading
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
          <div className="mx-auto bg-blue-100 rounded-full w-16 h-16 flex items-center justify-center animate-pulse">
            <svg className="h-8 w-8 text-blue-600 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900">Cargando...</h2>
          <p className="text-gray-600">Por favor, espera un momento</p>
        </div>
      </div>
    );
  }

  // Error
  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
          <div className="mx-auto bg-red-100 rounded-full w-16 h-16 flex items-center justify-center">
            <svg className="h-8 w-8 text-red-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-1.964-1.333-2.732 0L3.732 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900">Error</h2>
          <p className="text-gray-600">{error}</p>
          <Link href="/tienda/productos" className="inline-block mt-4 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
            Volver al catálogo
          </Link>
        </div>
      </div>
    );
  }

  // CASO 1: Ver producto específico
  if (productoId && producto) {
    // Producto sin modelo 3D
    if (!multimedia || !multimedia.urlModelo3d) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-16rem)] py-12 px-4">
          <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8 space-y-6 text-center">
            <div className="mx-auto bg-yellow-100 rounded-full w-16 h-16 flex items-center justify-center">
              <svg className="h-8 w-8 text-yellow-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900">Modelo 3D no disponible</h2>
            <p className="text-gray-600">
              El producto <strong>{producto.nombre}</strong> aún no tiene un modelo 3D cargado.
            </p>
            <div className="space-y-3">
              <Link href={`/tienda/productos/${productoId}`} className="block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
                Ver detalles del producto
              </Link>
              <Link href="/tienda/visualizacion-3d-ar" className="block px-6 py-3 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition">
                Ver otros productos en 3D
              </Link>
            </div>
          </div>
        </div>
      );
    }

    // Mostrar visor
    return <VisorProducto producto={producto} multimedia={multimedia} />;
  }

  // CASO 2: Mostrar galería de productos con 3D
  return <GaleriaProductos3D productos={productosConRA} />;
}

export default function Visualizacion3DARPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    }>
      <Visualizacion3DARContent />
    </Suspense>
  );
}
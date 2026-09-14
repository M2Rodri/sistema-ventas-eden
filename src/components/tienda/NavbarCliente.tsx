// src/components/tienda/NavbarCliente.tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

const NavbarCliente = () => {
  const { user, loading } = useAuth();
  const pathname = usePathname(); // 🔥 Hook para detectar la ruta actual

  // Función helper para determinar si un link está activo
  const isActive = (path: string) => {
    if (path === '/tienda') {
      // Para la página de inicio, solo activar si es exactamente "/tienda"
      return pathname === '/tienda';
    }
    // Para otras rutas, verificar si la ruta actual comienza con el path
    return pathname?.startsWith(path);
  };

  // Caso 1: Mientras se carga la información de autenticación
  if (loading) {
    return (
      <header className="fixed top-0 left-0 right-0 bg-gray-800 text-white p-4 z-50 shadow-lg">
        <div className="container mx-auto flex justify-between items-center">
          {/* Placeholder para logo y nombre de la tienda */}
          <div className="flex items-center space-x-2">
            <div className="bg-gray-200 border-2 border-dashed rounded-xl w-16 h-16" />
            <span className="text-xl font-bold">Mueblería Edén</span>
          </div>
          {/* Placeholder para navegación principal (desktop) */}
          <div className="hidden md:flex space-x-6">
            <div className="bg-gray-200 border-2 border-dashed rounded-xl w-16 h-6" />
            <div className="bg-gray-200 border-2 border-dashed rounded-xl w-16 h-6" />
            <div className="bg-gray-200 border-2 border-dashed rounded-xl w-16 h-6" />
          </div>
          
        </div>
      </header>
    );
  }

  // Caso 2: Información de autenticación cargada (loading = false)
  return (
    <header className="fixed top-0 left-0 right-0 bg-gray-800 text-white p-4 z-50 shadow-lg">
      <div className="container mx-auto flex justify-between items-center">
        {/* Logo y Nombre de la Tienda */}
        <div className="flex items-center space-x-2">
          <div className="bg-gray-200 border-2 border-dashed rounded-xl w-16 h-16" />
          <Link href="/tienda">
            <span className="text-xl font-bold hover:text-primary-300 transition-colors">
              Mueblería Edén
            </span>
          </Link>
        </div>

        {/* Navegación Principal (Visible en pantallas medianas y grandes) */}
        <nav className="hidden md:flex space-x-6">
          <Link 
            href="/tienda" 
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda') 
                ? 'text-primary-300 font-semibold bg-gray-700' 
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            Inicio
            {isActive('/tienda') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>

          <Link 
            href="/tienda/productos" 
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda/productos') 
                ? 'text-primary-300 font-semibold bg-gray-700' 
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            Catálogo
            {isActive('/tienda/productos') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>

          <Link 
            href="/tienda/promociones" 
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda/promociones') 
                ? 'text-primary-300 font-semibold bg-gray-700' 
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            Promociones
            {isActive('/tienda/promociones') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>

          <Link 
            href="/tienda/visualizacion-3d-ar" 
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda/visualizacion-3d-ar') 
                ? 'text-primary-300 font-semibold bg-gray-700' 
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            3D/AR
            {isActive('/tienda/visualizacion-3d-ar') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>

          <Link
            href="/tienda/nosotros"
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda/nosotros')
                ? 'text-primary-300 font-semibold bg-gray-700'
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            Nosotros
            {isActive('/tienda/nosotros') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>

          <Link
            href="/tienda/contacto"
            className={`
              relative px-3 py-2 rounded-lg transition-all duration-300
              ${isActive('/tienda/contacto')
                ? 'text-primary-300 font-semibold bg-gray-700'
                : 'hover:text-primary-300 hover:bg-gray-700/50'
              }
            `}
          >
            Contacto
            {isActive('/tienda/contacto') && (
              <span className="absolute bottom-0 left-0 right-0 h-1 bg-primary-400 rounded-full" />
            )}
          </Link>
        </nav>

        {/*
          La tienda es una vitrina pública: no tiene cuentas de cliente, así que
          no ofrece iniciar sesión ni "Mi Perfil". Si quien navega es personal
          del negocio (con sesión abierta en el sistema), se le muestra un
          acceso directo al panel; a cualquier otro visitante, nada.
        */}
        <div className="flex items-center space-x-4">
          {user && (
            <Link
              href="/dashboard"
              className="text-sm bg-primary-600 hover:bg-primary-700 text-white px-3 py-1 rounded transition-colors duration-300"
            >
              Ir al panel
            </Link>
          )}
        </div>
      </div>
    </header>
  );
};

export default NavbarCliente;
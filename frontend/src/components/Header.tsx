'use client';

import { LogOut, Store } from 'lucide-react';
import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import BuscadorGlobal from '@/components/BuscadorGlobal';
import NotificacionesMenu from '@/components/NotificacionesMenu';

export default function Header() {
  const { user, logout } = useAuth();

  return (
    <header className="bg-white border-b border-gray-200 h-16 flex items-center justify-between px-6 shadow-sm">
      {/* Buscador */}
      <BuscadorGlobal />

      {/* Usuario y acciones */}
      <div className="flex items-center gap-4">
        {/*
          La tienda publica queda fuera del alcance de este trabajo: no debe
          ser alcanzable desde el panel de administracion. Se comenta el
          acceso sin borrar el codigo, mismo patron que Envios/Promociones
          en Sidebar.tsx.

          <Link
            href="/tienda"
            target="_blank"
            rel="noopener noreferrer"
            title="Ver la tienda online (se abre en otra pestana)"
            className="flex items-center gap-2 rounded-lg px-3 py-2 text-gray-600 transition-colors hover:bg-primary-50 hover:text-primary-700"
          >
            <Store size={20} />
            <span className="hidden text-sm font-medium lg:inline">Ver tienda</span>
          </Link>
        */}

        {/* Notificaciones */}
        <NotificacionesMenu />

        {/* Perfil */}
        <div className="flex items-center gap-3 pl-3 border-l border-gray-200">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white font-semibold shadow-md">
            {user?.nombre.charAt(0).toUpperCase()}
          </div>
          <div className="text-sm">
            <p className="font-semibold text-gray-900">
              {user?.nombre} {user?.apellido}
            </p>
            <p className="text-xs text-gray-500 capitalize">{user?.role.toLowerCase()}</p>
          </div>
        </div>

        {/* Logout */}
        <button
          onClick={logout}
          className="p-2 hover:bg-red-50 rounded-lg transition-colors group"
          title="Cerrar sesión"
        >
          <LogOut size={20} className="text-gray-600 group-hover:text-red-600" />
        </button>
      </div>
    </header>
  );
}
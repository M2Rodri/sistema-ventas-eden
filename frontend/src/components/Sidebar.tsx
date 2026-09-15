'use client';

import { FolderOpen } from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import {
  LayoutDashboard,
  Users,
  Package,
  Boxes,
  ShoppingCart,
  DollarSign,
  Truck,
  CreditCard,
  BarChart3,
  Tag,
  Settings,
  Building2,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

interface MenuItem {
  name: string;
  icon: React.ReactNode;
  href: string;
  adminOnly?: boolean;
}

const menuItems: MenuItem[] = [
  { name: 'Inicio', icon: <LayoutDashboard size={20} />, href: '/dashboard' },
  { name: 'Usuarios', icon: <Users size={20} />, href: '/dashboard/usuarios', adminOnly: true },
  { name: 'Categorías', icon: <FolderOpen size={20} />, href: '/dashboard/categorias' },
  { name: 'Productos', icon: <Package size={20} />, href: '/dashboard/productos' },
  { name: 'Inventario', icon: <Boxes size={20} />, href: '/dashboard/inventario' },
  { name: 'Ventas', icon: <DollarSign size={20} />, href: '/dashboard/ventas' },
  { name: 'Proveedores', icon: <Building2 size={20} />, href: '/dashboard/proveedores', adminOnly: true },
  { name: 'Envíos', icon: <Truck size={20} />, href: '/dashboard/envios' },
  // { name: 'Pagos', icon: <CreditCard size={20} />, href: '/dashboard/pagos' },
  { name: 'Reportes', icon: <BarChart3 size={20} />, href: '/dashboard/reportes', adminOnly: true },
  { name: 'Promociones', icon: <Tag size={20} />, href: '/dashboard/promociones' },
  { name: 'Configuración', icon: <Settings size={20} />, href: '/dashboard/configuracion', adminOnly: true },
];

interface SidebarProps {
  isAdmin: boolean;
}

export default function Sidebar({ isAdmin }: SidebarProps) {
  const pathname = usePathname();
  const [isCollapsed, setIsCollapsed] = useState(false);

  const filteredMenu = menuItems.filter(item => 
    !item.adminOnly || (item.adminOnly && isAdmin)
  );

  return (
    <aside className={`${isCollapsed ? 'w-20' : 'w-64'} bg-gradient-to-b from-gray-900 via-gray-800 to-gray-900 h-screen border-r border-gray-700 transition-all duration-300 ease-in-out relative flex flex-col`}>
      {/* Header */}
      <div className={`p-6 border-b border-gray-700 ${isCollapsed ? 'px-3' : ''}`}>
        {!isCollapsed ? (
          <>
            <h1 className="text-xl font-bold text-white flex items-center gap-2">
              🛏️ Mueblería Edén
            </h1>
            <p className="text-xs text-gray-400 mt-1">Sistema de Administración</p>
          </>
        ) : (
          <div className="text-2xl text-center">🛏️</div>
        )}
      </div>

      {/* Toggle Button */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="absolute -right-3 top-20 bg-primary-600 hover:bg-primary-700 text-white p-1.5 rounded-full shadow-lg transition-all duration-200 z-10"
      >
        {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
      </button>
      
      {/* Menu */}
      
      <nav className="px-3 py-4 space-y-1 overflow-y-auto h-[calc(100vh-120px)]">
        {filteredMenu.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              title={isCollapsed ? item.name : ''}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all group ${
                isActive
                  ? 'bg-gradient-to-r from-primary-600 to-primary-700 text-white font-semibold shadow-lg scale-105'
                  : 'text-gray-300 hover:bg-gray-700/50 hover:text-white'
              } ${isCollapsed ? 'justify-center' : ''}`}
            >
              <span className={`${isActive ? 'text-white' : 'text-gray-400 group-hover:text-white'} transition-colors`}>
                {item.icon}
              </span>
              {!isCollapsed && <span className="truncate">{item.name}</span>}
              
              {/* Indicador activo cuando está colapsado */}
              {isCollapsed && isActive && (
                <span className="absolute left-0 w-1 h-8 bg-primary-400 rounded-r-full"></span>
              )}
            </Link>
          );
        })}
      </nav>

            {/* Footer cuando está expandido */}
      {/* {!isCollapsed && (
        <div className="absolute bottom-4 left-4 right-4 text-center text-xs text-gray-500">
          v1.0.0
        </div>
      )} */}
    </aside>
  );
}
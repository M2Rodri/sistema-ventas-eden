'use client';

import { useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { precargarDatos } from '@/lib/api';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';
import SesionExpiradaWatcher from '@/components/SesionExpiradaWatcher';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { loading, isAdmin, user } = useAuth();

  // Una vez dentro, deja listos los datos de los módulos mientras el usuario
  // mira el inicio. Espera un momento para no competir con las peticiones del
  // propio inicio.
  const haySesion = !loading && !!user;
  useEffect(() => {
    if (!haySesion) return;
    const espera = setTimeout(() => { void precargarDatos(); }, 1500);
    return () => clearTimeout(espera);
  }, [haySesion]);

  // Mostrar loading solo si realmente está cargando
  if (loading || !user) {
    return null; // No mostrar nada mientras redirige
  }

  return (
    <div className="flex h-screen overflow-hidden bg-primary-50">
      <SesionExpiradaWatcher />
      <Sidebar isAdmin={isAdmin()} />
      
      <div className="flex-1 flex flex-col overflow-hidden">
        <Header />
        
        <main className="flex-1 overflow-y-auto bg-gray-50 p-6">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
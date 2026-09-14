'use client';

import { useAuth } from '@/hooks/useAuth';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';
import SesionExpiradaWatcher from '@/components/SesionExpiradaWatcher';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { loading, isAdmin, user } = useAuth();

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
        
        <main className="flex-1 overflow-y-auto bg-gradient-to-br from-primary-50 via-white to-secondary-50 p-6">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
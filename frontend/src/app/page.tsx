'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { obtenerUsuarioDeCookie } from '@/lib/sesion';

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    const user = obtenerUsuarioDeCookie();

    if (user) {
      // Si es admin/empleado → Dashboard
      if (user.role === 'ADMIN' || user.role === 'EMPLEADO') {
        router.replace('/dashboard');
      } else {
        router.replace('/login');
      }
    } else {
      router.replace('/login');
    }
  }, [router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>
  );
}
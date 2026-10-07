'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    const token = Cookies.get('token');
    const userData = Cookies.get('user');
    
    if (token && userData) {
      const user = JSON.parse(userData);
      
      // Si es admin/empleado → Dashboard
      if (user.role === 'ADMIN' || user.role === 'EMPLEADO') {
        router.replace('/dashboard');
      } 
      // Cualquier otro rol no tiene panel: al login
      else {
        router.replace('/login');
      }
    } else {
      // Sin login → Tienda pública
      // router.replace('/tienda');
      // Sin login → Login
      router.replace('/login');
    }
  }, [router]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>
  );
}
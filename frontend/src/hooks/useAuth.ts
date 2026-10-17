'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { cerrarSesion, obtenerUsuarioDeCookie, DatosUsuario } from '@/lib/sesion';

export const useAuth = () => {
  const router = useRouter();
  const [user, setUser] = useState<DatosUsuario | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = () => {
    try {
      const userData = obtenerUsuarioDeCookie();

      if (userData) {
        setUser(userData);
        setLoading(false);
      } else {
        router.replace('/login');
      }
    } catch (error) {
      console.error('Error al verificar autenticación:', error);
      void cerrarSesion();
      router.replace('/login');
    }
  };

  const logout = async () => {
    await cerrarSesion();
    setUser(null);
    router.replace('/login');
  };

  const isAdmin = () => {
    return user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN';
  };

  return {
    user,
    loading,
    logout,
    isAdmin,
    isAuthenticated: !!user,
  };
};
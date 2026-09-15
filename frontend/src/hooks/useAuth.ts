'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';
import { AuthResponse } from '@/lib/api';

export const useAuth = () => {
  const router = useRouter();
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = () => {
    try {
      const token = Cookies.get('token');
      const userData = Cookies.get('user');

      if (token && userData) {
        setUser(JSON.parse(userData));
        setLoading(false);
      } else {
        router.replace('/login');
      }
    } catch (error) {
      console.error('Error al verificar autenticación:', error);
      Cookies.remove('token');
      Cookies.remove('user');
      Cookies.remove('role');
      router.replace('/login');
    }
  };

  const logout = () => {
    Cookies.remove('token');
    Cookies.remove('user');
    Cookies.remove('role');
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
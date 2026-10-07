'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import Cookies from 'js-cookie';
import { guardarSesion, borrarSesion } from '@/lib/sesion';

export type UserRole = 'ADMIN' | 'EMPLEADO' | 'CLIENTE';

export interface User {
  id: number;
  username: string;
  nombreCompleto: string;
  usuario: string;
  role: UserRole;
  activo: boolean;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, userData: User) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isEmpleado: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  // Cargar la sesión desde las cookies al montar (no se guarda nada en localStorage)
  useEffect(() => {
    const storedToken = Cookies.get('token');
    const storedUser = Cookies.get('user');

    if (storedToken && storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        setToken(storedToken);
        setUser(userData);
      } catch (error) {
        console.error('Error al parsear datos de usuario:', error);
        borrarSesion();
      }
    }
  }, []);

  const login = (newToken: string, userData: User) => {
    guardarSesion(newToken, userData, userData.role);
    setToken(newToken);
    setUser(userData);
  };

  const logout = () => {
    borrarSesion();
    setToken(null);
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    token,
    login,
    logout,
    isAuthenticated: !!token && !!user,
    isAdmin: user?.role === 'ADMIN',
    isEmpleado: user?.role === 'EMPLEADO',
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth debe ser usado dentro de un AuthProvider');
  }
  return context;
}
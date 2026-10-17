'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { cerrarSesion, obtenerUsuarioDeCookie, DatosUsuario } from '@/lib/sesion';

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
  login: (userData: User) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isEmpleado: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);

  // Cargar la sesión desde la cookie user_info al montar
  useEffect(() => {
    const datos = obtenerUsuarioDeCookie();
    if (datos) {
      setUser({
        id: datos.id,
        username: datos.usuario,
        nombreCompleto: `${datos.nombre} ${datos.apellido}`.trim(),
        usuario: datos.usuario,
        role: datos.role as UserRole,
        activo: true,
      });
    }
  }, []);

  const login = (userData: User) => {
    setUser(userData);
  };

  const logout = () => {
    void cerrarSesion();
    setUser(null);
  };

  const value: AuthContextType = {
    user,
    token: null, // El token es HttpOnly y ya no se expone a JavaScript
    login,
    logout,
    isAuthenticated: !!user,
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
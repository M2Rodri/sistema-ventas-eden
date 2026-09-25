"use client";
import Cookies from "js-cookie";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { login } from "@/lib/api";
import { mensajeError } from "@/lib/errores";

export default function LoginPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    usuario: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [sesionExpirada, setSesionExpirada] = useState(false);
  const [loading, setLoading] = useState(false);

  // Estados para el modal de recuperar contraseña
  const [showRecoverModal, setShowRecoverModal] = useState(false);
  const [mounted, setMounted] = useState(false);

  // Asegurar que el componente está montado en el cliente
  useEffect(() => {
    setMounted(true);
    // El motivo lo deja SesionExpiradaWatcher cuando el backend rechaza el
    // token por vencido. Va en sessionStorage y no en la URL porque useAuth
    // también redirige acá y pisaría cualquier parámetro.
    if (typeof window !== "undefined") {
      if (sessionStorage.getItem("motivoSalida") === "sesion-expirada") {
        setSesionExpirada(true);
        sessionStorage.removeItem("motivoSalida");
      }
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await login(formData);

      console.log("🔐 Login response:", response);

      Cookies.set("token", response.token, {
        expires: 7,
        path: "/",
        secure: process.env.NODE_ENV === "production",
      });

      Cookies.set("user", JSON.stringify(response), {
        expires: 7,
        path: "/",
        secure: process.env.NODE_ENV === "production",
      });

      const rawRole = response.role || "";
      const role = rawRole.replace("ROLE_", "").toUpperCase();

      Cookies.set("role", role, {
        expires: 7,
        path: "/",
        secure: process.env.NODE_ENV === "production",
      });

      console.log("🎭 Rol normalizado:", role);

      if (role === "ADMIN" || role === "EMPLEADO") {
        window.location.href = "/dashboard";
      } else {
        window.location.href = "/tienda";
      }
    } catch (err: any) {
      console.error("❌ Error en login:", err);
      setError(mensajeError(err, "Error al iniciar sesión."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-primary-100 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-lg">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            🛏️ Mueblería Edén
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            Inicia sesión en tu cuenta
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {sesionExpirada && !error && (
            <div className="rounded-md bg-amber-50 border border-amber-200 p-4">
              <p className="text-sm text-amber-800">
                Tu sesión expiró por seguridad. Volvé a iniciar sesión para continuar.
              </p>
            </div>
          )}

          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label
                htmlFor="usuario"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Usuario
              </label>
              <input
                id="usuario"
                name="usuario"
                type="text"
                required
                className="appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent sm:text-sm"
                placeholder="Tu usuario"
                value={formData.usuario}
                onChange={(e) =>
                  setFormData({ ...formData, usuario: e.target.value })
                }
              />
            </div>
            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Contraseña
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent sm:text-sm"
                placeholder="••••••••"
                value={formData.password}
                onChange={(e) =>
                  setFormData({ ...formData, password: e.target.value })
                }
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-lg text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 transition-colors"
            >
              {loading ? "Iniciando sesión..." : "Iniciar Sesión"}
            </button>
          </div>

          <div className="flex flex-col space-y-2 text-center">
            {/*
              Antes había un "Recuperar Contraseña" que simulaba el envío de un
              correo: mostraba un mensaje de éxito y no hacía nada. El sistema
              no tiene servicio de correo, y las contraseñas las restablece el
              administrador desde la gestión de usuarios.
            */}
            {mounted && showRecoverModal && (
              <p className="text-sm text-gray-600 bg-gray-50 border border-gray-200 rounded-lg p-3">
                Contactá a la persona encargada del sistema para recuperar tu acceso.
              </p>
            )}
            {mounted && !showRecoverModal && (
              <button
                type="button"
                onClick={() => {
                  setShowRecoverModal(true);
                  // Vuelve solo al estado normal: no hace falta que el
                  // usuario cierre el aviso a mano.
                  setTimeout(() => setShowRecoverModal(false), 7000);
                }}
                className="font-medium text-primary-600 hover:text-primary-500 text-sm"
              >
                ¿Olvidaste tu contraseña?
              </button>
            )}
          </div>
        </form>
      </div>

    </div>
  );
}

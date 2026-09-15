'use client';

import React, { useState, useEffect } from 'react';
import { enviarMensajeContacto, getDatosNegocio, DatosNegocio } from '@/lib/api';
import { MapPin, Phone, Mail, Clock, Send, MessageCircle, Facebook, Instagram, CheckCircle, Loader } from 'lucide-react';

export default function ContactoPage() {
  const [formData, setFormData] = useState({
    nombre: '',
    email: '',
    telefono: '',
    asunto: '',
    mensaje: ''
  });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [negocio, setNegocio] = useState<DatosNegocio>({});

  // Los datos de contacto salen de configuracion_sistema, no del código: así
  // se corrigen desde la pantalla de Configuración sin recompilar.
  useEffect(() => {
    let activo = true;
    getDatosNegocio()
      .then((d) => activo && setNegocio(d))
      .catch(() => {})
      .finally(() => {});
    return () => { activo = false; };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    // Antes esto era un setTimeout que mostraba "enviado con éxito" sin
    // llamar a ningún lado: el cliente creía haber escrito y nadie recibía
    // nada. Ahora la consulta se guarda y el negocio la ve en el sistema.
    try {
      await enviarMensajeContacto(formData);
      setSuccess(true);
      setFormData({ nombre: '', email: '', telefono: '', asunto: '', mensaje: '' });
      setTimeout(() => setSuccess(false), 6000);
    } catch (err: any) {
      setError(err?.message ?? 'No se pudo enviar el mensaje. Intentá de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const contactInfo = {
    direccion: negocio.negocio_direccion || 'Santa Cruz de la Sierra, Bolivia',
    telefono: negocio.negocio_telefono || '',
    whatsapp: negocio.negocio_whatsapp || '',
    email: negocio.negocio_email || '',
    horarios: negocio.negocio_horario
      ? negocio.negocio_horario.split('·').map((tramo) => {
          const [dia, horario] = tramo.split(' de ');
          return { dia: (dia || '').trim(), horario: (horario || '').trim() };
        })
      : [],
    redes: {
      facebook: 'https://facebook.com/muebleriaeden',
      instagram: 'https://instagram.com/muebleriaeden',
    },
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-secondary-50">
      {/* Hero Section */}
      <div className="relative bg-gradient-to-r from-primary-600 via-primary-700 to-primary-800 text-white py-20 px-4 sm:px-6 lg:px-8 overflow-hidden">
        <div className="absolute inset-0 bg-black opacity-5"></div>
        <div className="absolute inset-0">
          <div className="absolute top-10 left-10 w-40 h-40 bg-yellow-300 opacity-20 rounded-full blur-3xl animate-pulse"></div>
          <div className="absolute bottom-10 right-10 w-56 h-56 bg-orange-400 opacity-20 rounded-full blur-3xl animate-pulse"></div>
        </div>
        
        <div className="max-w-7xl mx-auto relative z-10 text-center">
          <div className="inline-flex items-center gap-3 bg-white/20 backdrop-blur-lg px-6 py-3 rounded-full mb-6 border border-white/30 shadow-xl">
            <MessageCircle className="w-6 h-6 text-yellow-300 animate-pulse" />
            <span className="text-base font-bold tracking-wide">Estamos para ayudarte</span>
          </div>
          
          <h1 className="text-5xl md:text-6xl lg:text-7xl font-black mb-6 leading-tight">
            Contáctanos
          </h1>
          
          <p className="text-xl md:text-2xl lg:text-3xl text-primary-100 max-w-4xl mx-auto font-medium leading-relaxed">
            ¿Tienes alguna pregunta? Nos encantaría escucharte
          </p>
        </div>

        {/* Decoración de ondas */}
        <div className="absolute bottom-0 left-0 right-0">
          <svg viewBox="0 0 1440 120" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full">
            <path d="M0,64L80,69.3C160,75,320,85,480,80C640,75,800,53,960,48C1120,43,1280,53,1360,58.7L1440,64L1440,120L1360,120C1280,120,1120,120,960,120C800,120,640,120,480,120C320,120,160,120,80,120L0,120Z" fill="rgb(249, 250, 251)" fillOpacity="1"></path>
          </svg>
        </div>
      </div>

      {/* Contenido Principal */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 -mt-8 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Información de Contacto */}
          <div className="space-y-8">
            <div className="bg-white rounded-3xl shadow-2xl p-8 border-2 border-primary-100">
              <h2 className="text-3xl font-black text-gray-900 mb-8 flex items-center gap-3">
                <div className="bg-primary-100 p-3 rounded-2xl">
                  <MapPin className="w-8 h-8 text-primary-600" />
                </div>
                Información de Contacto
              </h2>

              <div className="space-y-6">
                {/* Dirección */}
                <div className="flex items-start gap-4 p-4 bg-gradient-to-r from-primary-50 to-white rounded-2xl border border-primary-100 hover:border-primary-300 transition-all group">
                  <div className="bg-primary-100 p-3 rounded-xl group-hover:scale-110 transition-transform">
                    <MapPin className="w-6 h-6 text-primary-600" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 mb-1">Dirección</h3>
                    <p className="text-gray-600 leading-relaxed">{contactInfo.direccion}</p>
                  </div>
                </div>

                {/* Teléfono */}
                <div className="flex items-start gap-4 p-4 bg-gradient-to-r from-green-50 to-white rounded-2xl border border-green-100 hover:border-green-300 transition-all group">
                  <div className="bg-green-100 p-3 rounded-xl group-hover:scale-110 transition-transform">
                    <Phone className="w-6 h-6 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 mb-1">Teléfono</h3>
                    <a href={`tel:${contactInfo.telefono}`} className="text-green-600 hover:text-green-700 font-semibold">
                      {contactInfo.telefono}
                    </a>
                  </div>
                </div>

                {/* WhatsApp */}
                <div className="flex items-start gap-4 p-4 bg-gradient-to-r from-emerald-50 to-white rounded-2xl border border-emerald-100 hover:border-emerald-300 transition-all group">
                  <div className="bg-emerald-100 p-3 rounded-xl group-hover:scale-110 transition-transform">
                    <MessageCircle className="w-6 h-6 text-emerald-600" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 mb-1">WhatsApp</h3>
                    <a 
                      href={`https://wa.me/${contactInfo.whatsapp.replace(/[^0-9]/g, '')}`} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-emerald-600 hover:text-emerald-700 font-semibold"
                    >
                      {contactInfo.whatsapp}
                    </a>
                  </div>
                </div>

                {/* Email */}
                <div className="flex items-start gap-4 p-4 bg-gradient-to-r from-blue-50 to-white rounded-2xl border border-blue-100 hover:border-blue-300 transition-all group">
                  <div className="bg-blue-100 p-3 rounded-xl group-hover:scale-110 transition-transform">
                    <Mail className="w-6 h-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900 mb-1">Email</h3>
                    <a href={`mailto:${contactInfo.email}`} className="text-blue-600 hover:text-blue-700 font-semibold">
                      {contactInfo.email}
                    </a>
                  </div>
                </div>

                {/* Horarios */}
                <div className="p-6 bg-gradient-to-r from-purple-50 to-white rounded-2xl border border-purple-100">
                  <div className="flex items-center gap-3 mb-4">
                    <div className="bg-purple-100 p-2 rounded-xl">
                      <Clock className="w-6 h-6 text-purple-600" />
                    </div>
                    <h3 className="font-bold text-gray-900 text-lg">Horarios de Atención</h3>
                  </div>
                  <div className="space-y-2">
                    {contactInfo.horarios.map((horario, idx) => (
                      <div key={idx} className="flex justify-between items-center py-2 border-b border-purple-100 last:border-0">
                        <span className="text-gray-700 font-semibold">{horario.dia}</span>
                        <span className="text-purple-600 font-bold">{horario.horario}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Redes Sociales */}
                <div className="flex gap-4 justify-center pt-4">
                  <a 
                    href={contactInfo.redes.facebook} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="bg-blue-600 hover:bg-blue-700 text-white p-4 rounded-2xl transition-all transform hover:scale-110 shadow-lg hover:shadow-xl"
                  >
                    <Facebook className="w-6 h-6" />
                  </a>
                  <a 
                    href={contactInfo.redes.instagram} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white p-4 rounded-2xl transition-all transform hover:scale-110 shadow-lg hover:shadow-xl"
                  >
                    <Instagram className="w-6 h-6" />
                  </a>
                </div>
              </div>
            </div>
          </div>

          {/* Formulario de Contacto */}
          <div className="bg-white rounded-3xl shadow-2xl p-8 border-2 border-primary-100">
            <h2 className="text-3xl font-black text-gray-900 mb-8 flex items-center gap-3">
              <div className="bg-primary-100 p-3 rounded-2xl">
                <Send className="w-8 h-8 text-primary-600" />
              </div>
              Envíanos un Mensaje
            </h2>

            {success && (
              <div className="mb-6 p-4 bg-green-50 border-2 border-green-200 rounded-2xl flex items-center gap-3 animate-bounce-in">
                <CheckCircle className="w-6 h-6 text-green-600" />
                <p className="text-green-700 font-semibold">¡Mensaje enviado exitosamente! Te responderemos pronto.</p>
              </div>
            )}

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">Nombre Completo *</label>
                <input
                  type="text"
                  name="nombre"
                  value={formData.nombre}
                  onChange={handleChange}
                  required
                  className="w-full px-5 py-4 border-2 border-gray-200 rounded-2xl focus:border-primary-500 focus:ring-4 focus:ring-primary-100 outline-none transition-all font-medium"
                  placeholder="Ej: Juan Pérez"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">Email *</label>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    className="w-full px-5 py-4 border-2 border-gray-200 rounded-2xl focus:border-primary-500 focus:ring-4 focus:ring-primary-100 outline-none transition-all font-medium"
                    placeholder="tu@email.com"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">Teléfono</label>
                  <input
                    type="tel"
                    name="telefono"
                    value={formData.telefono}
                    onChange={handleChange}
                    className="w-full px-5 py-4 border-2 border-gray-200 rounded-2xl focus:border-primary-500 focus:ring-4 focus:ring-primary-100 outline-none transition-all font-medium"
                    placeholder="7 123-4567"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">Asunto *</label>
                <select
                  name="asunto"
                  value={formData.asunto}
                  onChange={handleChange}
                  required
                  className="w-full px-5 py-4 border-2 border-gray-200 rounded-2xl focus:border-primary-500 focus:ring-4 focus:ring-primary-100 outline-none transition-all font-medium bg-white"
                >
                  <option value="">Selecciona un asunto</option>
                  <option value="consulta">Consulta General</option>
                  <option value="producto">Información de Producto</option>
                  <option value="garantia">Garantías y Devoluciones</option>
                  <option value="otro">Otro</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">Mensaje *</label>
                <textarea
                  name="mensaje"
                  value={formData.mensaje}
                  onChange={handleChange}
                  required
                  rows={6}
                  className="w-full px-5 py-4 border-2 border-gray-200 rounded-2xl focus:border-primary-500 focus:ring-4 focus:ring-primary-100 outline-none transition-all resize-none font-medium"
                  placeholder="Escribe tu mensaje aquí..."
                />
              </div>

              <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-gradient-to-r from-primary-600 to-primary-700 hover:from-primary-700 hover:to-primary-800 text-white font-black py-5 px-8 rounded-2xl transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed text-lg"
              >
                {loading ? (
                  <>
                    <Loader className="w-6 h-6 animate-spin" />
                    <span>Enviando...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-6 h-6" />
                    <span>Enviar Mensaje</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Mapa */}
        <div className="mt-16">
          <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border-2 border-primary-100">
            <div className="p-6 bg-gradient-to-r from-primary-600 to-primary-700 text-white">
              <h2 className="text-2xl font-black flex items-center gap-3">
                <MapPin className="w-8 h-8" />
                Encuéntranos Aquí
              </h2>
            </div>
            {/*
              Antes había un mapa embebido con coordenadas fijas de Tarija,
              mientras el negocio está en Santa Cruz: marcaba un punto
              equivocado. Como la dirección se configura desde el sistema y
              puede cambiar, se busca por texto en lugar de fijar un punto.
            */}
            <div className="p-8 flex flex-col items-center text-center gap-4">
              <MapPin className="w-10 h-10 text-primary-600" />
              <p className="text-lg font-semibold text-gray-900">{contactInfo.direccion}</p>
              <a
                href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(contactInfo.direccion)}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white font-bold px-6 py-3 rounded-xl transition-colors"
              >
                <MapPin className="w-4 h-4" />
                Ver en Google Maps
              </a>
            </div>
          </div>
        </div>
      </div>

      <style jsx>{`
        @keyframes bounce-in {
          0% {
            opacity: 0;
            transform: scale(0.9);
          }
          50% {
            transform: scale(1.05);
          }
          100% {
            opacity: 1;
            transform: scale(1);
          }
        }
        .animate-bounce-in {
          animation: bounce-in 0.5s ease-out;
        }
      `}</style>
    </div>
  );
}
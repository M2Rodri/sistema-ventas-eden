'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { 
  getAllConfiguraciones, 
  updateValorByClave,
  getUltimasAuditorias,
  getAllAuditorias,
  getAuditoriasByUsuario,
  getAuditoriasByTabla,
  getAuditoriasByAccion,
  getAllUsers,
  getMensajesContacto,
  marcarMensajeAtendido,
  MensajeContacto
} from '@/lib/api';
import { Configuracion, Auditoria } from '@/types/configuracion';
import { User } from '@/lib/api';
import { Settings, Building2, Mail, Shield, Search, Eye, Inbox, CheckCircle } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import AuditoriaDetalleModal from '@/components/AuditoriaDetalleModal';
import AvisoCargaParcial from '@/components/AvisoCargaParcial';
import { crearRecolector } from '@/lib/cargaParcial';

type TabType = 'general' | 'empresa' | 'auditoria' | 'mensajes';

export default function ConfiguracionPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<TabType>('general');
  const [mensajes, setMensajes] = useState<MensajeContacto[]>([]);
  const [soloPendientes, setSoloPendientes] = useState(true);
  const [configuraciones, setConfiguraciones] = useState<Configuracion[]>([]);
  const [auditorias, setAuditorias] = useState<Auditoria[]>([]);
  const [filteredAuditorias, setFilteredAuditorias] = useState<Auditoria[]>([]);
  const [usuarios, setUsuarios] = useState<User[]>([]);
  const [fallosCarga, setFallosCarga] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  // Consultas recibidas desde el formulario de la tienda
  const cargarMensajes = async () => {
    try {
      setMensajes(await getMensajesContacto());
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  useEffect(() => {
    if (activeTab === 'mensajes') cargarMensajes();
  }, [activeTab]);

  const handleAtender = async (id: number) => {
    if (!user?.id) return;
    try {
      await marcarMensajeAtendido(id, user.id);
      showMessage('success', 'Mensaje marcado como atendido');
      cargarMensajes();
    } catch (error: any) {
      showMessage('error', error.message);
    }
  };

  // Estados para formularios
  const [configGeneral, setConfigGeneral] = useState({
    nombre_sistema: '',
    zona_horaria: 'America/La_Paz',
    moneda: 'BOB',
    stock_minimo_defecto: '5',
    modo_mantenimiento: 'false',
  });

  const [configEmpresa, setConfigEmpresa] = useState({
    negocio_razon_social: '',
    negocio_nit: '',
    negocio_direccion: '',
    negocio_telefono: '',
    negocio_email: '',
    negocio_sitio_web: '',
  });


  // Filtros de auditoría
  const [searchAuditoria, setSearchAuditoria] = useState('');
  const [usuarioFilter, setUsuarioFilter] = useState<string>('TODOS');
  const [accionFilter, setAccionFilter] = useState<string>('TODAS');
  const [tablaFilter, setTablaFilter] = useState<string>('TODAS');

  // Modal
  const [selectedAuditoria, setSelectedAuditoria] = useState<Auditoria | null>(null);
  const [isDetalleModalOpen, setIsDetalleModalOpen] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // allSettled: la auditoria y el listado de usuarios son pestanas aparte;
      // si fallan, la configuracion del negocio igual se tiene que poder editar.
      const [rConfigs, rAuditorias, rUsuarios] = await Promise.allSettled([
        getAllConfiguraciones(),
        getUltimasAuditorias(),
        getAllUsers()
      ]);

      const { tomar, fallos } = crearRecolector();
      const configsData = tomar(rConfigs, 'la configuracion', [] as Configuracion[]);
      const auditoriasData = tomar(rAuditorias, 'la auditoria', [] as Auditoria[]);
      const usuariosData = tomar(rUsuarios, 'los usuarios', [] as User[]);

      setConfiguraciones(configsData);
      setAuditorias(auditoriasData);
      setFilteredAuditorias(auditoriasData);
      setUsuarios(usuariosData);
      setFallosCarga(fallos);
      
      // Mapear configuraciones a estados
      mapConfiguraciones(configsData);
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setLoading(false);
    }
  };

  const mapConfiguraciones = (configs: Configuracion[]) => {
    const configMap: { [key: string]: string } = {};
    configs.forEach(c => {
      configMap[c.clave] = c.valor;
    });

    // General
    setConfigGeneral({
      nombre_sistema: configMap['nombre_sistema'] || 'Mueblería Edén',
      zona_horaria: configMap['zona_horaria'] || 'America/La_Paz',
      moneda: configMap['moneda'] || 'BOB',
      stock_minimo_defecto: configMap['stock_minimo_defecto'] || '5',
      modo_mantenimiento: configMap['modo_mantenimiento'] || 'false',
    });

    // Empresa
    setConfigEmpresa({
      negocio_razon_social: configMap['negocio_razon_social'] || '',
      negocio_nit: configMap['negocio_nit'] || '',
      negocio_direccion: configMap['negocio_direccion'] || '',
      negocio_telefono: configMap['negocio_telefono'] || '',
      negocio_email: configMap['negocio_email'] || '',
      negocio_sitio_web: configMap['negocio_sitio_web'] || '',
    });

    // Email
  };

  // Filtrar auditorías
  useEffect(() => {
    let filtered = auditorias;

    if (searchAuditoria) {
      filtered = filtered.filter(a =>
        a.nombreUsuario.toLowerCase().includes(searchAuditoria.toLowerCase()) ||
        a.accion.toLowerCase().includes(searchAuditoria.toLowerCase()) ||
        a.tablaAfectada.toLowerCase().includes(searchAuditoria.toLowerCase())
      );
    }

    if (usuarioFilter !== 'TODOS') {
      filtered = filtered.filter(a => a.idUsuario === parseInt(usuarioFilter));
    }

    if (accionFilter !== 'TODAS') {
      filtered = filtered.filter(a => a.accion === accionFilter);
    }

    if (tablaFilter !== 'TODAS') {
      filtered = filtered.filter(a => a.tablaAfectada === tablaFilter);
    }

    setFilteredAuditorias(filtered);
  }, [searchAuditoria, usuarioFilter, accionFilter, tablaFilter, auditorias]);

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text });
    setTimeout(() => setMessage(null), 4000);
  };

  const handleSaveGeneral = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);

    try {
      // Actualizar cada configuración
      for (const [clave, valor] of Object.entries(configGeneral)) {
        await updateValorByClave(clave, valor);
      }
      showMessage('success', 'Configuraciones generales guardadas correctamente');
      loadData();
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setSaving(false);
    }
  };

  const handleSaveEmpresa = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);

    try {
      for (const [clave, valor] of Object.entries(configEmpresa)) {
        await updateValorByClave(clave, valor);
      }
      showMessage('success', 'Datos de empresa guardados correctamente');
      loadData();
    } catch (error: any) {
      showMessage('error', error.message);
    } finally {
      setSaving(false);
    }
  };

  const handleVerDetalle = (auditoria: Auditoria) => {
    setSelectedAuditoria(auditoria);
    setIsDetalleModalOpen(true);
  };

  // El backend registra acciones como CREAR_PRODUCTO o ACTUALIZAR_USUARIO, no
  // CREATE ni UPDATE. Antes esta tabla buscaba coincidencia exacta contra los
  // nombres en ingles, asi que toda la auditoria habria salido en gris.
  // Se compara por prefijo porque cada accion lleva pegado el nombre de la
  // entidad.
  const getAccionBadge = (accion: string) => {
    if (accion.startsWith('LOGIN_FALLIDO') || accion.startsWith('LOGIN_RECHAZADO')) {
      // Un intento de ingreso rechazado es lo que mas conviene que salte a la vista.
      return 'bg-orange-100 text-orange-800';
    }
    if (accion.startsWith('LOGIN')) return 'bg-purple-100 text-purple-800';
    if (accion.startsWith('LOGOUT')) return 'bg-gray-100 text-gray-800';
    if (accion.startsWith('CREAR')) return 'bg-green-100 text-green-800';
    if (accion.startsWith('ACTUALIZAR') || accion.startsWith('CAMBIAR') || accion.startsWith('AJUSTAR')) {
      return 'bg-blue-100 text-blue-800';
    }
    if (accion.startsWith('ELIMINAR') || accion.startsWith('CANCELAR') || accion.startsWith('DESACTIVAR')) {
      return 'bg-red-100 text-red-800';
    }
    if (accion.startsWith('ACTIVAR')) return 'bg-green-100 text-green-800';
    return 'bg-gray-100 text-gray-800';
  };

  /** CREAR_PRODUCTO -> "Crear producto", para que la tabla se lea. */
  const formatearAccion = (accion: string) => {
    const texto = accion.replace(/_/g, ' ').toLowerCase();
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-BO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Obtener tablas únicas
  const tablasUnicas = Array.from(new Set(auditorias.map(a => a.tablaAfectada))).sort();
  const accionesUnicas = Array.from(new Set(auditorias.map(a => a.accion))).sort();

  if (loading) {
    return (
      <div>
        <Breadcrumbs items={[{ label: 'Configuración' }]} />
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Configuración' }]} />

      <AvisoCargaParcial fallos={fallosCarga} onReintentar={loadData} />

      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Configuración del Sistema</h1>
        <p className="text-gray-600 mt-1">Ajustes generales y mantenimiento</p>
      </div>

      {/* Mensaje de éxito/error */}
      {message && (
        <div className={`p-4 rounded-lg mb-6 ${message.type === 'success' ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
          {message.text}
        </div>
      )}

      {/* Pestañas */}
      <div className="flex gap-4 mb-6 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('general')}
          className={`px-6 py-3 font-medium transition-all ${
            activeTab === 'general'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <div className="flex items-center gap-2">
            <Settings size={20} />
            General
          </div>
        </button>
        <button
          onClick={() => setActiveTab('empresa')}
          className={`px-6 py-3 font-medium transition-all ${
            activeTab === 'empresa'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <div className="flex items-center gap-2">
            <Building2 size={20} />
            Tienda
          </div>
        </button>
        <button
          onClick={() => setActiveTab('auditoria')}
          className={`px-6 py-3 font-medium transition-all ${
            activeTab === 'auditoria'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <div className="flex items-center gap-2">
            <Shield size={20} />
            Auditoría
          </div>
        </button>
        <button
          onClick={() => setActiveTab('mensajes')}
          className={`px-6 py-3 font-medium transition-all ${
            activeTab === 'mensajes'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          <div className="flex items-center gap-2">
            <Inbox size={20} />
            Consultas
            {mensajes.filter((m) => !m.atendido).length > 0 && (
              <span className="bg-red-500 text-white text-xs px-2 py-0.5 rounded-full">
                {mensajes.filter((m) => !m.atendido).length}
              </span>
            )}
          </div>
        </button>
      </div>

      {/* ========== PESTAÑA: GENERAL ========== */}
      {activeTab === 'general' && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <form onSubmit={handleSaveGeneral} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Nombre del Sistema
              </label>
              <input
                type="text"
                value={configGeneral.nombre_sistema}
                onChange={(e) => setConfigGeneral({ ...configGeneral, nombre_sistema: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Zona Horaria
                </label>
                <select
                  value={configGeneral.zona_horaria}
                  onChange={(e) => setConfigGeneral({ ...configGeneral, zona_horaria: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
                >
                  <option value="America/La_Paz">América/La Paz (BOT)</option>
                  <option value="America/Buenos_Aires">América/Buenos Aires</option>
                  <option value="America/Lima">América/Lima</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Moneda
                </label>
                <select
                  value={configGeneral.moneda}
                  onChange={(e) => setConfigGeneral({ ...configGeneral, moneda: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
                >
                  <option value="BOB">Bs. - Bolivianos</option>
                  <option value="USD">$ - Dólares</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Stock Mínimo por Defecto
              </label>
              <input
                type="number"
                min="0"
                value={configGeneral.stock_minimo_defecto}
                onChange={(e) => setConfigGeneral({ ...configGeneral, stock_minimo_defecto: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div className="space-y-3 border-t pt-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={configGeneral.modo_mantenimiento === 'true'}
                  onChange={(e) => setConfigGeneral({ ...configGeneral, modo_mantenimiento: e.target.checked ? 'true' : 'false' })}
                  className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
                />
                <div>
                  <span className="text-sm font-medium text-gray-900">Modo Mantenimiento</span>
                  <p className="text-xs text-gray-500">Desactiva temporalmente la tienda virtual</p>
                </div>
              </label>

              {/*
                Se quitaron "Permitir registro de clientes" y "Mostrar precios
                sin iniciar sesión": la tienda es una vitrina pública sin inicio
                de sesión, así que ninguna de las dos tenía efecto posible.
              */}
            </div>

            <div className="flex justify-end pt-4 border-t">
              <button
                type="submit"
                disabled={saving}
                className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
              >
                {saving ? 'Guardando...' : 'Guardar Cambios'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ========== PESTAÑA: EMPRESA ========== */}
      {activeTab === 'empresa' && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <form onSubmit={handleSaveEmpresa} className="space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Razón Social
                </label>
                <input
                  type="text"
                  value={configEmpresa.negocio_razon_social}
                  onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_razon_social: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  NIT
                </label>
                <input
                  type="text"
                  value={configEmpresa.negocio_nit}
                    onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_nit: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Dirección
              </label>
              <input
                type="text"
                value={configEmpresa.negocio_direccion}
                onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_direccion: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Teléfono
                </label>
                <input
                  type="tel"
                  value={configEmpresa.negocio_telefono}
                  onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_telefono: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email
                </label>
                <input
                  type="email"
                  value={configEmpresa.negocio_email}
                  onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_email: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Sitio Web
              </label>
              <input
                type="url"
                value={configEmpresa.negocio_sitio_web}
                onChange={(e) => setConfigEmpresa({ ...configEmpresa, negocio_sitio_web: e.target.value })}
                placeholder="https://www.ejemplo.com"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div className="flex justify-end pt-4 border-t">
              <button
                type="submit"
                disabled={saving}
                className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50"
              >
                {saving ? 'Guardando...' : 'Guardar Cambios'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ========== PESTAÑA: EMAIL ========== */}
      {/* ========== PESTAÑA: AUDITORÍA ========== */}
      {activeTab === 'auditoria' && (
        <div>
          <div className="bg-white rounded-lg border border-gray-200 p-4 mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Registro de Actividades del Sistema</h3>
            
            {/* Filtros */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              {/* Búsqueda */}
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  placeholder="Buscar..."
                  value={searchAuditoria}
                  onChange={(e) => setSearchAuditoria(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              {/* Filtro por usuario */}
              <select
                value={usuarioFilter}
                onChange={(e) => setUsuarioFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="TODOS">Todos los usuarios</option>
                {usuarios.map(usuario => (
                  <option key={usuario.id} value={usuario.id}>{usuario.nombreCompleto}</option>
                ))}
              </select>

              {/* Filtro por acción */}
              <select
                value={accionFilter}
                onChange={(e) => setAccionFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="TODAS">Todas las acciones</option>
                {accionesUnicas.map(accion => (
                  <option key={accion} value={accion}>{accion}</option>
                ))}
              </select>

              {/* Filtro por tabla */}
              <select
                value={tablaFilter}
                onChange={(e) => setTablaFilter(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                <option value="TODAS">Todas las tablas</option>
                {tablasUnicas.map(tabla => (
                  <option key={tabla} value={tabla}>{tabla}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Tabla de Auditoría */}
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Usuario</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Acción</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tabla</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fecha/Hora</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">IP</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Detalle</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredAuditorias.map((auditoria) => (
                    <tr key={auditoria.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{auditoria.nombreUsuario}</div>
                        {auditoria.idUsuario && (
                          <div className="text-xs text-gray-500">ID: {auditoria.idUsuario}</div>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getAccionBadge(auditoria.accion)}`}>
                          {formatearAccion(auditoria.accion)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {auditoria.tablaAfectada}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                        {formatDateTime(auditoria.fechaHora)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 font-mono">
                        {auditoria.ipDispositivo || '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <button
                          onClick={() => handleVerDetalle(auditoria)}
                          className="text-primary-600 hover:text-primary-800 transition-colors flex items-center gap-1"
                        >
                          <Eye size={18} />
                          Ver
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {filteredAuditorias.length === 0 && (
                <div className="text-center py-12">
                  <Shield size={48} className="mx-auto text-gray-400 mb-4" />
                  <p className="text-gray-500">No se encontraron registros de auditoría</p>
                </div>
              )}
            </div>
          </div>

          {/* Información adicional */}
          <div className="mt-4 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              <strong>ℹ️ Nota:</strong> Los registros de auditoría son inmutables y no se pueden eliminar. 
              Se muestran las últimas 100 actividades.
            </p>
          </div>
        </div>
      )}

      {/* Modal de Detalle */}
      {isDetalleModalOpen && selectedAuditoria && (
        <AuditoriaDetalleModal
          auditoria={selectedAuditoria}
          onClose={() => {
            setIsDetalleModalOpen(false);
            setSelectedAuditoria(null);
          }}
        />
      )}

      {/* ========== PESTAÑA: CONSULTAS DE LA TIENDA ========== */}
      {activeTab === 'mensajes' && (
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Consultas de la tienda</h2>
              <p className="text-sm text-gray-500">
                Mensajes que los visitantes envían desde el formulario de contacto.
              </p>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-700">
              <input
                type="checkbox"
                checked={soloPendientes}
                onChange={(e) => setSoloPendientes(e.target.checked)}
                className="w-4 h-4 rounded"
              />
              Ver solo pendientes
            </label>
          </div>

          {(() => {
            const visibles = soloPendientes ? mensajes.filter((m) => !m.atendido) : mensajes;

            if (visibles.length === 0) {
              return (
                <div className="text-center py-12 bg-gray-50 border border-gray-200 rounded-lg">
                  <Inbox size={40} className="mx-auto text-gray-400 mb-3" />
                  <p className="text-gray-600 font-medium">
                    {soloPendientes ? 'No hay consultas pendientes' : 'Todavía no llegaron consultas'}
                  </p>
                  <p className="text-sm text-gray-500 mt-1">
                    Las consultas enviadas desde la tienda aparecen acá.
                  </p>
                </div>
              );
            }

            return (
              <div className="space-y-4">
                {visibles.map((m) => (
                  <div
                    key={m.id}
                    className={`border rounded-lg p-5 ${
                      m.atendido ? 'border-gray-200 bg-gray-50' : 'border-amber-200 bg-amber-50'
                    }`}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="font-semibold text-gray-900">{m.asunto}</p>
                        <p className="text-sm text-gray-600">
                          {m.nombre} · {m.email}
                          {m.telefono ? ` · ${m.telefono}` : ''}
                        </p>
                      </div>
                      <span className="text-xs text-gray-500 whitespace-nowrap">
                        {new Date(m.fechaEnvio).toLocaleString('es-BO')}
                      </span>
                    </div>

                    <p className="text-sm text-gray-800 mt-3 whitespace-pre-line">{m.mensaje}</p>

                    <div className="flex flex-wrap items-center gap-3 mt-4 pt-3 border-t border-gray-200">
                      {m.atendido ? (
                        <span className="inline-flex items-center gap-1.5 text-sm text-green-700">
                          <CheckCircle size={16} />
                          Atendido por {m.nombreUsuarioAtiende}
                          {m.fechaAtencion ? ` el ${new Date(m.fechaAtencion).toLocaleDateString('es-BO')}` : ''}
                        </span>
                      ) : (
                        <>
                          <button
                            onClick={() => handleAtender(m.id)}
                            className="inline-flex items-center gap-1.5 px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 transition-colors"
                          >
                            <CheckCircle size={16} />
                            Marcar como atendido
                          </button>
                          <a
                            href={`mailto:${m.email}?subject=Re: ${encodeURIComponent(m.asunto)}`}
                            className="text-sm text-primary-600 hover:text-primary-800"
                          >
                            Responder por correo
                          </a>
                          {m.telefono && (
                            <a
                              href={`https://wa.me/${m.telefono.replace(/[^0-9]/g, '')}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-sm text-green-600 hover:text-green-800"
                            >
                              Responder por WhatsApp
                            </a>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            );
          })()}
        </div>
      )}
    </div>
  );
}
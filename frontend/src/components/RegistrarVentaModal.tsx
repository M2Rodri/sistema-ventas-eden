"use client";

import React, { useState, useEffect } from "react";
import {
  X,
  AlertCircle,
  User,
  Phone,
  Search,
  Plus,
  Trash2,
  Package,
} from "lucide-react";
import {
  VentaRequest,
  MetodoPago,
  ItemVentaRequest,
  ModalidadEntrega,
} from "@/types/venta";
import { createVentaDirecta, getAllProductos, getAllClientes } from "@/lib/api";

interface ProductoCarrito {
  idProducto: number;
  nombre: string;
  sku: string;
  precioOriginal: number;
  precioFinal: number;
  cantidad: number;
  descuentoPorcentaje: number;
  subtotal: number;
}

interface RegistrarVentaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  idUsuarioActual: number;
  userRole: "ADMIN" | "EMPLEADO";
}

export default function RegistrarVentaModal({
  isOpen,
  onClose,
  onSuccess,
  idUsuarioActual,
  userRole,
}: RegistrarVentaModalProps) {
  // Cliente
  const [nombreCliente, setNombreCliente] = useState("");
  const [telefonoCliente, setCelularCliente] = useState("");
  const [busquedaCliente, setBusquedaCliente] = useState("");
  const [clientesEncontrados, setClientesEncontrados] = useState<any[]>([]);
  const [clienteSeleccionado, setClienteSeleccionado] = useState<any | null>(
    null,
  );

  // Productos
  const [productos, setProductos] = useState<any[]>([]);
  const [busquedaProducto, setBusquedaProducto] = useState("");
  const [productosFiltrados, setProductosFiltrados] = useState<any[]>([]);
  const [carrito, setCarrito] = useState<ProductoCarrito[]>([]);

  // Entrega
  const [modalidadEntrega, setModalidadEntrega] = useState<ModalidadEntrega>(
    ModalidadEntrega.RETIRO,
  );
  const [direccionDestino, setDireccionDestino] = useState("");
  const [ciudad, setCiudad] = useState("");
  const [transportadora, setTransportadora] = useState("");
  const [guiaRemision, setGuiaRemision] = useState("");

  // Pagos mixtos
  const [pagos, setPagos] = useState<
    { metodo: MetodoPago; monto: number; referencia: string }[]
  >([{ metodo: MetodoPago.EFECTIVO, monto: 0, referencia: "" }]);
  const [clientes, setClientes] = useState<any[]>([]);

  // Estado
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paso, setPaso] = useState(1);

  useEffect(() => {
    if (isOpen) cargarProductos();
    cargarClientes();
  }, [isOpen]);

  const cargarProductos = async () => {
    try {
      const data = await getAllProductos();
      setProductos(data);
    } catch (err) {
      console.error("Error al cargar productos:", err);
    }
  };
  const cargarClientes = async () => {
    try {
      const data = await getAllClientes();
      setClientes(data);
    } catch (err) {
      console.error("Error al cargar clientes:", err);
    }
  };

  useEffect(() => {
    if (busquedaProducto.trim().length > 0) {
      const filtrados = productos.filter(
        (p) =>
          p.nombre.toLowerCase().includes(busquedaProducto.toLowerCase()) ||
          p.sku.toLowerCase().includes(busquedaProducto.toLowerCase()),
      );
      setProductosFiltrados(filtrados);
    } else {
      setProductosFiltrados([]);
    }
  }, [busquedaProducto, productos]);

  const agregarAlCarrito = (producto: any) => {
    const existe = carrito.find((item) => item.idProducto === producto.id);
    if (existe) {
      setCarrito(
        carrito.map((item) =>
          item.idProducto === producto.id
            ? {
                ...item,
                cantidad: item.cantidad + 1,
                subtotal: (item.cantidad + 1) * item.precioFinal,
              }
            : item,
        ),
      );
    } else {
      setCarrito([
        ...carrito,
        {
          idProducto: producto.id,
          nombre: producto.nombre,
          sku: producto.sku,
          precioOriginal: producto.precioVenta,
          precioFinal: producto.precioVenta,
          cantidad: 1,
          descuentoPorcentaje: 0,
          subtotal: producto.precioVenta,
        },
      ]);
    }
    setBusquedaProducto("");
    setProductosFiltrados([]);
  };

  const actualizarCantidad = (idProducto: number, cantidad: number) => {
    if (cantidad < 1) return;
    setCarrito(
      carrito.map((item) =>
        item.idProducto === idProducto
          ? { ...item, cantidad, subtotal: cantidad * item.precioFinal }
          : item,
      ),
    );
  };

  const aplicarDescuento = (idProducto: number, porcentaje: number) => {
    if (porcentaje < 0 || porcentaje > 100) return;
    setCarrito(
      carrito.map((item) => {
        if (item.idProducto === idProducto) {
          const precioFinal = item.precioOriginal * (1 - porcentaje / 100);
          return {
            ...item,
            descuentoPorcentaje: porcentaje,
            precioFinal,
            subtotal: item.cantidad * precioFinal,
          };
        }
        return item;
      }),
    );
  };

  const eliminarDelCarrito = (idProducto: number) => {
    setCarrito(carrito.filter((item) => item.idProducto !== idProducto));
  };

  const totalVenta = carrito.reduce((acc, item) => acc + item.subtotal, 0);

  // Por defecto se cobra el total. Si el cajero quiere registrar una venta
  // a crédito (parcial o sin pago), reduce el monto a mano más abajo.
  useEffect(() => {
    if (pagos.length === 1) {
      setPagos([{ ...pagos[0], monto: totalVenta }]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [totalVenta]);

  const montoPagado = pagos.reduce((acc, p) => acc + p.monto, 0);

  const handleSubmit = async () => {
    setError(null);

    if (!clienteSeleccionado && !nombreCliente.trim()) {
      setError("El nombre del cliente es obligatorio");
      return;
    }

    if (carrito.length === 0) {
      setError("Debe agregar al menos un producto");
      return;
    }

    if (montoPagado > totalVenta) {
      setError("El monto pagado no puede superar el total de la venta");
      return;
    }

    if (modalidadEntrega !== ModalidadEntrega.RETIRO) {
      if (!direccionDestino.trim() || !ciudad.trim()) {
        setError("La dirección y la ciudad son obligatorias para esta modalidad de entrega");
        return;
      }
    }
    if (modalidadEntrega === ModalidadEntrega.TRANSPORTADORA) {
      if (!transportadora.trim() || !guiaRemision.trim()) {
        setError("La transportadora y la guía de remisión son obligatorias para esta modalidad de entrega");
        return;
      }
    }

    setLoading(true);
    try {
      const items: ItemVentaRequest[] = carrito.map((item) => ({
        idProducto: item.idProducto,
        cantidad: item.cantidad,
        precioUnitarioConDescuento:
          item.descuentoPorcentaje > 0 ? item.precioFinal : undefined,
        descuentoPorcentaje:
          item.descuentoPorcentaje > 0 ? item.descuentoPorcentaje : undefined,
      }));

      const request: VentaRequest = {
        idCliente: clienteSeleccionado?.id,
        nombreClienteInvitado: !clienteSeleccionado
          ? nombreCliente.trim()
          : undefined,
        telefonoClienteInvitado: !clienteSeleccionado
          ? telefonoCliente.trim() || undefined
          : undefined,
        metodoPago: pagos[0].metodo,
        referenciaPago: pagos[0].referencia || undefined,
        items,
        montoPagado,
        modalidadEntrega,
        direccionDestino:
          modalidadEntrega !== ModalidadEntrega.RETIRO
            ? direccionDestino.trim()
            : undefined,
        ciudad:
          modalidadEntrega !== ModalidadEntrega.RETIRO
            ? ciudad.trim()
            : undefined,
        transportadora:
          modalidadEntrega === ModalidadEntrega.TRANSPORTADORA
            ? transportadora.trim()
            : undefined,
        guiaRemision:
          modalidadEntrega === ModalidadEntrega.TRANSPORTADORA
            ? guiaRemision.trim()
            : undefined,
      };

      await createVentaDirecta(request, idUsuarioActual);
      onSuccess();
      resetForm();
      onClose();
    } catch (err: any) {
      setError(err.message || "Error al registrar la venta");
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setNombreCliente("");
    setCelularCliente("");
    setBusquedaCliente("");
    setClientesEncontrados([]);
    setClienteSeleccionado(null);
    setCarrito([]);
    setBusquedaProducto("");
    setModalidadEntrega(ModalidadEntrega.RETIRO);
    setDireccionDestino("");
    setCiudad("");
    setTransportadora("");
    setGuiaRemision("");
    setError(null);
    setPaso(1);
    setPagos([{ metodo: MetodoPago.EFECTIVO, monto: 0, referencia: "" }]);
    setClientes([]);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex justify-between items-center p-6 border-b sticky top-0 bg-white z-10">
          <h2 className="text-2xl font-bold text-gray-900">
            Registrar Nueva Venta
          </h2>
          <button
            onClick={() => {
              resetForm();
              onClose();
            }}
            disabled={loading}
          >
            <X size={24} />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
              <AlertCircle className="text-red-600 flex-shrink-0" size={20} />
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          {/* SECCIÓN 1 — CLIENTE */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <User size={20} /> Cliente
            </h3>

            {/* Buscador de cliente existente */}
            <div className="relative mb-3">
              <div className="flex items-center border border-gray-300 rounded-lg px-4 py-2 gap-2">
                <Search size={18} className="text-gray-400" />
                <input
                  type="text"
                  value={busquedaCliente}
                  onChange={(e) => {
                    setBusquedaCliente(e.target.value);
                    setClienteSeleccionado(null);
                  }}
                  className="flex-1 outline-none text-sm"
                  placeholder="Buscar cliente por nombre o celular..."
                />
                {clienteSeleccionado && (
                  <button
                    onClick={() => {
                      setClienteSeleccionado(null);
                      setBusquedaCliente("");
                    }}
                  >
                    <X size={16} className="text-gray-400" />
                  </button>
                )}
              </div>

              {/* Resultados búsqueda */}
              {busquedaCliente.length > 1 && !clienteSeleccionado && (
                <div className="absolute top-full left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg z-20 max-h-40 overflow-y-auto">
                  {clientes
                    .filter(
                      (c) =>
                        c.nombre
                          ?.toLowerCase()
                          .includes(busquedaCliente.toLowerCase()) ||
                        c.apellido
                          ?.toLowerCase()
                          .includes(busquedaCliente.toLowerCase()) ||
                        c.celular?.includes(busquedaCliente),
                    )
                    .map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        onClick={() => {
                          setClienteSeleccionado(c);
                          setBusquedaCliente(`${c.nombre} ${c.apellido || ""}`);
                        }}
                        className="w-full text-left px-4 py-3 hover:bg-gray-50 border-b last:border-0"
                      >
                        <p className="font-medium text-sm">
                          {c.nombre} {c.apellido}
                        </p>
                        <p className="text-xs text-gray-500">
                          {c.celular || "Sin celular"}
                        </p>
                      </button>
                    ))}
                  {clientes.filter(
                    (c) =>
                      c.nombre
                        ?.toLowerCase()
                        .includes(busquedaCliente.toLowerCase()) ||
                      c.celular?.includes(busquedaCliente),
                  ).length === 0 && (
                    <p className="text-sm text-gray-400 px-4 py-3">
                      No encontrado. Registrá abajo como cliente nuevo.
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Si no seleccionó cliente existente, registrar nuevo */}
            {!clienteSeleccionado && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Nombre Completo <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={nombreCliente}
                    onChange={(e) => setNombreCliente(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Ej: Juan Pérez"
                    maxLength={100}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Celular (opcional)
                  </label>
                  <input
                    type="text"
                    value={telefonoCliente}
                    onChange={(e) => setCelularCliente(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Ej: 71234567"
                    maxLength={20}
                  />
                </div>
              </div>
            )}

            {clienteSeleccionado && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-800">
                ✔ Cliente seleccionado:{" "}
                <strong>
                  {clienteSeleccionado.nombre} {clienteSeleccionado.apellido}
                </strong>
              </div>
            )}
          </div>

          {/* SECCIÓN 2 — PRODUCTOS */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Package size={20} /> Productos
            </h3>

            {/* Buscador */}
            <div className="relative mb-4">
              <div className="flex items-center border border-gray-300 rounded-lg px-4 py-2 gap-2">
                <Search size={18} className="text-gray-400" />
                <input
                  type="text"
                  value={busquedaProducto}
                  onChange={(e) => setBusquedaProducto(e.target.value)}
                  className="flex-1 outline-none text-sm"
                  placeholder="Buscar por nombre o SKU..."
                />
              </div>
              {productosFiltrados.length > 0 && (
                <div className="absolute top-full left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg z-20 max-h-48 overflow-y-auto">
                  {productosFiltrados.map((p) => (
                    <button
                      key={p.id}
                      type="button"
                      onClick={() => agregarAlCarrito(p)}
                      className="w-full text-left px-4 py-3 hover:bg-gray-50 flex justify-between items-center border-b last:border-0"
                    >
                      <div>
                        <p className="font-medium text-sm">{p.nombre}</p>
                        <p className="text-xs text-gray-500">
                          SKU: {p.sku} — Stock: {p.stock}
                        </p>
                      </div>
                      <span className="text-sm font-semibold text-green-600">
                        Bs. {p.precioVenta}
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Carrito */}
            {carrito.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-4 border border-dashed rounded-lg">
                Ningún producto agregado. Buscá arriba para agregar.
              </p>
            ) : (
              <div className="space-y-3">
                {carrito.map((item) => (
                  <div
                    key={item.idProducto}
                    className="border border-gray-200 rounded-lg p-4"
                  >
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <p className="font-medium text-sm">{item.nombre}</p>
                        <p className="text-xs text-gray-500">SKU: {item.sku}</p>
                      </div>
                      <button
                        onClick={() => eliminarDelCarrito(item.idProducto)}
                        className="text-red-400 hover:text-red-600"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div>
                        <label className="text-xs text-gray-500 block mb-1">
                          Cantidad
                        </label>
                        <input
                          type="number"
                          min={1}
                          value={item.cantidad}
                          onChange={(e) =>
                            actualizarCantidad(
                              item.idProducto,
                              parseInt(e.target.value),
                            )
                          }
                          className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500 block mb-1">
                          Descuento %
                        </label>
                        <input
                          type="number"
                          min={0}
                          max={100}
                          value={item.descuentoPorcentaje}
                          onChange={(e) =>
                            aplicarDescuento(
                              item.idProducto,
                              parseFloat(e.target.value) || 0,
                            )
                          }
                          className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500 block mb-1">
                          Subtotal
                        </label>
                        <p className="text-sm font-semibold text-green-600 pt-1">
                          Bs. {item.subtotal.toFixed(2)}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
                <div className="flex justify-end pt-2">
                  <p className="text-lg font-bold text-gray-900">
                    Total: Bs. {totalVenta.toFixed(2)}
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* SECCIÓN 3 — PAGOS MIXTOS */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Pagos</h3>

            {pagos.map((pago, index) => (
              <div
                key={index}
                className="border border-gray-200 rounded-lg p-4 mb-3"
              >
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 items-end">
                  <div>
                    <label className="text-xs text-gray-500 block mb-1">
                      Método
                    </label>
                    <select
                      value={pago.metodo}
                      onChange={(e) => {
                        const nuevos = [...pagos];
                        nuevos[index].metodo = e.target.value as MetodoPago;
                        setPagos(nuevos);
                      }}
                      className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                    >
                      {Object.values(MetodoPago).map((m) => (
                        <option key={m} value={m}>
                          {m}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="text-xs text-gray-500 block mb-1">
                      Monto (Bs.)
                    </label>
                    <input
                      type="number"
                      min={0}
                      value={pago.monto}
                      onChange={(e) => {
                        const nuevos = [...pagos];
                        nuevos[index].monto = parseFloat(e.target.value) || 0;
                        setPagos(nuevos);
                      }}
                      className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                    />
                  </div>
                </div>
                {pagos.length > 1 && (
                  <div className="flex justify-end mt-2">
                    <button
                      type="button"
                      onClick={() =>
                        setPagos(pagos.filter((_, i) => i !== index))
                      }
                      className="text-red-400 hover:text-red-600"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                )}
              </div>
            ))}

            <button
              type="button"
              onClick={() =>
                setPagos([
                  ...pagos,
                  { metodo: MetodoPago.EFECTIVO, monto: 0, referencia: "" },
                ])
              }
              className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-800 font-medium"
            >
              <Plus size={16} /> Agregar otro método de pago
            </button>

            {/* Resumen pagos */}
            <div className="mt-4 p-3 bg-gray-50 rounded-lg">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Total venta:</span>
                <span className="font-semibold">
                  Bs. {totalVenta.toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm mt-1">
                <span className="text-gray-600">Total pagado:</span>
                <span className="font-semibold">
                  Bs. {pagos.reduce((acc, p) => acc + p.monto, 0).toFixed(2)}
                </span>
              </div>
              <div className="flex justify-between text-sm mt-1">
                <span className="text-gray-600">Saldo pendiente:</span>
                <span
                  className={`font-semibold ${totalVenta - pagos.reduce((acc, p) => acc + p.monto, 0) > 0 ? "text-red-600" : "text-green-600"}`}
                >
                  Bs.{" "}
                  {(
                    totalVenta - pagos.reduce((acc, p) => acc + p.monto, 0)
                  ).toFixed(2)}
                </span>
              </div>
            </div>
          </div>

          {/* SECCIÓN 4 — ENTREGA */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Entrega
            </h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Modalidad
              </label>
              <select
                value={modalidadEntrega}
                onChange={(e) =>
                  setModalidadEntrega(e.target.value as ModalidadEntrega)
                }
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value={ModalidadEntrega.RETIRO}>
                  Retiro en tienda
                </option>
                <option value={ModalidadEntrega.DOMICILIO}>
                  Entrega a domicilio
                </option>
                <option value={ModalidadEntrega.TRANSPORTADORA}>
                  Envío por transportadora
                </option>
              </select>
            </div>

            {modalidadEntrega !== ModalidadEntrega.RETIRO && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Dirección <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={direccionDestino}
                    onChange={(e) => setDireccionDestino(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Dirección de entrega"
                    maxLength={300}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Ciudad <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={ciudad}
                    onChange={(e) => setCiudad(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Ej: Santa Cruz de la Sierra"
                    maxLength={50}
                  />
                </div>
              </div>
            )}

            {modalidadEntrega === ModalidadEntrega.TRANSPORTADORA && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Transportadora <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={transportadora}
                    onChange={(e) => setTransportadora(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Nombre de la transportadora"
                    maxLength={100}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Guía de remisión <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={guiaRemision}
                    onChange={(e) => setGuiaRemision(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                    placeholder="Número de guía"
                    maxLength={100}
                  />
                </div>
              </div>
            )}
          </div>

          {/* BOTONES */}
          <div className="flex gap-4 pt-2">
            <button
              type="button"
              onClick={() => {
                resetForm();
                onClose();
              }}
              className="flex-1 px-6 py-3 border-2 border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium"
              disabled={loading}
            >
              Cancelar
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={loading || carrito.length === 0}
              className="flex-1 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 font-medium disabled:opacity-50"
            >
              {loading
                ? "Registrando..."
                : `Registrar Venta — Bs. ${totalVenta.toFixed(2)}`}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

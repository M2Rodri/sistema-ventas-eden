"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  X,
  AlertCircle,
  User,
  Phone,
  Search,
  Plus,
  Trash2,
  Package,
  ChevronDown,
  Upload,
} from "lucide-react";
import {
  VentaRequest,
  MetodoPago,
  ItemVentaRequest,
  ModalidadEntrega,
} from "@/types/venta";
import {
  createVentaDirecta,
  getAllProductos,
  getAllInventario,
  getAllClientes,
  adjuntarComprobantePago,
} from "@/lib/api";

// Redondea a centavos apenas se calcula un monto, para que dos totales que
// deberían coincidir (venta vs. pagado) no queden desalineados por un
// residuo de coma flotante (ej: 0.1 + 0.2 = 0.30000000000000004).
const redondear = (valor: number) => Math.round(valor * 100) / 100;

interface ProductoCarrito {
  idProducto: number;
  nombre: string;
  sku: string;
  precioOriginal: number;
  precioFinal: number;
  cantidad: number;
  stock: number;
  descuentoPorcentaje: number;
  subtotal: number;
}

interface RegistrarVentaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  userRole: "ADMIN" | "EMPLEADO";
}

export default function RegistrarVentaModal({
  isOpen,
  onClose,
  onSuccess,
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
  const [mostrarListaClientes, setMostrarListaClientes] = useState(false);
  const clienteBoxRef = useRef<HTMLDivElement>(null);

  // Productos
  const [productos, setProductos] = useState<any[]>([]);
  const [busquedaProducto, setBusquedaProducto] = useState("");
  const [tipoActivo, setTipoActivo] = useState<string | null>(null);
  const [productosFiltrados, setProductosFiltrados] = useState<any[]>([]);
  const [mostrarListaProductos, setMostrarListaProductos] = useState(false);
  const [carrito, setCarrito] = useState<ProductoCarrito[]>([]);
  const productoBoxRef = useRef<HTMLDivElement>(null);

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
  const [comprobanteFile, setComprobanteFile] = useState<File | null>(null);
  const [saldoPendienteHabilitado, setSaldoPendienteHabilitado] = useState(false);
  const [clientes, setClientes] = useState<any[]>([]);

  // Estado
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paso, setPaso] = useState(1);

  useEffect(() => {
    if (isOpen) cargarProductos();
    cargarClientes();
  }, [isOpen]);

  // Cerrar los dropdowns de cliente/producto al hacer click fuera de su caja.
  useEffect(() => {
    const handleClickFuera = (e: MouseEvent) => {
      if (
        clienteBoxRef.current &&
        !clienteBoxRef.current.contains(e.target as Node)
      ) {
        setMostrarListaClientes(false);
      }
      if (
        productoBoxRef.current &&
        !productoBoxRef.current.contains(e.target as Node)
      ) {
        setMostrarListaProductos(false);
        setTipoActivo(null);
      }
    };
    document.addEventListener("mousedown", handleClickFuera);
    return () => document.removeEventListener("mousedown", handleClickFuera);
  }, []);

  const cargarProductos = async () => {
    try {
      // El stock no vive en Producto, vive en Inventario: sin este cruce,
      // "producto.stock" queda undefined y el tope de cantidad del carrito
      // no aplica (cualquier cantidad pasaba el chequeo silenciosamente).
      const [productosData, inventarioData] = await Promise.all([
        getAllProductos(),
        getAllInventario(),
      ]);
      const stockPorProducto = new Map(
        inventarioData.map((inv) => [inv.idProducto, inv.cantidadDisponible])
      );
      const productosConStock = productosData.map((p: any) => ({
        ...p,
        stock: stockPorProducto.get(p.id) ?? 0,
      }));
      setProductos(productosConStock);
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
    // Sin quitar tildes, buscar "colchon" no encontraba "Colchón": nadie
    // escribe tildes cuando busca rápido en un mostrador.
    const normalizar = (s: string) =>
      s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
    const texto = normalizar(busquedaProducto.trim());
    if (!texto && !tipoActivo) {
      setProductosFiltrados([]);
      return;
    }
    const filtrados = productos.filter((p) => {
      const coincideTexto =
        !texto ||
        normalizar(p.nombre).includes(texto) ||
        normalizar(p.sku).includes(texto);
      const coincideTipo = !tipoActivo || p.tipoProducto === tipoActivo;
      return coincideTexto && coincideTipo;
    });
    setProductosFiltrados(filtrados);
  }, [busquedaProducto, tipoActivo, productos]);

  const toggleTipoProducto = (tipo: string) => {
    setTipoActivo((prev) => (prev === tipo ? null : tipo));
    setMostrarListaProductos(true);
  };

  const agregarAlCarrito = (producto: any) => {
    const existe = carrito.find((item) => item.idProducto === producto.id);
    if (existe) {
      if (existe.cantidad >= existe.stock) return;
      setCarrito(
        carrito.map((item) =>
          item.idProducto === producto.id
            ? {
                ...item,
                cantidad: item.cantidad + 1,
                subtotal: redondear((item.cantidad + 1) * item.precioFinal),
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
          stock: producto.stock,
          descuentoPorcentaje: 0,
          subtotal: producto.precioVenta,
        },
      ]);
    }
    setBusquedaProducto("");
    setTipoActivo(null);
    setMostrarListaProductos(false);
  };

  // Mientras se edita el campo se acepta vacío (se guarda como 0, que se
  // muestra en blanco) para poder borrar y escribir otro número; recién al
  // salir del campo (onBlur) se exige mínimo 1. El máximo nunca pasa del
  // stock disponible.
  const actualizarCantidad = (idProducto: number, valor: string) => {
    if (valor === "") {
      setCarrito(
        carrito.map((item) =>
          item.idProducto === idProducto
            ? { ...item, cantidad: 0, subtotal: 0 }
            : item,
        ),
      );
      return;
    }

    const item = carrito.find((i) => i.idProducto === idProducto);
    if (!item) return;

    let cantidad = parseInt(valor, 10);
    if (isNaN(cantidad) || cantidad < 0) return;
    if (cantidad > item.stock) cantidad = item.stock;

    setCarrito(
      carrito.map((item) =>
        item.idProducto === idProducto
          ? {
              ...item,
              cantidad,
              subtotal: redondear(cantidad * item.precioFinal),
            }
          : item,
      ),
    );
  };

  // Si el campo queda en 0 (vacío) al salir, se completa con 1 en vez de
  // dejar una cantidad inválida.
  const confirmarCantidadMinima = (idProducto: number) => {
    setCarrito((prev) =>
      prev.map((item) =>
        item.idProducto === idProducto && item.cantidad < 1
          ? { ...item, cantidad: 1, subtotal: item.precioFinal }
          : item,
      ),
    );
  };

  // El precio acordado es lo que se escribe; el % de descuento sale solo,
  // calculado a partir de la diferencia con el precio de catálogo.
  const actualizarPrecioAcordado = (idProducto: number, valor: string) => {
    if (valor === "") return; // se deja vacío mientras se escribe, sin tocar el estado

    const item = carrito.find((i) => i.idProducto === idProducto);
    if (!item) return;

    const precioFinal = parseFloat(valor);
    if (isNaN(precioFinal) || precioFinal < 0) return;

    const descuentoPorcentaje =
      item.precioOriginal > 0
        ? redondear(
            ((item.precioOriginal - precioFinal) / item.precioOriginal) * 100,
          )
        : 0;

    setCarrito(
      carrito.map((i) =>
        i.idProducto === idProducto
          ? {
              ...i,
              precioFinal,
              descuentoPorcentaje,
              subtotal: redondear(i.cantidad * precioFinal),
            }
          : i,
      ),
    );
  };

  // Al salir del campo: si quedó vacío, se redibuja con el último precio
  // válido. El precio de venta nunca puede superar el catálogo: el backend
  // hoy solo acepta precios menores (para el descuento), y uno mayor lo
  // ignora en silencio y cobra igual el de catálogo. Hasta que eso se
  // resuelva del lado del backend, un precio por encima se recorta acá.
  const confirmarPrecioAcordado = (idProducto: number) => {
    setCarrito((prev) =>
      prev.map((i) =>
        i.idProducto === idProducto && i.precioFinal > i.precioOriginal
          ? {
              ...i,
              precioFinal: i.precioOriginal,
              descuentoPorcentaje: 0,
              subtotal: redondear(i.cantidad * i.precioOriginal),
            }
          : { ...i },
      ),
    );
  };

  const eliminarDelCarrito = (idProducto: number) => {
    setCarrito(carrito.filter((item) => item.idProducto !== idProducto));
  };

  // La rueda del mouse cambia el valor de un input numérico enfocado, aunque
  // la intención sea solo desplazar el modal. Se le quita el foco antes de
  // que el navegador aplique el scroll.
  const evitarCambioPorRueda = (e: React.WheelEvent<HTMLInputElement>) => {
    e.currentTarget.blur();
  };

  const totalVenta = redondear(
    carrito.reduce((acc, item) => acc + item.subtotal, 0),
  );
  const totalOriginal = redondear(
    carrito.reduce(
      (acc, item) => acc + item.precioOriginal * item.cantidad,
      0,
    ),
  );
  const totalDescuento = redondear(totalOriginal - totalVenta);

  // Por defecto se cobra el total. Mientras no se habilite el saldo
  // pendiente, "Monto a cobrar hoy" queda fijo en el total (no se puede
  // reducir): dejar algo a deber es una decisión explícita, no un
  // descuido de tipeo.
  useEffect(() => {
    if (!saldoPendienteHabilitado && pagos.length === 1) {
      setPagos([{ ...pagos[0], monto: totalVenta }]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [totalVenta, saldoPendienteHabilitado]);

  const montoPagado = redondear(pagos.reduce((acc, p) => acc + p.monto, 0));

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

    if (carrito.some((item) => item.cantidad < 1)) {
      setError("Hay un producto con cantidad inválida");
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

      const creada = await createVentaDirecta(request);

      // El comprobante viaja aparte: la venta ya quedó registrada aunque
      // esto falle. Si no hay pago (venta enteramente a crédito) no hay a
      // qué pago adjuntarlo todavía; se hace después desde el detalle.
      if (comprobanteFile && creada.pagos && creada.pagos.length > 0) {
        try {
          await adjuntarComprobantePago(creada.pagos[0].id, comprobanteFile);
        } catch (errComprobante: any) {
          alert(
            "La venta se registró, pero no se pudo subir el comprobante: " +
              (errComprobante.message || "error desconocido") +
              ". Podés adjuntarlo después desde el detalle de la venta.",
          );
        }
      }

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
    setTipoActivo(null);
    setMostrarListaProductos(false);
    setMostrarListaClientes(false);
    setComprobanteFile(null);
    setSaldoPendienteHabilitado(false);
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
        <div className="flex justify-between items-center p-6 border-b sticky top-0 bg-gradient-to-r from-primary-50 to-primary-100 z-10">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">
              Registrar Nueva Venta
            </h2>
            <p className="text-sm text-gray-600 mt-1">Elegí los productos y el cliente para generar la venta</p>
          </div>
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
            <div className="relative mb-3" ref={clienteBoxRef}>
              <div className="flex items-center border border-gray-300 rounded-lg px-4 py-2 gap-2">
                <Search size={18} className="text-gray-400" />
                <input
                  type="text"
                  value={busquedaCliente}
                  onChange={(e) => {
                    setBusquedaCliente(e.target.value);
                    setClienteSeleccionado(null);
                    setMostrarListaClientes(true);
                  }}
                  onFocus={() => setMostrarListaClientes(true)}
                  className="flex-1 outline-none text-sm"
                  placeholder="Buscar cliente por nombre o celular..."
                />
                {clienteSeleccionado && (
                  <button
                    onClick={() => {
                      setClienteSeleccionado(null);
                      setBusquedaCliente("");
                      setMostrarListaClientes(false);
                    }}
                  >
                    <X size={16} className="text-gray-400" />
                  </button>
                )}
                {!clienteSeleccionado && (
                  <button
                    type="button"
                    onClick={() => setMostrarListaClientes((v) => !v)}
                    className="flex items-center justify-center text-gray-400 hover:text-gray-600 border-l border-gray-200 pl-2"
                    title="Ver todos los clientes"
                  >
                    <ChevronDown size={16} />
                  </button>
                )}
              </div>

              {/* Resultados búsqueda */}
              {mostrarListaClientes && !clienteSeleccionado && (
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
                          setMostrarListaClientes(false);
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

            {/* Buscador + filtro por tipo, en la misma fila */}
            <div className="relative mb-4" ref={productoBoxRef}>
              <div className="grid grid-cols-[2fr_1fr_1fr_1fr_1fr] items-stretch gap-2">
                <div className="flex items-center border border-gray-300 rounded-lg px-3 py-1.5 gap-1.5">
                  <Search size={14} className="text-gray-400 flex-shrink-0" />
                  <input
                    type="text"
                    value={busquedaProducto}
                    onChange={(e) => {
                      setBusquedaProducto(e.target.value);
                      setMostrarListaProductos(true);
                    }}
                    onFocus={() => setMostrarListaProductos(true)}
                    className="w-full min-w-0 outline-none text-xs"
                    placeholder="Buscar..."
                  />
                </div>
                {[
                  { valor: "CAMA", etiqueta: "Camas" },
                  { valor: "COLCHON", etiqueta: "Colchones" },
                  { valor: "ALMOHADA", etiqueta: "Almohadas" },
                  { valor: "ACCESORIO", etiqueta: "Accesorios" },
                ].map((tipo) => (
                  <button
                    key={tipo.valor}
                    type="button"
                    onClick={() => toggleTipoProducto(tipo.valor)}
                    className={`w-full whitespace-nowrap border rounded-lg px-2 py-1.5 text-[11px] font-medium select-none text-center ${
                      tipoActivo === tipo.valor
                        ? "border-blue-500 bg-blue-50 text-blue-700"
                        : "border-gray-300 text-gray-600"
                    }`}
                  >
                    {tipo.etiqueta}
                  </button>
                ))}
              </div>
              {mostrarListaProductos && (busquedaProducto.trim() || tipoActivo) && (
                <div className="absolute top-full left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg z-20 max-h-48 overflow-y-auto">
                  {productosFiltrados.length > 0 ? (
                    productosFiltrados.map((p) => (
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
                    ))
                  ) : (
                    <p className="text-sm text-gray-400 px-4 py-3">
                      No se encontraron productos.
                    </p>
                  )}
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
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                      <div>
                        <label className="text-xs text-gray-500 block mb-1">
                          Cantidad
                        </label>
                        <input
                          type="number"
                          min={1}
                          max={item.stock}
                          value={item.cantidad === 0 ? "" : item.cantidad}
                          onChange={(e) =>
                            actualizarCantidad(item.idProducto, e.target.value)
                          }
                          onBlur={() =>
                            confirmarCantidadMinima(item.idProducto)
                          }
                          onWheel={evitarCambioPorRueda}
                          className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
                        />
                        <p className="text-[11px] text-gray-400 mt-0.5">
                          Stock: {item.stock}
                        </p>
                      </div>
                      <div>
                        <label className="text-xs text-gray-500 block mb-1">
                          Precio
                        </label>
                        <div className="flex items-center gap-2 flex-wrap">
                          {item.descuentoPorcentaje > 0 && (
                            <span className="text-xs text-gray-400 line-through">
                              Bs. {item.precioOriginal.toFixed(2)}
                            </span>
                          )}
                          <input
                            type="number"
                            min={0}
                            value={item.precioFinal}
                            onChange={(e) =>
                              actualizarPrecioAcordado(
                                item.idProducto,
                                e.target.value,
                              )
                            }
                            onBlur={() =>
                              confirmarPrecioAcordado(item.idProducto)
                            }
                            onWheel={evitarCambioPorRueda}
                            className="w-24 border border-gray-300 rounded px-2 py-1 text-sm"
                          />
                          {item.descuentoPorcentaje > 0 && (
                            <span className="text-xs font-medium text-orange-600">
                              -{item.descuentoPorcentaje.toFixed(2)}%
                            </span>
                          )}
                        </div>
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
                <div
                  className={`grid grid-cols-1 gap-3 items-end ${
                    index === 0 ? "md:grid-cols-3" : "md:grid-cols-2"
                  }`}
                >
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
                      Monto a cobrar hoy (Bs.)
                    </label>
                    <input
                      type="number"
                      min={0}
                      value={pago.monto === 0 ? "" : pago.monto}
                      onChange={(e) => {
                        const valor = e.target.value;
                        const montoIngresado =
                          valor === "" ? 0 : redondear(parseFloat(valor) || 0);
                        // No puede cobrarse (entre todos los métodos) más
                        // que el total de la venta.
                        const sumaOtrasFilas = pagos.reduce(
                          (acc, p, i) => (i === index ? acc : acc + p.monto),
                          0,
                        );
                        const maximoPermitido = Math.max(
                          0,
                          redondear(totalVenta - sumaOtrasFilas),
                        );
                        const nuevos = [...pagos];
                        nuevos[index].monto = Math.min(
                          montoIngresado,
                          maximoPermitido,
                        );
                        setPagos(nuevos);
                      }}
                      onWheel={evitarCambioPorRueda}
                      disabled={index === 0 && !saldoPendienteHabilitado}
                      className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm disabled:bg-gray-100 disabled:text-gray-500"
                    />
                  </div>
                  {index === 0 && !saldoPendienteHabilitado && (
                    <div className="flex items-end">
                      <button
                        type="button"
                        onClick={() => setSaldoPendienteHabilitado(true)}
                        className="text-sm text-blue-600 hover:text-blue-800 font-medium underline underline-offset-2"
                      >
                        + Habilitar saldo pendiente
                      </button>
                    </div>
                  )}
                  {index === 0 && saldoPendienteHabilitado && (
                    <div>
                      <label className="text-xs text-gray-500 flex items-center justify-between mb-1">
                        Saldo pendiente (Bs.)
                        <button
                          type="button"
                          onClick={() => {
                            setSaldoPendienteHabilitado(false);
                            const nuevos = [...pagos];
                            nuevos[0] = { ...nuevos[0], monto: totalVenta };
                            setPagos(nuevos);
                          }}
                          className="text-red-500 hover:text-red-700 text-[11px] font-normal"
                        >
                          Quitar
                        </button>
                      </label>
                      <input
                        type="number"
                        min={0}
                        max={totalVenta}
                        value={
                          totalVenta - montoPagado === 0
                            ? ""
                            : redondear(totalVenta - montoPagado)
                        }
                        onChange={(e) => {
                          const valor = e.target.value;
                          if (valor === "") {
                            const nuevos = [...pagos];
                            nuevos[0] = { ...nuevos[0], monto: totalVenta };
                            setPagos(nuevos);
                            return;
                          }
                          const saldo = parseFloat(valor);
                          if (isNaN(saldo) || saldo < 0) return;
                          const saldoAjustado = Math.min(saldo, totalVenta);
                          const nuevos = [...pagos];
                          nuevos[0] = {
                            ...nuevos[0],
                            monto: redondear(totalVenta - saldoAjustado),
                          };
                          setPagos(nuevos);
                        }}
                        onWheel={evitarCambioPorRueda}
                        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                      />
                    </div>
                  )}
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

            {/* Comprobante: solo tiene sentido para QR/transferencia. Es
                opcional siempre, y también se puede adjuntar después desde
                el detalle de la venta si acá no se sube. */}
            {pagos[0].metodo !== MetodoPago.EFECTIVO && (
              <div className="mb-3 p-3 border border-dashed border-gray-300 rounded-lg">
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <Upload size={16} className="text-gray-500" />
                  {comprobanteFile
                    ? `Comprobante: ${comprobanteFile.name}`
                    : "Adjuntar foto del comprobante (opcional)"}
                  <input
                    type="file"
                    accept=".jpg,.jpeg,.png,.webp"
                    className="hidden"
                    onChange={(e) =>
                      setComprobanteFile(e.target.files?.[0] ?? null)
                    }
                  />
                </label>
                {comprobanteFile && (
                  <button
                    type="button"
                    onClick={() => setComprobanteFile(null)}
                    className="text-xs text-red-500 hover:text-red-700 mt-1"
                  >
                    Quitar
                  </button>
                )}
                <p className="text-xs text-gray-400 mt-1">
                  Si no la adjuntás ahora, se puede subir después desde el
                  detalle de la venta.
                </p>
              </div>
            )}

            <button
              type="button"
              onClick={() =>
                setPagos([
                  ...pagos,
                  { metodo: MetodoPago.EFECTIVO, monto: 0, referencia: "" },
                ])
              }
              className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-800 font-medium mb-3"
            >
              <Plus size={16} /> Agregar otro método de pago
            </button>

            {/* Resumen pagos */}
            <div className="p-3 bg-gray-50 rounded-lg">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Total venta:</span>
                <span className="font-semibold">
                  Bs. {totalVenta.toFixed(2)}
                </span>
              </div>
              {totalDescuento > 0 && (
                <div className="flex justify-between text-sm mt-1">
                  <span className="text-gray-600">Descuento:</span>
                  <span className="font-semibold">
                    Bs. {totalDescuento.toFixed(2)}
                  </span>
                </div>
              )}
              <div className="flex justify-between text-sm mt-1">
                <span className="text-gray-600">Total pagado:</span>
                <span className="font-semibold">
                  Bs. {montoPagado.toFixed(2)}
                </span>
              </div>
              {saldoPendienteHabilitado && (
                <div className="flex justify-between text-sm mt-1">
                  <span className="text-gray-600">Saldo pendiente:</span>
                  <span
                    className={`font-semibold ${totalVenta - montoPagado > 0 ? "text-red-600" : "text-green-600"}`}
                  >
                    Bs. {redondear(totalVenta - montoPagado).toFixed(2)}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* SECCIÓN 4 — ENTREGA */}
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Venta
            </h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Modalidad
              </label>
              <select
                value={modalidadEntrega}
                onChange={(e) => {
                  const nuevaModalidad = e.target.value as ModalidadEntrega;
                  setModalidadEntrega(nuevaModalidad);
                  // Sugerencia, no un valor fijo: el negocio opera en Santa
                  // Cruz, pero sigue siendo editable (por ejemplo, para
                  // probar el sistema desde otra ciudad). Solo completa si
                  // el campo todavía está vacío, para no pisar lo que ya
                  // se haya escrito.
                  if (nuevaModalidad === ModalidadEntrega.DOMICILIO && !ciudad.trim()) {
                    setCiudad("Santa Cruz de la Sierra");
                  }
                }}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value={ModalidadEntrega.RETIRO}>
                  En tienda
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
              {loading ? "Registrando..." : "Registrar Venta"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

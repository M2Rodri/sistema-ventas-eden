"use client";

import { useState } from "react";
import { X, AlertCircle, Banknote } from "lucide-react";
import { registrarPago } from "@/lib/api";
import { PagoRequest, MetodoPago } from "@/types/pago";
import { Venta } from "@/types/venta";

interface CobrarSaldoModalProps {
  venta: Venta;
  onClose: () => void;
  onSuccess: () => void;
}

export default function CobrarSaldoModal({
  venta,
  onClose,
  onSuccess,
}: CobrarSaldoModalProps) {
  const saldoPendiente = venta.saldoPendiente ?? 0;

  const [metodoPago, setMetodoPago] = useState<MetodoPago>("EFECTIVO");
  const [monto, setMonto] = useState<number>(saldoPendiente);
  const [referencia, setReferencia] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("es-BO", { style: "currency", currency: "BOB" }).format(
      value,
    );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (monto <= 0) {
      setError("El monto debe ser mayor a 0");
      return;
    }
    if (monto > saldoPendiente) {
      setError(
        `El monto no puede superar el saldo pendiente (${formatCurrency(saldoPendiente)})`,
      );
      return;
    }

    const request: PagoRequest = {
      idVenta: venta.id,
      monto,
      metodoPago,
      referencia: referencia.trim() || undefined,
    };

    setLoading(true);
    try {
      await registrarPago(request);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || "Error al registrar el cobro");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-gradient-to-r from-primary-50 to-primary-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center">
              <Banknote className="text-green-600" size={20} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-gray-900">
                Cobrar saldo pendiente
              </h2>
              <p className="text-xs text-gray-500">Venta #{venta.id}</p>
            </div>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X size={22} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-start gap-2">
              <AlertCircle className="text-red-600 flex-shrink-0" size={18} />
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          <div className="bg-gray-50 rounded-lg p-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">Cliente:</span>
              <span className="font-medium">{venta.nombreCliente}</span>
            </div>
            <div className="flex justify-between mt-1">
              <span className="text-gray-600">Saldo pendiente:</span>
              <span className="font-semibold text-red-600">
                {formatCurrency(saldoPendiente)}
              </span>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Método de pago
            </label>
            <select
              value={metodoPago}
              onChange={(e) => setMetodoPago(e.target.value as MetodoPago)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
            >
              <option value="EFECTIVO">💵 Efectivo</option>
              <option value="TRANSFERENCIA">🏦 Transferencia</option>
              <option value="QR">📱 QR</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Monto a cobrar (Bs.)
            </label>
            <input
              type="number"
              min={0.01}
              max={saldoPendiente}
              step="0.01"
              value={monto}
              onChange={(e) => setMonto(parseFloat(e.target.value) || 0)}
              onWheel={(e) => e.currentTarget.blur()}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <p className="text-xs text-gray-500 mt-1">
              Por defecto es el saldo completo; se puede reducir para otro pago
              parcial.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Referencia (opcional)
            </label>
            <input
              type="text"
              value={referencia}
              onChange={(e) => setReferencia(e.target.value)}
              maxLength={100}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Ej: TRX-12345"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 font-medium"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 font-medium disabled:opacity-50"
            >
              {loading ? "Registrando..." : "Confirmar cobro"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

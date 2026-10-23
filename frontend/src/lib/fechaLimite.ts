// Fecha límite del pago pendiente: texto "yyyy-MM-dd", como la manda y recibe el servidor (fechaLimitePago).

const dosDigitos = (n: number): string => String(n).padStart(2, '0');

/** Hoy más N días, como yyyy-MM-dd (solo para mostrar "vence aprox."; el servidor calcula la real). */
export const sumarDiasISO = (dias: number): string => {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`;
};

/** yyyy-MM-dd → dd/MM/yyyy. Vacío si no hay fecha. */
export const formatearFechaLimite = (iso?: string | null): string => {
  if (!iso) return '';
  const [anio, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${anio}`;
};

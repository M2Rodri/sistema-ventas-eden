// Fecha límite del pago pendiente de una venta: fechas "yyyy-MM-dd" (las mismas que
// manda y recibe el servidor en `fechaLimitePago`). Se manejan como texto para que la
// zona horaria nunca corra la fecha un día.
//
// Si una fecha ya venció NO se calcula aquí: lo decide el servidor con su reloj y llega
// en `fechaLimiteVencida`, así un reloj mal puesto en el navegador no cambia nada.

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

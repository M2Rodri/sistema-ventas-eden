'use client';

import { useEffect } from 'react';

/** En celular las tablas se muestran como tarjetas (una por fila, con "Etiqueta: valor"). */
function prepararTabla(tabla: HTMLTableElement) {
  if (tabla.closest('.area-impresion')) return;
  const cabeceras = Array.from(tabla.querySelectorAll('thead th')).map((th) =>
    (th.textContent ?? '').trim(),
  );
  if (cabeceras.length === 0) return;
  tabla.setAttribute('data-tarjetas', '');
  tabla.querySelectorAll('tbody > tr').forEach((fila) => {
    let columna = 0;
    Array.from(fila.children).forEach((celda) => {
      if (!(celda instanceof HTMLTableCellElement)) return;
      const ocupa = celda.colSpan || 1;
      const etiqueta = ocupa === 1 ? (cabeceras[columna] ?? '') : '';
      if (celda.getAttribute('data-label') !== etiqueta) {
        celda.setAttribute('data-label', etiqueta);
      }
      columna += ocupa;
    });
  });
}

export default function TablasResponsivas() {
  useEffect(() => {
    let pendiente: ReturnType<typeof setTimeout> | null = null;
    const revisar = () => {
      pendiente = null;
      document.querySelectorAll('table').forEach((t) => prepararTabla(t as HTMLTableElement));
    };
    const programar = () => {
      if (pendiente === null) pendiente = setTimeout(revisar, 30);
    };
    revisar();
    const observador = new MutationObserver(programar);
    observador.observe(document.body, { childList: true, subtree: true });
    return () => {
      observador.disconnect();
      if (pendiente !== null) clearTimeout(pendiente);
    };
  }, []);

  return null;
}

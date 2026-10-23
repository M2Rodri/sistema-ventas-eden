import { useEffect, useRef, useState, type DependencyList, type DragEvent, type MouseEvent, type PointerEvent } from 'react';

/**
 * Arrastrar una tabla desde su <thead> como si fuera una barra de scroll horizontal; solo se activa cuando la tabla desborda.
 * El estado del arrastre vive en un ref y stopDragging es el único lugar que lo apaga.
 * Uso: const { scrollContainerRef, tableRef, theadRef, hasOverflow, theadProps } = useDragScrollTable([listaFiltrada]);
 */
export function useDragScrollTable(deps: DependencyList = []) {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const tableRef = useRef<HTMLTableElement>(null);
  const theadRef = useRef<HTMLTableSectionElement>(null);
  const dragStateRef = useRef({ isDragging: false, startX: 0, scrollLeftStart: 0, moved: 0 });
  const [hasOverflow, setHasOverflow] = useState(false);

  useEffect(() => {
    const checkOverflow = () => {
      const el = scrollContainerRef.current;
      if (!el) return;
      const overflowing = el.scrollWidth > el.clientWidth;
      setHasOverflow(overflowing);
      // Si el desborde desaparece a mitad del arrastre (cambia el zoom), se corta.
      if (!overflowing && dragStateRef.current.isDragging) {
        dragStateRef.current.isDragging = false;
        if (theadRef.current) theadRef.current.style.cursor = '';
      }
    };

    checkOverflow();

    const ro = new ResizeObserver(checkOverflow);
    if (scrollContainerRef.current) ro.observe(scrollContainerRef.current);
    if (tableRef.current) ro.observe(tableRef.current);

    return () => ro.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    const stopDragging = (pointerId?: number) => {
      if (!dragStateRef.current.isDragging) return;
      dragStateRef.current.isDragging = false;
      if (theadRef.current) {
        theadRef.current.style.cursor = '';
        if (pointerId !== undefined && theadRef.current.hasPointerCapture(pointerId)) {
          theadRef.current.releasePointerCapture(pointerId);
        }
      }
    };

    const handlePointerMove = (e: globalThis.PointerEvent) => {
      const state = dragStateRef.current;
      const el = scrollContainerRef.current;
      if (!state.isDragging || !el) return;
      const delta = e.pageX - state.startX;
      state.moved = Math.max(state.moved, Math.abs(delta));
      el.scrollLeft = state.scrollLeftStart - delta;
    };

    const handlePointerUp = (e: globalThis.PointerEvent) => stopDragging(e.pointerId);
    const handlePointerCancel = (e: globalThis.PointerEvent) => stopDragging(e.pointerId);
    const handleWindowBlurOrLeave = () => stopDragging();

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
    window.addEventListener('pointercancel', handlePointerCancel);
    document.addEventListener('mouseleave', handleWindowBlurOrLeave);
    window.addEventListener('blur', handleWindowBlurOrLeave);
    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
      window.removeEventListener('pointercancel', handlePointerCancel);
      document.removeEventListener('mouseleave', handleWindowBlurOrLeave);
      window.removeEventListener('blur', handleWindowBlurOrLeave);
    };
  }, []);

  const handleTheadPointerDown = (e: PointerEvent<HTMLTableSectionElement>) => {
    if (!hasOverflow || !scrollContainerRef.current) return;
    // Evita el arrastre nativo de selección o imagen, que se traga el pointerup.
    e.preventDefault();
    dragStateRef.current.isDragging = true;
    dragStateRef.current.startX = e.pageX;
    dragStateRef.current.scrollLeftStart = scrollContainerRef.current.scrollLeft;
    dragStateRef.current.moved = 0;
    // Con la captura, el pointerup llega a este elemento aunque el mouse salga de la ventana.
    e.currentTarget.setPointerCapture(e.pointerId);
    if (theadRef.current) theadRef.current.style.cursor = 'grabbing';
  };

  const handleTheadDragStart = (e: DragEvent) => {
    e.preventDefault();
  };

  // Tras un arrastre real (>= 5px) el clic no llega a los encabezados, para no disparar un orden por error.
  const handleTheadClickCapture = (e: MouseEvent) => {
    if (dragStateRef.current.moved >= 5) {
      e.stopPropagation();
      e.preventDefault();
    }
  };

  return {
    scrollContainerRef,
    tableRef,
    theadRef,
    hasOverflow,
    theadProps: {
      onPointerDown: handleTheadPointerDown,
      onDragStart: handleTheadDragStart,
      onClickCapture: handleTheadClickCapture,
    },
  };
}

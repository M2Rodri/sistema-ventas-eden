import { useEffect, useRef, useState, type DependencyList, type DragEvent, type MouseEvent, type PointerEvent } from 'react';

/**
 * Arrastrar una tabla desde su <thead>, como si fuera una barra de scroll
 * horizontal: se aprieta sobre el encabezado, se arrastra y la tabla se
 * mueve, sin tener que bajar hasta la barra de scroll del final.
 *
 * Se activa solo cuando la tabla realmente desborda (scrollWidth >
 * clientWidth del contenedor). Eso se recalcula con un ResizeObserver sobre
 * el contenedor y la tabla, más un chequeo inmediato cuando cambian las
 * `deps` (por ejemplo, la lista filtrada: filtrar puede hacer que el
 * desborde aparezca o desaparezca sin que cambie el tamaño de la ventana).
 *
 * El estado del arrastre vive en un ref (no en useState) para no
 * re-renderizar en cada pointermove. Usa Pointer Events con captura: el
 * pointerup de un gesto llega garantizado al thead que empezó el arrastre,
 * aunque el mouse termine fuera de la ventana. `stopDragging` es el único
 * lugar que apaga el estado (soltar, cancelar, el mouse se va de la
 * ventana, la ventana pierde el foco caen todos ahí), así nunca queda el
 * arrastre prendido por un camino de salida que no se cubrió.
 *
 * Uso:
 *   const { scrollContainerRef, tableRef, theadRef, hasOverflow, theadProps }
 *     = useDragScrollTable([listaFiltrada]);
 *   <div className="overflow-x-auto" ref={scrollContainerRef}>
 *     <table ref={tableRef}>
 *       <thead ref={theadRef} className={hasOverflow ? 'cursor-grab select-none' : ''} {...theadProps}>
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
      // Si el desborde desaparece a mitad de un arrastre (cambia el zoom,
      // por ejemplo), se corta el arrastre.
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
    // Sin esto el navegador arranca su propio arrastre de selección/imagen,
    // se traga el pointerup y el estado queda colgado.
    e.preventDefault();
    dragStateRef.current.isDragging = true;
    dragStateRef.current.startX = e.pageX;
    dragStateRef.current.scrollLeftStart = scrollContainerRef.current.scrollLeft;
    dragStateRef.current.moved = 0;
    // Con la captura, el pointerup de este gesto llega garantizado a este
    // mismo elemento, aunque el mouse termine fuera de la ventana.
    e.currentTarget.setPointerCapture(e.pointerId);
    if (theadRef.current) theadRef.current.style.cursor = 'grabbing';
  };

  const handleTheadDragStart = (e: DragEvent) => {
    e.preventDefault();
  };

  // Si hubo arrastre real (>= 5px), no dejar que el clic llegue a los
  // encabezados: así, si alguna columna ordena por clic, arrastrar no lo
  // dispara por error. Va en la fase de captura para llegar antes que el
  // onClick de la columna.
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

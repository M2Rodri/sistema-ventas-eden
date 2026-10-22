import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../data/api_client.dart';
import '../../data/api_exception.dart';
import '../../data/reporte_repository.dart';
import '../../models/ganancia_producto.dart';
import '../../theme/app_colors.dart';
import '../ventas/venta_detalle_screen.dart';

class GananciasScreen extends StatefulWidget {
  const GananciasScreen({super.key, required this.token});

  final String token;

  @override
  State<GananciasScreen> createState() => _GananciasScreenState();
}

class _GananciasScreenState extends State<GananciasScreen> {
  // Primer día desde el que el sistema permite consultar registros.
  // Se ajusta aquí cuando se confirme la fecha exacta de inicio del negocio.
  DateTime _fechaInicioRegistros = DateTime(2026, 1, 1);

  final _repository = ReporteRepository();
  final _moneda = NumberFormat.currency(locale: 'es_BO', symbol: 'Bs. ', decimalDigits: 2);
  final _fecha = DateFormat('dd/MM/yyyy');
  final _fechaHora = DateFormat('dd/MM/yyyy HH:mm');
  final _mesAnio = DateFormat('MMMM yyyy', 'es_BO');
  DateTime _inicio = DateTime.now();
  DateTime _fin = DateTime.now();
  DateTime _referencia = DateTime.now();
  bool _tieneRangoPersonalizado = false;
  List<GananciaProducto> _filas = const [];
  bool _cargando = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _seleccionarPeriodo(PeriodoGanancia.hoy);
    _cargarFechaInicio();
  }

  Future<void> _cargarFechaInicio() async {
    try {
      final fecha = await _repository.obtenerFechaInicio(widget.token);
      if (mounted) {
        final limite = fecha ?? DateTime.now();
        setState(() => _fechaInicioRegistros = DateTime(limite.year, limite.month, limite.day));
      }
    } catch (_) {
      // Se conserva el límite local de respaldo si no se puede consultar.
    }
  }

  Future<void> _cargar() async {
    setState(() { _cargando = true; _error = null; });
    try {
      final filas = await _repository.obtenerGananciasPorProducto(
        token: widget.token, inicio: _inicio, fin: _fin,
      );
      if (mounted) setState(() { _filas = filas; _cargando = false; });
    } on ApiException catch (e) {
      if (mounted) setState(() { _error = e.mensaje; _cargando = false; });
    } catch (_) {
      if (mounted) setState(() { _error = 'No se pudo cargar la ganancia.'; _cargando = false; });
    }
  }

  void _seleccionarPeriodo(PeriodoGanancia periodo) {
    _periodoActual = periodo;
    if (periodo == PeriodoGanancia.personalizado) {
      _elegirPersonalizado();
      return;
    }
    _referencia = DateTime.now();
    _aplicarRango();
    _cargar();
  }

  void _aplicarRango() {
    final dia = DateTime(_referencia.year, _referencia.month, _referencia.day);
    if (_periodoActual == PeriodoGanancia.hoy) {
      _inicio = dia;
      _fin = dia.add(const Duration(days: 1)).subtract(const Duration(milliseconds: 1));
    } else if (_periodoActual == PeriodoGanancia.semana) {
      _inicio = dia.subtract(Duration(days: dia.weekday - 1));
      _fin = _inicio.add(const Duration(days: 7)).subtract(const Duration(milliseconds: 1));
    } else {
      _inicio = DateTime(_referencia.year, _referencia.month, 1);
      _fin = DateTime(_referencia.year, _referencia.month + 1, 1).subtract(const Duration(milliseconds: 1));
    }
  }

  bool get _puedeAvanzar {
    if (_periodoActual == PeriodoGanancia.personalizado || _periodoActual == PeriodoGanancia.hoy) return false;
    final hoy = DateTime.now();
    final actual = _periodoActual == PeriodoGanancia.semana
        ? DateTime(hoy.year, hoy.month, hoy.day).subtract(Duration(days: hoy.weekday - 1))
        : DateTime(hoy.year, hoy.month, 1);
    return _inicio.isBefore(actual);
  }

  void _moverPeriodo(int meses) {
    if (_periodoActual == PeriodoGanancia.semana) {
      _referencia = _referencia.add(Duration(days: meses * 7));
    } else if (_periodoActual == PeriodoGanancia.mes) {
      _referencia = DateTime(_referencia.year, _referencia.month + meses, 1);
    }
    _aplicarRango();
    _cargar();
  }

  List<DateTime> _mesesDisponibles() {
    final hoy = DateTime.now();
    final primero = DateTime(_fechaInicioRegistros.year, _fechaInicioRegistros.month, 1);
    final ultimo = DateTime(hoy.year, hoy.month, 1);
    final meses = <DateTime>[];
    var mes = primero;
    while (!mes.isAfter(ultimo)) {
      meses.add(mes);
      mes = DateTime(mes.year, mes.month + 1, 1);
    }
    return meses.reversed.toList();
  }

  Future<void> _elegirPersonalizado() async {
    final hoy = DateTime.now();
    final fechaInicial = _tieneRangoPersonalizado &&
            !_inicio.isBefore(_fechaInicioRegistros) &&
            !_inicio.isAfter(hoy)
        ? _inicio
        : hoy;
    final fecha = await _mostrarSelectorFecha(fechaInicial, hoy);
    if (fecha == null || !mounted) return;
    setState(() {
      _inicio = DateTime(fecha.year, fecha.month, fecha.day);
      _fin = DateTime(fecha.year, fecha.month, fecha.day, 23, 59, 59, 999);
      _tieneRangoPersonalizado = true;
    });
    _cargar();
  }

  Future<DateTime?> _mostrarSelectorFecha(DateTime inicial, DateTime hoy) {
    return showDialog<DateTime>(
      context: context,
      builder: (dialogContext) {
        var seleccionada = DateTime(inicial.year, inicial.month, inicial.day);
        var mesMostrado = DateTime(seleccionada.year, seleccionada.month, 1);
        final anios = [
          for (var anio = _fechaInicioRegistros.year - 5; anio <= hoy.year + 5; anio++) anio,
        ];
        final meses = List<int>.generate(12, (indice) => indice + 1);

        DateTime ajustar(int anio, int mes, int dia) {
          final ultimoDia = DateTime(anio, mes + 1, 0).day;
          var fecha = DateTime(anio, mes, dia > ultimoDia ? ultimoDia : dia);
          if (fecha.isBefore(_fechaInicioRegistros)) fecha = _fechaInicioRegistros;
          if (fecha.isAfter(hoy)) fecha = DateTime(hoy.year, hoy.month, hoy.day);
          return fecha;
        }

        bool mesBloqueado(int anio, int mes) {
          return (anio == _fechaInicioRegistros.year && mes < _fechaInicioRegistros.month) ||
              (anio == hoy.year && mes > hoy.month);
        }

        bool anioBloqueado(int anio) {
          return anio < _fechaInicioRegistros.year || anio > hoy.year;
        }

        int diaInicialPara(int anio, int mes) {
          if (anio == hoy.year && mes == hoy.month) return hoy.day;
          if (anio == _fechaInicioRegistros.year && mes == _fechaInicioRegistros.month) {
            return _fechaInicioRegistros.day;
          }
          return 1;
        }

        List<int?> diasDelMes() {
          final primerDia = DateTime(mesMostrado.year, mesMostrado.month, 1);
          final cantidad = DateTime(mesMostrado.year, mesMostrado.month + 1, 0).day;
          final dias = <int?>[...List<int?>.filled(primerDia.weekday - 1, null)];
          dias.addAll(List<int?>.generate(cantidad, (indice) => indice + 1));
          while (dias.length % 7 != 0) dias.add(null);
          return dias;
        }

        return StatefulBuilder(
          builder: (context, actualizar) => AlertDialog(
            title: const Text('Seleccionar fecha'),
            content: SizedBox(
              width: 340,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButton<int>(
                          value: mesMostrado.month,
                          isExpanded: true,
                          items: meses.map((mes) {
                            final bloqueado = mesBloqueado(mesMostrado.year, mes);
                            return DropdownMenuItem<int>(
                              value: mes,
                              enabled: !bloqueado,
                              child: Text(
                                DateFormat('MMMM', 'es_BO').format(DateTime(mesMostrado.year, mes, 1)),
                                style: TextStyle(color: bloqueado ? AppColors.textoSecundario.withValues(alpha: 0.45) : AppColors.textoPrincipal),
                              ),
                            );
                          }).toList(),
                          onChanged: (mes) {
                            if (mes == null) return;
                            actualizar(() {
                              mesMostrado = DateTime(mesMostrado.year, mes, 1);
                              seleccionada = ajustar(mesMostrado.year, mesMostrado.month, diaInicialPara(mesMostrado.year, mesMostrado.month));
                            });
                          },
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: DropdownButton<int>(
                          value: mesMostrado.year,
                          isExpanded: true,
                          items: anios.map((anio) {
                            final bloqueado = anioBloqueado(anio);
                            return DropdownMenuItem<int>(
                              value: anio,
                              enabled: !bloqueado,
                              child: Text(
                                '$anio',
                                style: TextStyle(color: bloqueado ? AppColors.textoSecundario.withValues(alpha: 0.45) : AppColors.textoPrincipal),
                              ),
                            );
                          }).toList(),
                          onChanged: (anio) {
                            if (anio == null) return;
                            actualizar(() {
                              mesMostrado = DateTime(anio, mesMostrado.month, 1);
                              seleccionada = ajustar(mesMostrado.year, mesMostrado.month, diaInicialPara(mesMostrado.year, mesMostrado.month));
                            });
                          },
                        ),
                      ),
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      IconButton(
                        tooltip: 'Mes anterior',
                        onPressed: mesMostrado.year == _fechaInicioRegistros.year && mesMostrado.month == _fechaInicioRegistros.month
                            ? null
                            : () => actualizar(() {
                                mesMostrado = DateTime(mesMostrado.year, mesMostrado.month - 1, 1);
                                seleccionada = ajustar(mesMostrado.year, mesMostrado.month, diaInicialPara(mesMostrado.year, mesMostrado.month));
                              }),
                        icon: const Icon(Icons.chevron_left_rounded),
                      ),
                      IconButton(
                        tooltip: 'Mes siguiente',
                        onPressed: mesMostrado.year == hoy.year && mesMostrado.month == hoy.month
                            ? null
                            : () => actualizar(() {
                                mesMostrado = DateTime(mesMostrado.year, mesMostrado.month + 1, 1);
                                seleccionada = ajustar(mesMostrado.year, mesMostrado.month, diaInicialPara(mesMostrado.year, mesMostrado.month));
                              }),
                        icon: const Icon(Icons.chevron_right_rounded),
                      ),
                    ],
                  ),
                  Row(
                    children: ['L', 'M', 'M', 'J', 'V', 'S', 'D']
                        .map((dia) => Expanded(child: Center(child: Text(dia, style: TextStyle(color: AppColors.textoSecundario, fontWeight: FontWeight.bold)))))
                        .toList(),
                  ),
                  const SizedBox(height: 6),
                  GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: diasDelMes().length,
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 7),
                    itemBuilder: (_, indice) {
                      final dia = diasDelMes()[indice];
                      if (dia == null) return const SizedBox.shrink();
                      final fecha = DateTime(mesMostrado.year, mesMostrado.month, dia);
                      final bloqueado = fecha.isBefore(_fechaInicioRegistros) || fecha.isAfter(hoy);
                      final elegido = fecha.year == seleccionada.year && fecha.month == seleccionada.month && fecha.day == seleccionada.day;
                      return InkWell(
                        onTap: bloqueado ? null : () => actualizar(() => seleccionada = fecha),
                        child: Center(child: Container(
                          width: 34,
                          height: 34,
                          alignment: Alignment.center,
                          decoration: elegido ? BoxDecoration(shape: BoxShape.circle, color: AppColors.verdeOscuro) : null,
                          child: Text('$dia', style: TextStyle(color: bloqueado ? AppColors.textoSecundario.withValues(alpha: 0.45) : (elegido ? Colors.white : AppColors.textoPrincipal), fontWeight: elegido ? FontWeight.bold : FontWeight.normal)),
                        )),
                      );
                    },
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: const Text('Cancelar')),
              FilledButton(onPressed: () => Navigator.of(dialogContext).pop(seleccionada), child: const Text('Aplicar')),
            ],
          ),
        );
      },
    );
  }

  double get _ventas => _filas.fold(0, (s, f) => s + f.subtotal);
  double get _costos => _filas.fold(0, (s, f) => s + ((f.costoUnitario ?? 0) * f.cantidad));
  double get _ganancia => _filas.fold(0, (s, f) => s + (f.ganancia ?? 0));
  bool get _hayCostosFaltantes => _filas.any((f) => f.costoUnitario == null);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ganancias por ventas')),
      body: RefreshIndicator(
        color: AppColors.verdeOscuro,
        onRefresh: _cargar,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            _periodos(),
            const SizedBox(height: 14),
            _rangoPeriodo(),
            const SizedBox(height: 12),
            if (_cargando) const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()))
            else if (_error != null) _mensajeError()
            else ...[_resumen(), const SizedBox(height: 20), _lista()],
          ],
        ),
      ),
    );
  }

  Widget _periodos() => SegmentedButton<PeriodoGanancia>(
    showSelectedIcon: false,
    segments: const [
      ButtonSegment(value: PeriodoGanancia.hoy, label: Text('Hoy')),
      ButtonSegment(value: PeriodoGanancia.semana, label: Text('Semana')),
      ButtonSegment(value: PeriodoGanancia.mes, label: Text('Mes')),
      ButtonSegment(value: PeriodoGanancia.personalizado, icon: Icon(Icons.edit_calendar_outlined), label: SizedBox.shrink()),
    ],
    selected: {_periodoActual},
    onSelectionChanged: (s) => _seleccionarPeriodo(s.first),
  );

  Widget _rangoPeriodo() {
    final navegable = _periodoActual == PeriodoGanancia.semana || _periodoActual == PeriodoGanancia.mes;
    final mismoDia = _inicio.year == _fin.year &&
        _inicio.month == _fin.month &&
        _inicio.day == _fin.day;
    final textoRango = mismoDia
        ? _fecha.format(_inicio)
        : '${_fecha.format(_inicio)} - ${_fecha.format(_fin)}';
    return Row(children: [
      if (navegable) IconButton(onPressed: () => _moverPeriodo(-1), icon: const Icon(Icons.chevron_left_rounded)),
      Expanded(
        child: _periodoActual == PeriodoGanancia.mes
            ? DropdownButtonHideUnderline(
                child: DropdownButton<DateTime>(
                  value: DateTime(_inicio.year, _inicio.month, 1),
                  isExpanded: true,
                  alignment: Alignment.center,
                  items: _mesesDisponibles().map((mes) => DropdownMenuItem<DateTime>(
                    value: mes,
                    child: Center(child: Text(_mesAnio.format(mes))),
                  )).toList(),
                  onChanged: (mes) {
                    if (mes == null) return;
                    _referencia = mes;
                    _aplicarRango();
                    _cargar();
                  },
                ),
              )
            : Text(textoRango, textAlign: navegable ? TextAlign.center : TextAlign.left, style: TextStyle(color: AppColors.textoSecundario)),
      ),
      if (navegable) IconButton(onPressed: _puedeAvanzar ? () => _moverPeriodo(1) : null, icon: const Icon(Icons.chevron_right_rounded)),
      if (_periodoActual == PeriodoGanancia.personalizado) IconButton(onPressed: _elegirPersonalizado, icon: const Icon(Icons.edit_calendar_outlined)),
    ]);
  }

  PeriodoGanancia _periodoActual = PeriodoGanancia.hoy;

  Widget _resumen() => Column(children: [
    Card(color: AppColors.tintVerde, child: Padding(padding: const EdgeInsets.all(18), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Ganancia real', style: TextStyle(color: AppColors.acVerde, fontWeight: FontWeight.bold)),
      const SizedBox(height: 4), Text(_moneda.format(_ganancia), style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: AppColors.textoPrincipal)),
    ]))),
    const SizedBox(height: 10),
    Row(children: [Expanded(child: _dato('Costo total', _costos)), const SizedBox(width: 10), Expanded(child: _dato('Ventas totales', _ventas))]),
    if (_hayCostosFaltantes) ...[
      const SizedBox(height: 8),
      Text('Algunas líneas no tienen costo histórico; su ganancia se muestra como no disponible.', style: TextStyle(color: AppColors.acAmbar, fontSize: 12)),
    ],
  ]);

  Widget _dato(String titulo, double valor) => Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(titulo, style: TextStyle(color: AppColors.textoSecundario)), const SizedBox(height: 4), Text(_moneda.format(valor), style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.textoPrincipal))])));

  Widget _lista() => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
    Text('Detalle por producto', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.textoPrincipal)),
    const SizedBox(height: 10),
    if (_filas.isEmpty) Card(child: Padding(padding: const EdgeInsets.all(20), child: Center(child: Text('No hay ventas completadas en este período.', style: TextStyle(color: AppColors.textoSecundario)))))
    else ..._filas.map(_fila),
  ]);

  Future<void> _abrirVenta(int idVenta) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => VentaDetalleScreen(
          idVenta: idVenta,
          token: widget.token,
          esAdmin: true,
        ),
      ),
    );
  }

  Widget _fila(GananciaProducto fila) => Card(margin: const EdgeInsets.only(bottom: 10), child: ExpansionTile(
    leading: CircleAvatar(backgroundColor: AppColors.tintAguamarina, child: Icon(Icons.trending_up_rounded, color: AppColors.acAguamarina)),
    title: Text(fila.nombreProducto, style: const TextStyle(fontWeight: FontWeight.bold)),
    subtitle: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(fila.fechaVenta == null ? 'Fecha no disponible' : _fechaHora.format(fila.fechaVenta!)),
        FittedBox(
          fit: BoxFit.scaleDown,
          alignment: Alignment.centerLeft,
          child: Text('${fila.cantidad} unidad(es) · Venta ${_moneda.format(fila.subtotal)}'),
        ),
      ],
    ),
    trailing: Text(_moneda.format(fila.ganancia ?? 0), style: TextStyle(fontWeight: FontWeight.bold, color: (fila.ganancia ?? 0) >= 0 ? AppColors.acVerde : AppColors.error)),
    children: [Padding(padding: const EdgeInsets.fromLTRB(72, 0, 16, 16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('Cantidad: ${fila.cantidad}'),
      Text('Costo unitario: ${fila.costoUnitario == null ? 'No disponible' : _moneda.format(fila.costoUnitario)}'),
      if (fila.cantidad > 1)
        Text('Costo total: ${fila.costoUnitario == null ? 'No disponible' : _moneda.format(fila.costoUnitario! * fila.cantidad)}'),
      const SizedBox(height: 4),
      Text('Precio de venta unitario: ${_moneda.format(fila.precioUnitario)}'),
      if (fila.cantidad > 1) Text('Venta total: ${_moneda.format(fila.subtotal)}'),
      const SizedBox(height: 4),
      Text('Ganancia total: ${fila.ganancia == null ? 'No disponible' : _moneda.format(fila.ganancia)}', style: const TextStyle(fontWeight: FontWeight.bold)),
      if (fila.costoUnitario != null) Text('(${_moneda.format(fila.precioUnitario)} - ${_moneda.format(fila.costoUnitario)}) × ${fila.cantidad} = ${_moneda.format(fila.ganancia ?? 0)}', style: TextStyle(color: AppColors.textoSecundario, fontSize: 12)),
      if (fila.fechaVenta != null) Text('Fecha de venta: ${_fechaHora.format(fila.fechaVenta!)}'),
      const SizedBox(height: 12),
      OutlinedButton.icon(
        onPressed: () => _abrirVenta(fila.idVenta),
        icon: const Icon(Icons.receipt_long_outlined, size: 18),
        label: Text('Ver venta #${fila.idVenta}'),
      ),
    ]))],
  ));

  Widget _mensajeError() => Card(color: AppColors.errorFondo, child: Padding(padding: const EdgeInsets.all(18), child: Column(children: [Text(_error!, textAlign: TextAlign.center), const SizedBox(height: 10), OutlinedButton(onPressed: _cargar, child: const Text('Reintentar'))])));
}

enum PeriodoGanancia { hoy, semana, mes, personalizado }

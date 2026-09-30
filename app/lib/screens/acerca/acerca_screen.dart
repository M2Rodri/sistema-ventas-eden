import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../../theme/app_colors.dart';

/// Acerca de la app: nombre, versión y qué es. Nada más, es para el dueño.
class AcercaScreen extends StatelessWidget {
  const AcercaScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Acerca de la app')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Container(
                width: 88,
                height: 88,
                decoration: BoxDecoration(
                  color: AppColors.fondoResumen,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: AppColors.verdeOscuro.withValues(alpha: 0.12)),
                ),
                child: const Icon(Icons.bed_outlined, color: AppColors.verdeOscuro, size: 44),
              ),
              const SizedBox(height: 20),
              const Text(
                'Mueblería Edén',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.textoPrincipal),
              ),
              const SizedBox(height: 4),
              FutureBuilder<PackageInfo>(
                future: PackageInfo.fromPlatform(),
                builder: (context, snapshot) {
                  final info = snapshot.data;
                  return Text(
                    info == null ? ' ' : 'Versión ${info.version}',
                    style: const TextStyle(fontSize: 13.5, color: AppColors.textoSecundario),
                  );
                },
              ),
              const SizedBox(height: 24),
              const Text(
                'Sistema de ventas para registrar ventas, cobros y entregas.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, height: 1.4, color: AppColors.textoPrincipal),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

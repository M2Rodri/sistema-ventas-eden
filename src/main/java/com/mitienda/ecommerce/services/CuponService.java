package com.mitienda.ecommerce.services;

import com.mitienda.ecommerce.dto.CuponRequest;
import com.mitienda.ecommerce.dto.CuponResponse;
import com.mitienda.ecommerce.dto.ValidarCuponRequest;
import com.mitienda.ecommerce.dto.ValidarCuponResponse;
import com.mitienda.ecommerce.models.Cupon;
import com.mitienda.ecommerce.models.TipoCupon;
import com.mitienda.ecommerce.repositories.CuponRepository;
import com.mitienda.ecommerce.repositories.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de cupones
 */
@Service
public class CuponService {

    @Autowired
    private CuponRepository cuponRepository;

    @Autowired
    private VentaRepository ventaRepository;

    /**
     * Listar todos los cupones
     */
    public List<CuponResponse> getAllCupones() {
        return cuponRepository.findAll()
                .stream()
                .map(CuponResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar cupones activos
     */
    public List<CuponResponse> getActiveCupones() {
        return cuponRepository.findByActivoTrue()
                .stream()
                .map(CuponResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar cupones vigentes
     */
    public List<CuponResponse> getCuponesVigentes() {
        return cuponRepository.findCuponesVigentes(LocalDate.now())
                .stream()
                .map(CuponResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtener cupón por ID
     */
    public CuponResponse getCuponById(Long id) {
        Cupon cupon = cuponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con ID: " + id));
        return new CuponResponse(cupon);
    }

    /**
     * Obtener cupón por código
     */
    public CuponResponse getCuponByCodigo(String codigo) {
        Cupon cupon = cuponRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con código: " + codigo));
        return new CuponResponse(cupon);
    }

    /**
     * Crear nuevo cupón
     */
    @Transactional
    public CuponResponse createCupon(CuponRequest request) {
        // Validar que el código no exista
        if (cuponRepository.existsByCodigo(request.getCodigo().toUpperCase())) {
            throw new RuntimeException("Ya existe un cupón con el código: " + request.getCodigo());
        }

        // Validar fechas
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        Cupon cupon = new Cupon();
        cupon.setCodigo(request.getCodigo().toUpperCase());
        cupon.setDescripcion(request.getDescripcion());
        cupon.setTipoCupon(request.getTipoCupon());
        cupon.setValorDescuento(request.getValorDescuento());
        cupon.setMontoMinimo(request.getMontoMinimo());
        cupon.setDescuentoMaximo(request.getDescuentoMaximo());
        cupon.setFechaInicio(request.getFechaInicio());
        cupon.setFechaFin(request.getFechaFin());
        cupon.setUsoMaximo(request.getUsoMaximo());
        cupon.setActivo(request.getActivo());
        cupon.setPrimeraCompra(request.getPrimeraCompra());
        cupon.setVecesUsado(0);

        Cupon savedCupon = cuponRepository.save(cupon);
        return new CuponResponse(savedCupon);
    }

    /**
     * Actualizar cupón existente
     */
    @Transactional
    public CuponResponse updateCupon(Long id, CuponRequest request) {
        Cupon cupon = cuponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con ID: " + id));

        // Validar código único (si cambió)
        if (!cupon.getCodigo().equals(request.getCodigo().toUpperCase()) && 
            cuponRepository.existsByCodigo(request.getCodigo().toUpperCase())) {
            throw new RuntimeException("Ya existe un cupón con el código: " + request.getCodigo());
        }

        // Validar fechas
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        cupon.setCodigo(request.getCodigo().toUpperCase());
        cupon.setDescripcion(request.getDescripcion());
        cupon.setTipoCupon(request.getTipoCupon());
        cupon.setValorDescuento(request.getValorDescuento());
        cupon.setMontoMinimo(request.getMontoMinimo());
        cupon.setDescuentoMaximo(request.getDescuentoMaximo());
        cupon.setFechaInicio(request.getFechaInicio());
        cupon.setFechaFin(request.getFechaFin());
        cupon.setUsoMaximo(request.getUsoMaximo());
        cupon.setActivo(request.getActivo());
        cupon.setPrimeraCompra(request.getPrimeraCompra());

        Cupon updatedCupon = cuponRepository.save(cupon);
        return new CuponResponse(updatedCupon);
    }

    /**
     * Eliminar cupón (desactivar)
     */
    @Transactional
    public void deleteCupon(Long id) {
        Cupon cupon = cuponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con ID: " + id));

        cupon.setActivo(false);
        cuponRepository.save(cupon);
    }

    /**
     * Activar/Desactivar cupón
     */
    @Transactional
    public CuponResponse toggleCuponStatus(Long id) {
        Cupon cupon = cuponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con ID: " + id));

        cupon.setActivo(!cupon.getActivo());
        Cupon updatedCupon = cuponRepository.save(cupon);
        return new CuponResponse(updatedCupon);
    }

    /**
     * Validar y aplicar cupón
     */
    public ValidarCuponResponse validarCupon(ValidarCuponRequest request) {
        // Buscar cupón
        Cupon cupon = cuponRepository.findByCodigo(request.getCodigoCupon().toUpperCase())
                .orElse(null);

        if (cupon == null) {
            return new ValidarCuponResponse(false, "Cupón no encontrado", BigDecimal.ZERO, request.getMontoCompra(), false);
        }

        // Validar si está vigente
        if (!cupon.estaVigente()) {
            String mensaje = !cupon.getActivo() ? "Cupón inactivo" :
                           LocalDate.now().isBefore(cupon.getFechaInicio()) ? "Cupón aún no válido" :
                           LocalDate.now().isAfter(cupon.getFechaFin()) ? "Cupón expirado" :
                           "Cupón agotado";
            return new ValidarCuponResponse(false, mensaje, BigDecimal.ZERO, request.getMontoCompra(), false);
        }

        // Validar monto mínimo
        if (request.getMontoCompra().compareTo(cupon.getMontoMinimo()) < 0) {
            return new ValidarCuponResponse(false, 
                "Compra mínima requerida: " + cupon.getMontoMinimo(), 
                BigDecimal.ZERO, request.getMontoCompra(), false);
        }

        // Validar si es para primera compra
        if (cupon.getPrimeraCompra() && request.getIdCliente() != null) {
            long comprasAnteriores = ventaRepository.findByClienteIdOrderByFechaVentaDesc(request.getIdCliente()).size();
            if (comprasAnteriores > 0) {
                return new ValidarCuponResponse(false, "Cupón solo válido para primera compra", 
                    BigDecimal.ZERO, request.getMontoCompra(), false);
            }
        }

        // Calcular descuento
        BigDecimal descuento = cupon.calcularDescuento(request.getMontoCompra());
        BigDecimal montoFinal = request.getMontoCompra().subtract(descuento);
        Boolean envioGratis = cupon.getTipoCupon() == TipoCupon.ENVIO_GRATIS;

        return new ValidarCuponResponse(true, "Cupón aplicado correctamente", descuento, montoFinal, envioGratis);
    }

    /**
     * Aplicar cupón (incrementar contador de uso)
     */
    @Transactional
    public void aplicarCupon(String codigoCupon) {
        Cupon cupon = cuponRepository.findByCodigo(codigoCupon.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado con código: " + codigoCupon));

        if (!cupon.estaVigente()) {
            throw new RuntimeException("El cupón no está vigente");
        }

        cupon.incrementarUso();
        cuponRepository.save(cupon);
    }

    /**
     * Contar cupones activos
     */
    public Long countActiveCupones() {
        return cuponRepository.countByActivo(true);
    }
}
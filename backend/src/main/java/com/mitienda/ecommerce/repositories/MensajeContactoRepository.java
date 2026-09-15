package com.mitienda.ecommerce.repositories;

import com.mitienda.ecommerce.models.MensajeContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {

    List<MensajeContacto> findAllByOrderByFechaEnvioDesc();

    List<MensajeContacto> findByAtendidoOrderByFechaEnvioDesc(Boolean atendido);

    Long countByAtendido(Boolean atendido);
}

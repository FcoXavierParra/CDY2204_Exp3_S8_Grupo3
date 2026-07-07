package com.duoc.gestionpedidos.repository;

import com.duoc.gestionpedidos.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    /** Consulta por transportista y fecha de despacho (ambos opcionales se resuelven en el service). */
    List<GuiaDespacho> findByTransportistaAndFechaDespacho(String transportista, LocalDate fechaDespacho);

    List<GuiaDespacho> findByTransportista(String transportista);

    List<GuiaDespacho> findByFechaDespacho(LocalDate fechaDespacho);

    boolean existsByNumeroGuia(String numeroGuia);
}

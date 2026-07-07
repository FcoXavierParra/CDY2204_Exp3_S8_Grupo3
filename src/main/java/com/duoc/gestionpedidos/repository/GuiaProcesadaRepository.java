package com.duoc.gestionpedidos.repository;

import com.duoc.gestionpedidos.model.GuiaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA de la tabla GUIA_PROCESADA (Oracle Cloud).
 */
@Repository
public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {

    List<GuiaProcesada> findByTransportista(String transportista);

    List<GuiaProcesada> findByNumeroGuia(String numeroGuia);
}

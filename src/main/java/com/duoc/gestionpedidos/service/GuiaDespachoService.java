package com.duoc.gestionpedidos.service;

import com.duoc.gestionpedidos.dto.GuiaRequestDTO;
import com.duoc.gestionpedidos.exception.ResourceNotFoundException;
import com.duoc.gestionpedidos.model.EstadoGuia;
import com.duoc.gestionpedidos.model.GuiaDespacho;
import com.duoc.gestionpedidos.repository.GuiaDespachoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Logica de negocio de las guias de despacho (metadatos en H2).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaDespachoService {

    private final GuiaDespachoRepository repository;

    @Transactional
    public GuiaDespacho crear(GuiaRequestDTO dto) {
        if (repository.existsByNumeroGuia(dto.getNumeroGuia())) {
            throw new IllegalArgumentException(
                "Ya existe una guia con el numero: " + dto.getNumeroGuia());
        }
        GuiaDespacho guia = GuiaDespacho.builder()
                .numeroGuia(dto.getNumeroGuia())
                .transportista(dto.getTransportista())
                .origen(dto.getOrigen())
                .destino(dto.getDestino())
                .descripcion(dto.getDescripcion())
                .fechaDespacho(dto.getFechaDespacho())
                .estado(EstadoGuia.GENERADA)
                .build();
        GuiaDespacho guardada = repository.save(guia);
        log.info("Guia creada id={} numero={}", guardada.getId(), guardada.getNumeroGuia());
        return guardada;
    }

    @Transactional(readOnly = true)
    public GuiaDespacho obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Guia de despacho no encontrada con id: " + id));
    }

    @Transactional
    public GuiaDespacho actualizar(Long id, GuiaRequestDTO dto) {
        GuiaDespacho guia = obtenerPorId(id);
        guia.setNumeroGuia(dto.getNumeroGuia());
        guia.setTransportista(dto.getTransportista());
        guia.setOrigen(dto.getOrigen());
        guia.setDestino(dto.getDestino());
        guia.setDescripcion(dto.getDescripcion());
        guia.setFechaDespacho(dto.getFechaDespacho());
        GuiaDespacho actualizada = repository.save(guia);
        log.info("Guia actualizada id={}", id);
        return actualizada;
    }

    @Transactional
    public void eliminar(Long id) {
        GuiaDespacho guia = obtenerPorId(id);
        repository.delete(guia);
        log.info("Guia eliminada id={}", id);
    }

    /** Marca la guia como almacenada en S3 y guarda su key. */
    @Transactional
    public GuiaDespacho registrarSubidaS3(Long id, String s3Key) {
        GuiaDespacho guia = obtenerPorId(id);
        guia.setS3Key(s3Key);
        guia.setEstado(EstadoGuia.ALMACENADA_S3);
        return repository.save(guia);
    }

    /** Consulta por transportista y/o fecha de despacho (ambos opcionales). */
    @Transactional(readOnly = true)
    public List<GuiaDespacho> consultar(String transportista, LocalDate fechaDespacho) {
        boolean tieneTransportista = transportista != null && !transportista.isBlank();
        boolean tieneFecha = fechaDespacho != null;

        if (tieneTransportista && tieneFecha) {
            return repository.findByTransportistaAndFechaDespacho(transportista, fechaDespacho);
        } else if (tieneTransportista) {
            return repository.findByTransportista(transportista);
        } else if (tieneFecha) {
            return repository.findByFechaDespacho(fechaDespacho);
        }
        return repository.findAll();
    }
}

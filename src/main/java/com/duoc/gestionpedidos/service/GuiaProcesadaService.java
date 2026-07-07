package com.duoc.gestionpedidos.service;

import com.duoc.gestionpedidos.dto.GuiaMensaje;
import com.duoc.gestionpedidos.model.GuiaProcesada;
import com.duoc.gestionpedidos.repository.GuiaProcesadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transforma el mensaje recibido desde la COLA 1 en una entidad
 * {@link GuiaProcesada} y la persiste en la tabla GUIA_PROCESADA de Oracle Cloud.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuiaProcesadaService {

    private final GuiaProcesadaRepository repository;

    @Transactional
    public GuiaProcesada guardarDesdeMensaje(GuiaMensaje msg) {
        GuiaProcesada procesada = GuiaProcesada.builder()
                .guiaId(msg.getGuiaId())
                .numeroGuia(msg.getNumeroGuia())
                .transportista(msg.getTransportista())
                .origen(msg.getOrigen())
                .destino(msg.getDestino())
                .descripcion(msg.getDescripcion())
                .fechaDespacho(msg.getFechaDespacho())
                .fechaProceso(LocalDateTime.now())
                .estado("PROCESADO")
                .build();

        GuiaProcesada guardada = repository.save(procesada);
        log.info("Guia {} persistida en Oracle Cloud (GUIA_PROCESADA) con id {}",
                guardada.getNumeroGuia(), guardada.getId());
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<GuiaProcesada> listarTodas() {
        return repository.findAll();
    }
}

package com.duoc.gestionpedidos.dto;

import com.duoc.gestionpedidos.model.GuiaDespacho;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Mensaje que viaja por la cola RabbitMQ (serializado en JSON).
 *
 * Representa una guia de despacho enviada a la COLA 1 para su procesamiento
 * asincrono. El consumidor lo transforma en la entidad {@code GuiaProcesada}
 * y lo persiste en Oracle Cloud.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuiaMensaje {

    /** Id de la guia original (tabla GUIA_DESPACHO). */
    private Long guiaId;

    private String numeroGuia;
    private String transportista;
    private String origen;
    private String destino;
    private String descripcion;
    private LocalDate fechaDespacho;

    /**
     * Bandera de apoyo para EVIDENCIAR la cola de errores (COLA 2) en la demo:
     * si viene en true, el consumidor rechaza el mensaje a proposito para que
     * RabbitMQ lo enrute a la Dead Letter Queue. En uso normal es false.
     */
    @Builder.Default
    private boolean simularError = false;

    /** Construye el mensaje a partir de la entidad ya persistida. */
    public static GuiaMensaje fromEntity(GuiaDespacho g, boolean simularError) {
        return GuiaMensaje.builder()
                .guiaId(g.getId())
                .numeroGuia(g.getNumeroGuia())
                .transportista(g.getTransportista())
                .origen(g.getOrigen())
                .destino(g.getDestino())
                .descripcion(g.getDescripcion())
                .fechaDespacho(g.getFechaDespacho())
                .simularError(simularError)
                .build();
    }
}

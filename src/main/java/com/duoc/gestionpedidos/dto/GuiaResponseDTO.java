package com.duoc.gestionpedidos.dto;

import com.duoc.gestionpedidos.model.EstadoGuia;
import com.duoc.gestionpedidos.model.GuiaDespacho;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos de salida de una guia de despacho.
 */
@Data
@Builder
public class GuiaResponseDTO {

    private Long id;
    private String numeroGuia;
    private String transportista;
    private String origen;
    private String destino;
    private String descripcion;
    private LocalDate fechaDespacho;
    private EstadoGuia estado;
    private String s3Key;
    private LocalDateTime fechaCreacion;

    public static GuiaResponseDTO fromEntity(GuiaDespacho g) {
        return GuiaResponseDTO.builder()
                .id(g.getId())
                .numeroGuia(g.getNumeroGuia())
                .transportista(g.getTransportista())
                .origen(g.getOrigen())
                .destino(g.getDestino())
                .descripcion(g.getDescripcion())
                .fechaDespacho(g.getFechaDespacho())
                .estado(g.getEstado())
                .s3Key(g.getS3Key())
                .fechaCreacion(g.getFechaCreacion())
                .build();
    }
}

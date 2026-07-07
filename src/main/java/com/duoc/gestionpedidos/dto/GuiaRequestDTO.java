package com.duoc.gestionpedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Datos de entrada para crear o actualizar una guia de despacho.
 */
@Data
public class GuiaRequestDTO {

    @NotBlank(message = "El numero de guia es obligatorio")
    private String numeroGuia;

    @NotBlank(message = "El transportista es obligatorio")
    private String transportista;

    @NotBlank(message = "El origen es obligatorio")
    private String origen;

    @NotBlank(message = "El destino es obligatorio")
    private String destino;

    private String descripcion;

    @NotNull(message = "La fecha de despacho es obligatoria")
    private LocalDate fechaDespacho;
}

package com.duoc.gestionpedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Guia de despacho de la empresa transportista.
 * Los metadatos se persisten en Oracle Cloud (tabla GUIA_DESPACHO);
 * el PDF asociado se almacena en AWS S3.
 */
@Entity
@Table(name = "GUIA_DESPACHO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guia_despacho_seq")
    @SequenceGenerator(name = "guia_despacho_seq", sequenceName = "SEQ_GUIA_DESPACHO", allocationSize = 1)
    private Long id;

    /** Numero de guia (correlativo de negocio). */
    @Column(nullable = false, unique = true)
    private String numeroGuia;

    /** Transportista responsable del despacho. */
    @Column(nullable = false)
    private String transportista;

    /** Direccion / ciudad de origen. */
    @Column(nullable = false)
    private String origen;

    /** Direccion / ciudad de destino. */
    @Column(nullable = false)
    private String destino;

    /** Descripcion de la carga. */
    @Column(length = 1000)
    private String descripcion;

    /** Fecha de despacho (usada para la consulta por transportista y fecha). */
    @Column(nullable = false)
    private LocalDate fechaDespacho;

    /** Estado del documento: GENERADA o ALMACENADA_S3. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoGuia estado;

    /** Ruta (key) del PDF dentro del bucket S3, una vez subido. */
    private String s3Key;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoGuia.GENERADA;
        }
    }
}

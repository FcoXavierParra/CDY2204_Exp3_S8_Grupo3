package com.duoc.gestionpedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa la tabla NUEVA GUIA_PROCESADA en Oracle Cloud
 * (Autonomous Database). Aqui el consumidor persiste cada guia que lee desde
 * la COLA 1 de RabbitMQ.
 *
 * Es una tabla DISTINTA a la usada en las sumativas anteriores (GUIA_DESPACHO),
 * tal como exige la actividad de la Semana 8.
 */
@Entity
@Table(name = "GUIA_PROCESADA")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuiaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guia_procesada_seq")
    @SequenceGenerator(name = "guia_procesada_seq", sequenceName = "SEQ_GUIA_PROCESADA", allocationSize = 1)
    @Column(name = "ID_PROCESADA")
    private Long id;

    /** Id de la guia original en la tabla GUIA_DESPACHO. */
    @Column(name = "GUIA_ID")
    private Long guiaId;

    @Column(name = "NUMERO_GUIA", nullable = false, length = 60)
    private String numeroGuia;

    @Column(name = "TRANSPORTISTA", nullable = false, length = 150)
    private String transportista;

    @Column(name = "ORIGEN", nullable = false, length = 150)
    private String origen;

    @Column(name = "DESTINO", nullable = false, length = 150)
    private String destino;

    @Column(name = "DESCRIPCION", length = 1000)
    private String descripcion;

    @Column(name = "FECHA_DESPACHO", nullable = false)
    private LocalDate fechaDespacho;

    /** Momento en que el consumidor proceso el mensaje desde la cola. */
    @Column(name = "FECHA_PROCESO", nullable = false)
    private LocalDateTime fechaProceso;

    /** Estado del procesamiento asincrono (ej. PROCESADO). */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @PrePersist
    void prePersist() {
        if (fechaProceso == null) {
            fechaProceso = LocalDateTime.now();
        }
        if (estado == null) {
            estado = "PROCESADO";
        }
    }
}

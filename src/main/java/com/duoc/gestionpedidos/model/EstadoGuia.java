package com.duoc.gestionpedidos.model;

/**
 * Estado del ciclo de vida de una guia de despacho.
 */
public enum EstadoGuia {
    /** Guia creada en el sistema, sin PDF aun en S3. */
    GENERADA,
    /** PDF de la guia ya almacenado en AWS S3. */
    ALMACENADA_S3
}

-- ================================================================
--  CDY2204 - Semana 8 - Base de datos Oracle Cloud (Autonomous DB)
--  Script de respaldo para crear las tablas y secuencias.
--
--  NOTA: con spring.jpa.hibernate.ddl-auto=update, Hibernate crea
--  automaticamente estas tablas y secuencias al arrancar la app.
--  Este script se entrega como respaldo / evidencia y para ejecutar
--  manualmente en Oracle Database Actions (SQL Web) o SQL Developer,
--  conectado como usuario ADMIN.
-- ================================================================

-- ---------------------------------------------------------------
--  Tabla 1: GUIA_DESPACHO (metadatos de las guias) - migrada de S6
-- ---------------------------------------------------------------
CREATE SEQUENCE SEQ_GUIA_DESPACHO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE GUIA_DESPACHO (
    ID              NUMBER          NOT NULL,
    NUMERO_GUIA     VARCHAR2(60)    NOT NULL,
    TRANSPORTISTA   VARCHAR2(150)   NOT NULL,
    ORIGEN          VARCHAR2(150)   NOT NULL,
    DESTINO         VARCHAR2(150)   NOT NULL,
    DESCRIPCION     VARCHAR2(1000),
    FECHA_DESPACHO  DATE            NOT NULL,
    ESTADO          VARCHAR2(20)    NOT NULL,
    S3KEY           VARCHAR2(500),
    FECHA_CREACION  TIMESTAMP       NOT NULL,
    CONSTRAINT PK_GUIA_DESPACHO PRIMARY KEY (ID),
    CONSTRAINT UQ_GUIA_DESPACHO_NUMERO UNIQUE (NUMERO_GUIA)
);

-- ---------------------------------------------------------------
--  Tabla 2 (NUEVA): GUIA_PROCESADA
--  Aqui el consumidor guarda las guias leidas desde la COLA 1.
--  Es una tabla DISTINTA a la de sumativas anteriores.
-- ---------------------------------------------------------------
CREATE SEQUENCE SEQ_GUIA_PROCESADA START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE GUIA_PROCESADA (
    ID_PROCESADA    NUMBER          NOT NULL,
    GUIA_ID         NUMBER,
    NUMERO_GUIA     VARCHAR2(60)    NOT NULL,
    TRANSPORTISTA   VARCHAR2(150)   NOT NULL,
    ORIGEN          VARCHAR2(150)   NOT NULL,
    DESTINO         VARCHAR2(150)   NOT NULL,
    DESCRIPCION     VARCHAR2(1000),
    FECHA_DESPACHO  DATE            NOT NULL,
    FECHA_PROCESO   TIMESTAMP       NOT NULL,
    ESTADO          VARCHAR2(20)    NOT NULL,
    CONSTRAINT PK_GUIA_PROCESADA PRIMARY KEY (ID_PROCESADA)
);

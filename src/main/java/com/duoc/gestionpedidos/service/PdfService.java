package com.duoc.gestionpedidos.service;

import com.duoc.gestionpedidos.model.GuiaDespacho;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF de la guia de despacho a partir de sus metadatos.
 */
@Service
@Slf4j
public class PdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public byte[] generarGuiaPdf(GuiaDespacho guia) {
        log.info("Generando PDF de la guia id={}", guia.getId());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("GUIA DE DESPACHO")
                    .setBold().setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Empresa Transportista - CDY2204")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("\n"));

            Table tabla = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth();

            agregarFila(tabla, "N de Guia", guia.getNumeroGuia());
            agregarFila(tabla, "Transportista", guia.getTransportista());
            agregarFila(tabla, "Origen", guia.getOrigen());
            agregarFila(tabla, "Destino", guia.getDestino());
            agregarFila(tabla, "Descripcion de la carga",
                    guia.getDescripcion() == null ? "-" : guia.getDescripcion());
            agregarFila(tabla, "Fecha de despacho",
                    guia.getFechaDespacho() == null ? "-" : guia.getFechaDespacho().format(FECHA));
            agregarFila(tabla, "Estado", String.valueOf(guia.getEstado()));

            doc.add(tabla);
            doc.add(new Paragraph("\nDocumento generado automaticamente por el Sistema de Gestion de Pedidos.")
                    .setFontSize(8).setItalic());

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF de la guia: " + e.getMessage(), e);
        }
    }

    private void agregarFila(Table tabla, String etiqueta, String valor) {
        tabla.addCell(new Cell().add(new Paragraph(etiqueta).setBold()));
        tabla.addCell(new Cell().add(new Paragraph(valor == null ? "" : valor)));
    }
}

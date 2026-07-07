package com.duoc.gestionpedidos.controller;

import com.duoc.gestionpedidos.dto.GuiaMensaje;
import com.duoc.gestionpedidos.dto.GuiaRequestDTO;
import com.duoc.gestionpedidos.dto.GuiaResponseDTO;
import com.duoc.gestionpedidos.model.GuiaDespacho;
import com.duoc.gestionpedidos.service.GuiaDespachoService;
import com.duoc.gestionpedidos.service.PdfService;
import com.duoc.gestionpedidos.service.ProductorGuiaService;
import com.duoc.gestionpedidos.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints REST de las guias de despacho. Todos requieren un JWT valido de Azure AD B2C.
 *
 * Roles:
 *   GESTOR   -> todos los endpoints.
 *   CONSULTA -> SOLO el endpoint de Descargar guias.
 *
 * Los permisos se aplican con @PreAuthorize (Spring Method Security). El claim de rol del
 * token se transforma en autoridad ROLE_GESTOR / ROLE_CONSULTA en RoleClaimConverter.
 */
@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guias de Despacho", description = "CRUD de guias + gestion del PDF en AWS S3")
@SecurityRequirement(name = "bearerAuth")
public class GuiaDespachoController {

    private final GuiaDespachoService guiaService;
    private final PdfService pdfService;
    private final S3Service s3Service;
    private final ProductorGuiaService productorGuiaService;

    // ── 1. Crear guia (guarda metadatos y la ENVIA a la COLA 1 de RabbitMQ) ────

    @Operation(summary = "Crear guia de despacho",
        description = "Crea la guia (tabla GUIA_DESPACHO) y la publica en la COLA 1 de RabbitMQ para "
            + "su procesamiento asincrono. Con simularError=true la guia se deriva a la COLA 2 de "
            + "errores (para evidencia). Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @Valid @RequestBody GuiaRequestDTO dto,
            @RequestParam(defaultValue = "false") boolean simularError,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Usuario [{}] crea guia {}", jwt.getSubject(), dto.getNumeroGuia());
        GuiaDespacho guia = guiaService.crear(dto);

        // El sistema de guias envia los datos a la cola (COLA 1); ante fallo, a la COLA 2.
        boolean enviadaACola1 = productorGuiaService.enviarGuia(
                GuiaMensaje.fromEntity(guia, simularError));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", "Guia creada y enviada a la cola de mensajeria");
        body.put("guia", GuiaResponseDTO.fromEntity(guia));
        body.put("cola", enviadaACola1 ? "COLA 1 (guias.queue)" : "COLA 2 (guias.error.queue)");
        body.put("simularError", simularError);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── 2. Subir el PDF de la guia a S3 ───────────────────────────────────────

    @Operation(summary = "Subir guia a AWS S3",
        description = "Genera el PDF de la guia y lo sube al bucket S3. Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @PostMapping("/{id}/s3")
    public ResponseEntity<Map<String, Object>> subirS3(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Usuario [{}] sube guia {} a S3", jwt.getSubject(), id);
        GuiaDespacho guia = guiaService.obtenerPorId(id);
        byte[] pdf = pdfService.generarGuiaPdf(guia);
        String s3Key = s3Service.subirGuia(guia.getNumeroGuia(), pdf, MediaType.APPLICATION_PDF_VALUE);
        GuiaDespacho actualizada = guiaService.registrarSubidaS3(id, s3Key);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Guia subida exitosamente a S3",
            "id", id,
            "numeroGuia", actualizada.getNumeroGuia(),
            "estado", actualizada.getEstado(),
            "s3Key", s3Key
        ));
    }

    // ── 3. Descargar el PDF de la guia (con validacion de permisos) ────────────

    @Operation(summary = "Descargar guia desde AWS S3",
        description = "Descarga el PDF de la guia. Roles GESTOR y CONSULTA.")
    @PreAuthorize("hasAnyRole('GESTOR','CONSULTA')")
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Usuario [{}] descarga guia {}", jwt.getSubject(), id);
        GuiaDespacho guia = guiaService.obtenerPorId(id);
        byte[] pdf = s3Service.descargarGuia(guia.getNumeroGuia());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("guia_" + guia.getNumeroGuia() + ".pdf").build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ── 4. Modificar / actualizar la guia ──────────────────────────────────────

    @Operation(summary = "Actualizar guia de despacho", description = "Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<GuiaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuiaRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Usuario [{}] actualiza guia {}", jwt.getSubject(), id);
        GuiaDespacho guia = guiaService.actualizar(id, dto);
        return ResponseEntity.ok(GuiaResponseDTO.fromEntity(guia));
    }

    // ── 5. Eliminar la guia ─────────────────────────────────────────────────────

    @Operation(summary = "Eliminar guia de despacho",
        description = "Elimina los metadatos y, si existe, el PDF en S3. Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Usuario [{}] elimina guia {}", jwt.getSubject(), id);
        GuiaDespacho guia = guiaService.obtenerPorId(id);
        if (guia.getS3Key() != null) {
            try {
                s3Service.borrarGuia(guia.getNumeroGuia());
            } catch (RuntimeException e) {
                log.warn("No se pudo borrar el PDF en S3 de la guia {}: {}", id, e.getMessage());
            }
        }
        guiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ── 6. Consultar por transportista y/o fecha ────────────────────────────────

    @Operation(summary = "Consultar guias por transportista y fecha", description = "Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @GetMapping
    public ResponseEntity<List<GuiaResponseDTO>> consultar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<GuiaResponseDTO> guias = guiaService.consultar(transportista, fecha)
                .stream().map(GuiaResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(guias);
    }
}

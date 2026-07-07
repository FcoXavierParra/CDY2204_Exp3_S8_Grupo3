package com.duoc.gestionpedidos.controller;

import com.duoc.gestionpedidos.model.GuiaProcesada;
import com.duoc.gestionpedidos.service.GuiaProcesadaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint para consultar las guias que el CONSUMIDOR ya proceso desde la COLA 1
 * y persistio en la tabla GUIA_PROCESADA de Oracle Cloud.
 *
 * Sirve como evidencia del flujo asincrono completo:
 *   POST /api/guias  ->  COLA 1  ->  consumidor  ->  Oracle (GUIA_PROCESADA)  ->  GET aqui.
 */
@RestController
@RequestMapping("/api/guias-procesadas")
@RequiredArgsConstructor
@Tag(name = "Guias Procesadas", description = "Guias consumidas desde la cola y guardadas en Oracle Cloud")
@SecurityRequirement(name = "bearerAuth")
public class GuiaProcesadaController {

    private final GuiaProcesadaService guiaProcesadaService;

    @Operation(summary = "Listar guias procesadas desde la cola",
        description = "Devuelve las guias que el consumidor guardo en Oracle Cloud. Solo rol GESTOR.")
    @PreAuthorize("hasRole('GESTOR')")
    @GetMapping
    public ResponseEntity<List<GuiaProcesada>> listar() {
        return ResponseEntity.ok(guiaProcesadaService.listarTodas());
    }
}

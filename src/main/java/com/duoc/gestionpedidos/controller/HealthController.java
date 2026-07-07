package com.duoc.gestionpedidos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint publico de salud, usado por el health check del EC2 y del API Gateway.
 * GET /health -> 200 {"status":"UP"}
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "gestion-pedidos"));
    }
}

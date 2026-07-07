package com.duoc.gestionpedidos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.*;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Sistema de Gestion de Pedidos y Guias de Despacho - CDY2204",
        version = "1.0.0",
        description = "API REST para gestionar guias de despacho de una empresa transportista. "
            + "Securitizada con Spring Security, autenticada con Azure AD B2C (IDaaS) y "
            + "almacenamiento de PDF en AWS S3.",
        contact = @Contact(name = "Equipo CDY2204", email = "team@duoc.cl")
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenido desde Azure AD B2C (jwt.ms tras correr el User Flow). Formato: Bearer {token}"
)
public class OpenApiConfig {
}

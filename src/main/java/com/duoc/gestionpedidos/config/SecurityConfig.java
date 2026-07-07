package com.duoc.gestionpedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de Spring Security como OAuth2 Resource Server.
 *
 * Valida los JWT emitidos por Azure AD B2C (IDaaS):
 *   - firma (contra el jwk-set-uri),
 *   - issuer,
 *   - audience (Application Client ID).
 *
 * Los permisos por rol (GESTOR / CONSULTA) se aplican con @PreAuthorize en el controller,
 * gracias a @EnableMethodSecurity. El claim de rol se convierte en autoridades
 * ROLE_GESTOR / ROLE_CONSULTA mediante {@link RoleClaimConverter}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${azure.b2c.audience}")
    private String audience;

    @Value("${azure.b2c.role-claim}")
    private String roleClaim;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publicos: health (EC2 / API Gateway) y documentacion
                .requestMatchers(
                    "/health",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/h2-console/**"
                ).permitAll()
                // Todo lo demas requiere un token valido; el rol se valida con @PreAuthorize
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * Decoder que valida firma (jwk-set-uri), issuer y audience del token de Azure AD B2C.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience =
            new DelegatingOAuth2TokenValidator<>(withIssuer, new AudienceValidator(audience));

        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }

    /**
     * Convierte el claim de rol del JWT (ej. extension_rol = "gestor") en la
     * autoridad de Spring ROLE_GESTOR, para que @PreAuthorize("hasRole('GESTOR')") funcione.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RoleClaimConverter(roleClaim));
        return converter;
    }
}

package com.duoc.gestionpedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configura el cliente de AWS S3.
 *
 * AWS Academy entrega credenciales temporales (access key + secret key + session token)
 * que cambian en cada inicio del laboratorio. Por eso:
 *   - Si se definen aws.access-key / aws.secret-key / aws.session-token, se usan esos valores.
 *   - Si NO se definen, se usa la cadena de credenciales por defecto del SDK, que lee
 *     automaticamente las variables de entorno AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY /
 *     AWS_SESSION_TOKEN (lo recomendado en el contenedor de EC2).
 */
@Configuration
public class AwsS3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.session-token:}")
    private String sessionToken;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(region));

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            AwsCredentials credentials = sessionToken.isBlank()
                ? AwsBasicCredentials.create(accessKey, secretKey)
                : AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}

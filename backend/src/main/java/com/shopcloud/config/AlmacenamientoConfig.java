package com.shopcloud.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cliente S3 para Neon Object Storage. Solo se crea si existe AWS_ENDPOINT_URL_S3,
 * así la aplicación arranca igual en local o en pruebas sin almacenamiento.
 */
@Configuration
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class AlmacenamientoConfig {

    @Value("${AWS_ENDPOINT_URL_S3}")
    private String endpoint;

    @Value("${AWS_REGION:us-east-2}")
    private String region;

    @Value("${AWS_ACCESS_KEY_ID}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretAccessKey;

    // Neon solo admite direcciones de tipo ruta: https://endpoint/bucket/clave
    private static final S3Configuration ESTILO_RUTA = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credenciales())
                .serviceConfiguration(ESTILO_RUTA)
                .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credenciales())
                .serviceConfiguration(ESTILO_RUTA)
                .build();
    }

    private StaticCredentialsProvider credenciales() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}

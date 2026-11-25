package com.shopcloud.util;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Guarda y lee archivos en el bucket de Neon (por defecto "uploads", privado).
 * En la base de datos se guarda la clave del objeto, no los bytes.
 */
@Service
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class AlmacenamientoArchivosService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public AlmacenamientoArchivosService(
            S3Client s3,
            S3Presigner presigner,
            @Value("${app.almacenamiento.bucket:uploads}") String bucket
    ) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    public void subir(String clave, byte[] contenido, String tipoContenido) {
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(clave).contentType(tipoContenido).build(),
                RequestBody.fromBytes(contenido)
        );
    }

    public byte[] descargar(String clave) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(clave).build()).asByteArray();
    }

    /** URL temporal de lectura para el navegador, sin exponer credenciales. */
    public String urlFirmada(String clave, Duration vigencia) {
        return presigner.presignGetObject(peticion -> peticion
                .signatureDuration(vigencia)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(clave).build())
        ).url().toString();
    }

    public void eliminar(String clave) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    }
}

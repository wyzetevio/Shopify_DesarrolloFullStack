package com.shopcloud.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.shopcloud.config.AlmacenamientoConfig;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Prueba contra el bucket real de Neon. Solo se ejecuta si están las credenciales
 * (neon env pull / .env.local); en CI sin ellas se omite.
 */
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class AlmacenamientoNeonTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withPropertyValues(
                    "AWS_ENDPOINT_URL_S3=" + System.getenv("AWS_ENDPOINT_URL_S3"),
                    "AWS_REGION=" + System.getenv("AWS_REGION"),
                    "AWS_ACCESS_KEY_ID=" + System.getenv("AWS_ACCESS_KEY_ID"),
                    "AWS_SECRET_ACCESS_KEY=" + System.getenv("AWS_SECRET_ACCESS_KEY"))
            .withUserConfiguration(AlmacenamientoConfig.class, AlmacenamientoArchivosService.class);

    @Test
    void subeDescargaFirmaYElimina() {
        contexto.run(ctx -> {
            AlmacenamientoArchivosService almacenamiento = ctx.getBean(AlmacenamientoArchivosService.class);
            String clave = "pruebas/" + UUID.randomUUID() + ".txt";
            byte[] contenido = "hola desde ShopCloud".getBytes(StandardCharsets.UTF_8);

            almacenamiento.subir(clave, contenido, "text/plain");
            try {
                assertThat(almacenamiento.descargar(clave)).isEqualTo(contenido);

                // La URL firmada funciona sin credenciales, como la usaría el navegador
                String url = almacenamiento.urlFirmada(clave, Duration.ofMinutes(5));
                HttpResponse<String> respuesta = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create(url)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(respuesta.statusCode()).isEqualTo(200);
                assertThat(respuesta.body()).isEqualTo("hola desde ShopCloud");
            } finally {
                almacenamiento.eliminar(clave);
            }

            assertThatThrownBy(() -> almacenamiento.descargar(clave)).isInstanceOf(NoSuchKeyException.class);
        });
    }
}

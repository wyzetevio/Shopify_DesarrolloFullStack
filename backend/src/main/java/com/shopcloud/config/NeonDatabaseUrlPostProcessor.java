package com.shopcloud.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Traduce la cadena de conexión de Neon ({@code postgresql://usuario:clave@host/base?...})
 * a las propiedades JDBC de Spring.
 *
 * <p>Solo actúa si {@code DB_URL} no está definida, así la configuración local con
 * {@code DB_URL}/{@code DB_USERNAME}/{@code DB_PASSWORD} sigue funcionando igual.
 * Prefiere {@code DATABASE_URL_UNPOOLED}: Hikari ya mantiene su propio pool, así que
 * no hace falta pasar por el PgBouncer de Neon.
 */
public class NeonDatabaseUrlPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String NOMBRE_FUENTE = "neonDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.hasText(environment.getProperty("DB_URL"))) {
            return;
        }
        String url = environment.getProperty("DATABASE_URL_UNPOOLED");
        if (!StringUtils.hasText(url)) {
            url = environment.getProperty("DATABASE_URL");
        }
        if (!StringUtils.hasText(url)) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(NOMBRE_FUENTE, aPropiedadesJdbc(url)));
    }

    static Map<String, Object> aPropiedadesJdbc(String url) {
        URI uri = URI.create(url);
        if (!"postgresql".equals(uri.getScheme()) && !"postgres".equals(uri.getScheme())) {
            throw new IllegalStateException("DATABASE_URL debe empezar por postgresql://");
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath());

        List<String> parametros = new ArrayList<>();
        if (uri.getRawQuery() != null) {
            for (String parametro : uri.getRawQuery().split("&")) {
                // libpq usa channel_binding; el driver JDBC lo llama channelBinding
                parametros.add(parametro.startsWith("channel_binding=")
                        ? "channelBinding=" + parametro.substring("channel_binding=".length())
                        : parametro);
            }
        }
        if (!parametros.isEmpty()) {
            jdbc.append('?').append(String.join("&", parametros));
        }

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("spring.datasource.url", jdbc.toString());
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int separador = userInfo.indexOf(':');
            String usuario = separador < 0 ? userInfo : userInfo.substring(0, separador);
            propiedades.put("spring.datasource.username", decodificar(usuario));
            if (separador >= 0) {
                propiedades.put("spring.datasource.password", decodificar(userInfo.substring(separador + 1)));
            }
        }
        return propiedades;
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        // Después de cargar application.properties y el .env importado
        return Ordered.LOWEST_PRECEDENCE;
    }
}

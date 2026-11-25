package com.shopcloud.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

class NeonDatabaseUrlPostProcessorTest {

    private final NeonDatabaseUrlPostProcessor procesador = new NeonDatabaseUrlPostProcessor();

    @Test
    void convierteLaCadenaDeNeonAPropiedadesJdbc() {
        Map<String, Object> propiedades = NeonDatabaseUrlPostProcessor.aPropiedadesJdbc(
                "postgresql://usuario:cl%40ve@ep-x.us-east-2.aws.neon.tech/shop?sslmode=require&channel_binding=require");

        assertThat(propiedades)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-x.us-east-2.aws.neon.tech/shop?sslmode=require&channelBinding=require")
                .containsEntry("spring.datasource.username", "usuario")
                .containsEntry("spring.datasource.password", "cl@ve");
    }

    @Test
    void prefiereLaConexionDirectaSobreLaDelPool() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://u:p@ep-x-pooler.neon.tech/db")
                .withProperty("DATABASE_URL_UNPOOLED", "postgresql://u:p@ep-x.neon.tech/db");

        procesador.postProcessEnvironment(entorno, null);

        assertThat(entorno.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://ep-x.neon.tech/db");
    }

    @Test
    void noTocaNadaSiYaHayDbUrl() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("DB_URL", "jdbc:postgresql://localhost:5432/shopcloud_test")
                .withProperty("DATABASE_URL", "postgresql://u:p@ep-x.neon.tech/db");

        procesador.postProcessEnvironment(entorno, null);

        assertThat(entorno.getPropertySources().contains(NeonDatabaseUrlPostProcessor.NOMBRE_FUENTE)).isFalse();
    }

    @Test
    void noTocaNadaSinVariablesDeNeon() {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        procesador.postProcessEnvironment(entorno, null);

        assertThat(entorno.getPropertySources().contains(NeonDatabaseUrlPostProcessor.NOMBRE_FUENTE)).isFalse();
    }
}

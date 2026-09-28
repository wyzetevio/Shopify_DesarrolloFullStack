package com.shopcloud.tenant;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class TenantConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {

        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns(
                        "/api/productos/**",
                        "/api/categorias/**",
                        "/api/inventario/**",
                        "/api/descuentos/**",
                        "/api/clientes/**",
                        "/api/pedidos/**",
                        "/api/analitica/**",
                        "/api/suscripciones/**"
                );
    }
}
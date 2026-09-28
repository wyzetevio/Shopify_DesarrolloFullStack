package com.shopcloud.tenant;

import com.shopcloud.security.UsuarioPrincipal;
import com.shopcloud.tienda.repository.TiendaRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TiendaRepository tiendaRepository;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        String tenantHeader =
                request.getHeader(TENANT_HEADER);

        // 1. Verificar que exista X-Tenant-ID
        if (tenantHeader == null ||
                tenantHeader.isBlank()) {

            response.sendError(
                    HttpStatus.BAD_REQUEST.value(),
                    "El encabezado X-Tenant-ID es obligatorio"
            );

            return false;
        }

        // 2. Convertir el header a Long
        Long tiendaId;

        try {

            tiendaId = Long.valueOf(tenantHeader);

        } catch (NumberFormatException e) {

            response.sendError(
                    HttpStatus.BAD_REQUEST.value(),
                    "X-Tenant-ID debe ser un número válido"
            );

            return false;
        }

        // 3. Obtener usuario autenticado
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal()
                        instanceof UsuarioPrincipal principal)) {

            response.sendError(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Usuario no autenticado"
            );

            return false;
        }

        // 4. Las rutas del comprador solo necesitan una tienda activa.
        // La propiedad de sus recursos se valida después con el usuario del JWT.
        if (request.getRequestURI().startsWith("/api/mi-cuenta/")) {
            if (!tiendaRepository.existsByIdAndActivoTrue(tiendaId)) {
                response.sendError(
                        HttpStatus.NOT_FOUND.value(),
                        "La tienda activa no existe"
                );
                return false;
            }

            TenantContext.setTenantId(tiendaId);
            return true;
        }

        // 5. Verificar si es SUPER_ADMINISTRADOR
        boolean esSuperAdministrador =
                principal.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals(
                                                "ROLE_SUPER_ADMINISTRADOR"
                                        )
                        );

        boolean tieneAcceso;

        // 6. Validar acceso administrativo a la tienda
        if (esSuperAdministrador) {

            tieneAcceso =
                    tiendaRepository
                            .existsByIdAndActivoTrue(
                                    tiendaId
                            );

        } else {

            tieneAcceso =
                    tiendaRepository
                            .existsByIdAndPropietarioIdAndActivoTrue(
                                    tiendaId,
                                    principal.getId()
                            );
        }

        if (!tieneAcceso) {

            response.sendError(
                    HttpStatus.FORBIDDEN.value(),
                    "No tiene acceso a esta tienda"
            );

            return false;
        }

        // 7. Guardar tenant validado
        TenantContext.setTenantId(tiendaId);

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {

        TenantContext.clear();
    }
}

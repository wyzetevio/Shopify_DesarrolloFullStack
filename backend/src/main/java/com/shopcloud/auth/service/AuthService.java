package com.shopcloud.auth.service;

import com.shopcloud.auth.dto.AuthRespuestaDTO;
import com.shopcloud.auth.dto.LoginDTO;
import com.shopcloud.auth.dto.RegistroDTO;
import com.shopcloud.auth.dto.UsuarioRespuestaDTO;

import com.shopcloud.security.UsuarioPrincipal;
import com.shopcloud.security.jwt.JwtService;

import com.shopcloud.usuario.entity.Rol;
import com.shopcloud.usuario.entity.Usuario;
import com.shopcloud.usuario.repository.RolRepository;
import com.shopcloud.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class  AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Transactional
    public AuthRespuestaDTO registrar(RegistroDTO dto) {

        String correo = dto.getCorreoElectronico()
                .trim()
                .toLowerCase();

        if (usuarioRepository.existsByCorreoElectronico(correo)) {
            throw new IllegalArgumentException(
                    "El correo electrónico ya está registrado"
            );
        }

        Rol rolCliente = rolRepository
                .findByNombre("CLIENTE")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "El rol CLIENTE no existe"
                        )
                );

        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre().trim())
                .correoElectronico(correo)
                .contrasena(
                        passwordEncoder.encode(
                                dto.getContrasena()
                        )
                )
                .activo(true)
                .roles(roles)
                .build();

        Usuario usuarioGuardado =
                usuarioRepository.save(usuario);

        UsuarioPrincipal principal =
                new UsuarioPrincipal(usuarioGuardado);

        String token =
                jwtService.generarToken(principal);

        return construirRespuesta(
                usuarioGuardado,
                token
        );
    }

    public AuthRespuestaDTO login(LoginDTO dto) {

        String correo = dto.getCorreoElectronico()
                .trim()
                .toLowerCase();

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                correo,
                                dto.getContrasena()
                        )
                );

        UsuarioPrincipal principal =
                (UsuarioPrincipal)
                        authentication.getPrincipal();

        String token =
                jwtService.generarToken(principal);

        return construirRespuesta(
                principal.getUsuario(),
                token
        );
    }

    @Transactional(readOnly = true)
    public UsuarioRespuestaDTO obtenerUsuarioActual(
            UsuarioPrincipal principal
    ) {

        Usuario usuario = usuarioRepository
                .findById(principal.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado"
                        )
                );

        return UsuarioRespuestaDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .correoElectronico(
                        usuario.getCorreoElectronico()
                )
                .activo(usuario.getActivo())
                .roles(
                        obtenerRoles(usuario)
                )
                .build();
    }

    private AuthRespuestaDTO construirRespuesta(
            Usuario usuario,
            String token
    ) {

        return AuthRespuestaDTO.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .nombre(usuario.getNombre())
                .correoElectronico(
                        usuario.getCorreoElectronico()
                )
                .roles(
                        obtenerRoles(usuario)
                )
                .build();
    }

    private List<String> obtenerRoles(
            Usuario usuario
    ) {

        return usuario.getRoles()
                .stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
    }
}
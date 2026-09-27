package com.shopcloud.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarReglaNegocio(
            ReglaNegocioException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.CONFLICT,
                "Regla de negocio",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail manejarAccesoDenegado(
            AccesoDenegadoException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.FORBIDDEN,
                "Acceso denegado",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail manejarCredencialesInvalidas(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas",
                "Correo electrónico o contraseña incorrectos",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                crearProblemDetail(
                        HttpStatus.BAD_REQUEST,
                        "Error de validación",
                        "Uno o más campos contienen errores",
                        request
                );

        Map<String, String> errores =
                new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errores.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        problem.setProperty("errores", errores);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail manejarJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El cuerpo JSON no es válido o contiene valores incorrectos",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail manejarIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorGeneral(
            Exception ex,
            HttpServletRequest request
    ) {

        return crearProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno",
                "Ocurrió un error inesperado en el servidor",
                request
        );
    }

    private ProblemDetail crearProblemDetail(
            HttpStatus status,
            String titulo,
            String detalle,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detalle
                );

        problem.setTitle(titulo);

        problem.setInstance(
                URI.create(request.getRequestURI())
        );

        return problem;
    }
}
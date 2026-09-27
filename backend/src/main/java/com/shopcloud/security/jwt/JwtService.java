package com.shopcloud.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generarToken(UserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "roles",
                userDetails.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        );

        return crearToken(claims, userDetails);
    }

    private String crearToken(
            Map<String, Object> claims,
            UserDetails userDetails
    ) {

        Date ahora = new Date();

        Date expiracion = new Date(
                ahora.getTime() + jwtExpiration
        );

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getSigningKey())
                .compact();
    }

    public String extraerCorreo(String token) {
        return extraerClaim(
                token,
                Claims::getSubject
        );
    }

    public Date extraerExpiracion(String token) {
        return extraerClaim(
                token,
                Claims::getExpiration
        );
    }

    public <T> T extraerClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        Claims claims = extraerTodosLosClaims(token);

        return claimsResolver.apply(claims);
    }

    private Claims extraerTodosLosClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esTokenValido(
            String token,
            UserDetails userDetails
    ) {

        String correo = extraerCorreo(token);

        return correo.equals(userDetails.getUsername())
                && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return extraerExpiracion(token)
                .before(new Date());
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
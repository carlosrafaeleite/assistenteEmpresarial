package com.assistente.empresarial.security;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.assistente.empresarial.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // Em produção, coloque esta chave no application.properties (mínimo de 32 caracteres)
	@Value("${api.security.token.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 horas em milissegundos
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String gerarToken(Usuario usuario) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("empresaId", usuario.getEmpresaId().toString());
        extraClaims.put("perfil", usuario.getPerfil());
        extraClaims.put("nome", usuario.getNome());

        return Jwts.builder()
                .claims(extraClaims)
                .subject(usuario.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairTodasClaims(token).getSubject();
    }
    
    public String extrairEmpresaId(String token) {
        return extrairTodasClaims(token).get("empresaId", String.class);
    }

    private Claims extrairTodasClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValido(String token, String email) {
        final String emailExtraido = extrairEmail(token);
        return (emailExtraido.equals(email)) && !isTokenExpirado(token);
    }

    private boolean isTokenExpirado(String token) {
        return extrairTodasClaims(token).getExpiration().before(new Date());
    }
}

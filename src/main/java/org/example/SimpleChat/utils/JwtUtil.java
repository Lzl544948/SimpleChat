package org.example.SimpleChat.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    //private static final SecretKey key = Keys.hmacShaKeyFor("xinyanhui".getBytes(StandardCharsets.UTF_8));
    private static final Long expire = 1000 * 60 * 60 * 12L;   //12 hours
    private static final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public static String generateJwt(Map<String,Object> claims){
        return Jwts.builder()
                .addClaims(claims)
                .signWith(key,SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis()+expire ))
                .compact();
    }

    public static Map<String,Object> parseJwt(String jwt){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}

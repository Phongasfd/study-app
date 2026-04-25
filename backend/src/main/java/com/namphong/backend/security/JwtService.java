package com.namphong.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey SECRET;

    public JwtService(@Value("${jwt.secret}") String SECRET) {
        this.SECRET = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
    // inject secret from config - decode from base64 to bytes - turn bytes to secret key

    public String generateToken(UUID userId){
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(SECRET)
                .compact();
    }
    // For generate jwt token

    public UUID getUserId(String token){
        Claims claims = Jwts.parser()
                .verifyWith(SECRET)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    // Get userId from jwt token

//    public boolean isValidToken(String token){
//        try{
//            Jwts.parser()
//                    .verifyWith(SECRET)
//                    .build()
//                    .parseSignedClaims(token);
//            return true;
//        } catch(Exception e){
//            return false;
//        }
//    }
//
//    // for validating token

}

package com.project.configuration.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.project.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.project.exception.ErrorCode.INVALID_TOKEN;

@Component
@RequiredArgsConstructor
public class JwtToken {
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${jwt.secret.key}")
    private String jwtKey;
    private final String ACCESS_TOKEN = "AccessToken";
    private final String REFRESH_TOKEN = "RefreshToken";
    private final Long ACCESS_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 6;
    private final Long REFRESH_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 24 * 2;
    private final String BEARER = "Bearer ";
    private final String ACCESS_HEADER = "Authorization";
    private final String REFRESH_HEADER = "Refresh";
    private final String EMAIL = "email";

    public String generateAccessToken(String email) { // accessToken 생성
        Date date = new Date();
        return JWT.create()
                .withSubject(ACCESS_TOKEN)
                .withExpiresAt(new Date(date.getTime() + ACCESS_TOKEN_EXPIRATION_PERIOD))
                .withClaim(EMAIL, email)
                .sign(Algorithm.HMAC256(jwtKey));
    }

    public void generateRefreshToken(String email) { // refreshToken 생성
        Date now = new Date();
        Date refreshValidTime = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_PERIOD);

        String token = JWT.create()
                .withSubject(REFRESH_TOKEN)
                .withExpiresAt(refreshValidTime)
                .withClaim(EMAIL, email)
                .sign(Algorithm.HMAC256(jwtKey));

        redisTemplate.opsForValue().set(email, token);
    }

    public String getPayloadEmail(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(jwtKey))
                    .build()
                    .verify(token)
                    .getClaim(EMAIL)
                    .asString();
        } catch (Exception e) {
            throw new CustomException(INVALID_TOKEN);
        }
    }

    public String getAccessTokenFromRequest(HttpServletRequest request) { // Request Header 토큰 추출
        String tokenWithBearer = request.getHeader(ACCESS_HEADER);

        if (tokenWithBearer.startsWith(BEARER)) {
            return tokenWithBearer.substring(BEARER.length());
        }
        return null;
    }

    public boolean isValidToken(String token) {
        return JWT.require(Algorithm.HMAC256(jwtKey))
                .build()
                .verify(token)
                .getExpiresAt()
                .before(new Date());
    }
}

package vn.edu.utc.hotel_booking.modules.identity.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.TokenBlacklistServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistServiceImpl tokenBlacklistService;

    private String generateTestJwt(Instant expirationInstant) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test_user")
                .expirationTime(Date.from(expirationInstant))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new MACSigner("01234567890123456789012345678901"));
        return signedJWT.serialize();
    }

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Blacklist valid active token saves to Redis with correct remaining TTL")
    void blacklistToken_ActiveToken_Success() throws Exception {
        Instant exp = Instant.now().plus(1800, ChronoUnit.SECONDS); // 30 mins
        String token = generateTestJwt(exp);

        tokenBlacklistService.blacklistToken("Bearer " + token);

        verify(valueOperations, times(1)).set(
                eq("token:blacklist:" + token),
                eq("REVOKED"),
                longThat(ttl -> ttl > 1700 && ttl <= 1800),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Blacklist expired token skips Redis to save RAM")
    void blacklistToken_ExpiredToken_SkipsRedis() throws Exception {
        Instant exp = Instant.now().minus(10, ChronoUnit.SECONDS); // Expired
        String token = generateTestJwt(exp);

        tokenBlacklistService.blacklistToken(token);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("isBlacklisted returns true when key exists in Redis")
    void isBlacklisted_ReturnsTrue() {
        when(stringRedisTemplate.hasKey("token:blacklist:my_test_token")).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted("my_test_token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted returns false for null, blank, or garbage tokens")
    void isBlacklisted_GarbageTokens_ReturnsFalse() {
        assertThat(tokenBlacklistService.isBlacklisted(null)).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted("   ")).isFalse();
        assertThat(tokenBlacklistService.isBlacklisted("Bearer   ")).isFalse();
        verify(stringRedisTemplate, never()).hasKey(anyString());
    }
}

package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String REVOKED_VALUE = "REVOKED";

    @Override
    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        String cleanToken = token.regionMatches(true, 0, "Bearer ", 0, 7)
                ? token.substring(7).trim()
                : token.trim();

        if (!StringUtils.hasText(cleanToken)) {
            return;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(cleanToken);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            if (expirationTime == null) {
                log.warn("Access token does not contain 'exp' claim, fallback blacklist TTL 1 hour");
                stringRedisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + cleanToken, REVOKED_VALUE, 3600, TimeUnit.SECONDS);
                return;
            }

            long remainingSeconds = expirationTime.toInstant().getEpochSecond() - Instant.now().getEpochSecond();

            if (remainingSeconds > 0) {
                String redisKey = BLACKLIST_KEY_PREFIX + cleanToken;
                stringRedisTemplate.opsForValue().set(redisKey, REVOKED_VALUE, remainingSeconds, TimeUnit.SECONDS);
                log.info("Access token blacklisted successfully in Redis with dynamic TTL: {} seconds", remainingSeconds);
            } else {
                log.info("Access token has already expired naturally (remaining TTL <= 0). Skipping Redis blacklist to save memory.");
            }
        } catch (Exception e) {
            log.error("Failed to parse and blacklist JWT token: {}", e.getMessage(), e);
            // Fallback an toàn: Vẫn đưa vào blacklist với TTL mặc định 1 giờ nếu parse JWT gặp lỗi
            stringRedisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + cleanToken, REVOKED_VALUE, 3600, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String cleanToken = token.regionMatches(true, 0, "Bearer ", 0, 7)
                ? token.substring(7).trim()
                : token.trim();

        if (!StringUtils.hasText(cleanToken)) {
            return false;
        }

        Boolean exists = stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + cleanToken);
        return Boolean.TRUE.equals(exists);
    }
}

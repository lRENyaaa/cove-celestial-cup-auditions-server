package top.rymc.phira.main.storage.player;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import top.rymc.phira.main.data.UserInfo;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


public class VerificationCodeService {

    private final Cache<UserInfo, String> userToCodeCache;
    private final Map<String, UserInfo> codeToUserMap = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public VerificationCodeService() {
        userToCodeCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .removalListener((UserInfo userId, String code, RemovalCause cause) -> {
                    if (code != null) {
                        codeToUserMap.remove(code);
                    }
                })
                .build();
    }

    public synchronized String generateOrGetCode(UserInfo userId) {
        String existingCode = userToCodeCache.getIfPresent(userId);
        if (existingCode != null) {
            return existingCode;
        }

        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (codeToUserMap.containsKey(code));

        userToCodeCache.put(userId, code);
        codeToUserMap.put(code, userId);

        return code;
    }

    public Optional<UserInfo> peekUserByCode(String code) {
        return Optional.ofNullable(codeToUserMap.get(code));
    }

    public synchronized Optional<UserInfo> validateAndConsume(String inputCode) {
        UserInfo userId = codeToUserMap.remove(inputCode);
        if (userId != null) {
            userToCodeCache.invalidate(userId);
            return Optional.of(userId);
        }
        return Optional.empty();
    }
}


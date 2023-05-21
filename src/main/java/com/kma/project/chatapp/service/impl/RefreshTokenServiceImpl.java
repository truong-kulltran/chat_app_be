package com.kma.project.chatapp.service.impl;

import com.kma.project.chatapp.dto.request.auth.TokenRefreshRequest;
import com.kma.project.chatapp.dto.response.auth.TokenRefreshResponse;
import com.kma.project.chatapp.entity.RefreshToken;
import com.kma.project.chatapp.entity.UserEntity;
import com.kma.project.chatapp.exception.AppException;
import com.kma.project.chatapp.handler.TokenRefreshException;
import com.kma.project.chatapp.repository.RefreshTokenRepository;
import com.kma.project.chatapp.repository.UserRepository;
import com.kma.project.chatapp.security.jwt.JwtUtils;
import com.kma.project.chatapp.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Autowired
    JwtUtils jwtUtils;
    @Value("${viet.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Value("${viet.app.jwtExpirationMs}")
    private int jwtExpirationMs;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.builder().errorCodes(Collections.singletonList("error.username-not-found")).build());
        refreshToken.setUser(userEntity);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(LocalDateTime.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }

        return token;
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest refreshRequest) {
        String requestRefreshToken = refreshRequest.getRefreshToken();

        return findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = "Bearer " + jwtUtils.generateTokenFromUsername(user.getUsername());
                    Date expiredToken = new Date((new Date()).getTime() + jwtExpirationMs);
                    LocalDateTime localDate = expiredToken.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    return new TokenRefreshResponse(token, requestRefreshToken, localDate.toString());
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken,
                        "Refresh token is not in database!"));
    }
}

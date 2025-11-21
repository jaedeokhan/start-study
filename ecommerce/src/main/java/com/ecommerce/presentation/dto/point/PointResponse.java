package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.user.User;

import java.time.LocalDateTime;

public record PointResponse (
    Long userId,
    long pointBalance,
    LocalDateTime lastUpdatedAt
) {
    public static PointResponse from(User user) {
        return new PointResponse(
            user.getId(),
            user.getPointBalance(),
            user.getUpdatedAt()
        );
    }
}

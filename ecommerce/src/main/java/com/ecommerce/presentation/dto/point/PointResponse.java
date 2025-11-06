package com.ecommerce.presentation.dto.point;

import com.ecommerce.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PointResponse {
    private Long userId;
    private long pointBalance;
    private LocalDateTime lastUpdatedAt;

    public static PointResponse from(User user) {
        return new PointResponse(
            user.getId(),
            user.getPointBalance(),
            user.getUpdatedAt()
        );
    }
}

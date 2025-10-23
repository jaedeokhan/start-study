package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getPointById(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getHistoriesById(long userId) {
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
        return pointHistories;
    }

    public UserPoint charge(long userId, long chargeAmount) {
        UserPoint chargedUserPoint = userPointTable.selectById(userId).charge(chargeAmount);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(
                chargedUserPoint.id(),
                chargedUserPoint.point()
        );

        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        return savedUserPoint;
    }

    public UserPoint use(long userId, long useAmount) {
        UserPoint usedUserPoint = userPointTable.selectById(userId).use(useAmount);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(
                usedUserPoint.id(),
                usedUserPoint.point()
        );

        pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());

        return savedUserPoint;
    }
}

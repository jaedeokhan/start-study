package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
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

    public UserPoint charge(long userId, long amount) {
        UserPoint chargedUserPoint = userPointTable.selectById(userId).charge(amount);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(
                chargedUserPoint.id(),
                chargedUserPoint.point()
        );

        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return savedUserPoint;
    }

    public UserPoint use() {

        return new UserPoint(0, 0, 0);
    }
}

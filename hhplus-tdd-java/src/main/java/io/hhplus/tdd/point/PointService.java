package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public UserPoint getPointById(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getHistoriesById(long userId) {
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
        return pointHistories;
    }

    public UserPoint charge(long userId, long chargeAmount) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        synchronized(lock) {
            UserPoint userPoint = userPointTable.selectById(userId);
            UserPoint chargedPoint = userPoint.charge(chargeAmount);

            UserPoint savedUserPoint = userPointTable.insertOrUpdate(
                    chargedPoint.id(),
                    chargedPoint.point()
            );

            pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
            return savedUserPoint;
        }
    }

    public UserPoint use(long userId, long useAmount) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        synchronized(lock) {
            UserPoint userPoint = userPointTable.selectById(userId);
            UserPoint usedUserPoint = userPoint.use(useAmount);

            UserPoint savedUserPoint = userPointTable.insertOrUpdate(
                    usedUserPoint.id(),
                    usedUserPoint.point()
            );

            pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());
            return savedUserPoint;
        }
    }
}

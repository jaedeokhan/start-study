package redis.lettuce.string;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

public class RedisLettuceStringIncr {

    @Test
    public void incrDecr() {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withDatabase(0) // 0~15
                .build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> redisCommands = connection.sync();

        String key = "lettuce:incr";
        String value = "hello world";

//        redisCommands.set(key, value);
        redisCommands.flushdb();

        // incr, decr
        Long incr = redisCommands.incr(key);
        System.out.println("incr = " + incr);

        Long decr = redisCommands.decr(key);
        System.out.println("decr = " + decr);

        // 1씩 증감, 감소가 아닌 원하는 값 설정
        // incrby, decrby
        Long incrby = redisCommands.incrby(key, 10);
        System.out.println("incrby = " + incrby);

        Long decrby = redisCommands.decrby(key, 20);
        System.out.println("decrby = " + decrby);

        connection.close();
        redisClient.shutdown();
    }
}

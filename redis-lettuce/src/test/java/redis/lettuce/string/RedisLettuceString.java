package redis.lettuce.string;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

public class RedisLettuceString {
    // string
    // set, get, mset, mget
    // incr, decr

    @Test
    public void setGet() {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withDatabase(0) // 0~15
                .build();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> redisCommands = connection.sync();

        String key = "lettuce:string";
        String value = "hello world";

        redisCommands.set(key, value);

        String returnValue = redisCommands.get(key);
        System.out.println("returnValue = " + returnValue);
    }

}

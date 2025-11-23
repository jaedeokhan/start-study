package redis.lettuce.string;

import io.lettuce.core.GetExArgs;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
        System.out.println();

        // TTL (time to live)
        System.out.println("before ttl = " + redisCommands.ttl(key));
        System.out.println();

        redisCommands.expire(key, Duration.ofMinutes(1));
        System.out.println("set expire ttl = " + redisCommands.ttl(key));
        System.out.println();

        SetArgs setArgs = SetArgs.Builder
//                .ex(90);
                .keepttl();

        redisCommands.set(key, value + "_new", setArgs);
        System.out.println("set SetArgs ttl = " + redisCommands.ttl(key));
        System.out.println();

        // redis 6.2 getDel, getEx
//        String getdel = redisCommands.getdel(key);
//        System.out.println("getdel = " + getdel);
        System.out.println("ttl = " + redisCommands.ttl(key));
        System.out.println();

        GetExArgs getExArgs = GetExArgs.Builder.ex(120);
        String getex = redisCommands.getex(key, getExArgs);
        System.out.println("getex = " + getex);
        System.out.println("ttl = " + redisCommands.ttl(key));

        connection.close();
        redisClient.shutdown();
    }


}

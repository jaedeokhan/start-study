package redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class CommandTemplate {

    public static void commandAction(CommandAction action) {
        RedisURI redisURI = getRedisUri();
        RedisClient redisClient = RedisClient.create(redisURI);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> redisCommands = connection.sync();

        action.doInExecute(redisCommands);

        connection.close();
        redisClient.shutdown();
    }

    private static RedisURI getRedisUri() {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withDatabase(0) // 0~15
                .build();
        return redisURI;
    }
}

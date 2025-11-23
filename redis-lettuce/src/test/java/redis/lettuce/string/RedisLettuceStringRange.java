package redis.lettuce.string;

import org.junit.jupiter.api.Test;
import redis.lettuce.CommandAction;
import redis.lettuce.CommandTemplate;

public class RedisLettuceStringRange {

    @Test
    public void substring() {
        CommandAction action = (redisCommands -> {
            String key = "lettcue:string";
            String value = "hello";

            redisCommands.set(key, value);

            String get = redisCommands.get(key);
            System.out.println("get = " + get);
        });
        CommandTemplate.commandAction(action);
    }
}

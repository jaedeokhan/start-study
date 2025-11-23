package redis.lettuce.string;

import org.junit.jupiter.api.Test;
import redis.lettuce.CommandAction;
import redis.lettuce.CommandTemplate;

public class RedisLettuceStringIncr {

    @Test
    public void incrDecr() {
        CommandAction action = (redisCommands -> {
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
        });
        CommandTemplate.commandAction(action);
    }
}

package io.gridgo.redis.command.hyperloglog;

import org.joo.promise4j.Promise;

import io.gridgo.bean.BElement;
import io.gridgo.bean.BObject;
import io.gridgo.redis.RedisClient;
import io.gridgo.redis.command.AbstractRedisCommandHandler;
import io.gridgo.redis.command.RedisCommand;
import io.gridgo.redis.command.RedisCommands;

@RedisCommand(RedisCommands.PFADD)
public class RedisPfaddHandler extends AbstractRedisCommandHandler {

    public RedisPfaddHandler() {
        super("key", "value");
    }

    @Override
    protected Promise<BElement, Exception> process(RedisClient redis, BObject options, BElement[] params) {
        return redis.pfadd(params[0].asValue().getRaw(), extractListBytesFromSecond(params));
    }

}

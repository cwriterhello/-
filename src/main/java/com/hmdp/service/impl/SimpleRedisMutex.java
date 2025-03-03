package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.service.ILock;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisMutex implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    //为什么要使用uuid作为线程前缀：为了保证线程id的唯一性。若是在两个机器上创建锁，线程可能id可能恰好一样
    private static final String ID_PREFIX = UUID.randomUUID().toString()+"-";
    public SimpleRedisMutex(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeOutSec) {
        String id =ID_PREFIX + Thread.currentThread().getId();
        Boolean sucess = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name,id,timeOutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(sucess);
    }

    @Override
    public void unLock() {
        //当前线程id
        String id =ID_PREFIX + Thread.currentThread().getId();
        //锁中的id
        String id1 = stringRedisTemplate.opsForValue().get(KEY_PREFIX+name);
        if (id.equals(id1)) {
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}

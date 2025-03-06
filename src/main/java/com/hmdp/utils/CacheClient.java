package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate s) {
        this.stringRedisTemplate = s;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long expire, TimeUnit unit) {
        //2创建过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expire)));
        //3写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFullBack, Long expire, TimeUnit unit) {
        String key = keyPrefix + id;
        //1从redis中查询商铺信息
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if (json != null){
            return null;
        }
        R r = dbFullBack.apply(id);
        if (r == null){
            this.set(key,"",expire,unit);
            return null;
        }
        this.set(key,r,expire,unit);
        return r;
    }


//    public Shop queryWithLogicalExpire(String key) {
//        String key = CACHE_SHOP_KEY + id;
//        String lockKey = LOCK_SHOP_KEY + id;
//        //1从redis中查询商铺信息
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //redis中没有返回null
//        if (StrUtil.isBlank(shopJson)) {
//            return null;
//        }
//        //有数据，判断是否过期
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        int i = LocalDateTime.now().compareTo(redisData.getExpireTime());
//        if (i < 0) {
//            //数据没有过期直接返回
//            return shop;
//        }
//        Boolean lock = tryLock(lockKey);
//        //如果拿到锁开启缓存重建
//        if (lock) {
//            //拿到则开启独立线程
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    //查询数据库并更新redis数据
//                    this.saveShop2Redis(id, 30L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unlock(LOCK_SHOP_KEY + id);
//                }
//            });
//            return null;
//        }
//        return shop;
//    }
}

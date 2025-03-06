package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存穿透查询
     * @param id
     * @return
     */
    @Override
    @Transactional
    public Result queryById(Long id) {
//        String key = "cache:shop:"+id;
//        String huChiKey = "lock:shop:"+id;
//        ////流程图文字化
//        //1从redis中查询商铺信息
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2判断商铺信息是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
//            return Result.ok(shop);
//        }
//        if (Objects.equals(shopJson, "")){
//            return Result.fail("数据不存在");
//        }
//        Boolean lock = tryLock(huChiKey);
//
//        //创建互斥锁
//        Shop shop = null;
//        try {
//            if (!lock){
//                //获取互斥锁失败，休眠并重复
//                Thread.sleep(50);
//               return queryById(id);
//            }
//            //3如果不存在查询数据库
//            shop = getById(id);
//            //4判断数据库信息是否存在
//            if (shop == null){
//                //5如果不存在，返回错误信息.额外：避免缓存穿透
//                stringRedisTemplate.opsForValue().set(key,"",2L, TimeUnit.MINUTES);
//                return Result.fail("店铺不存在");
//            }
//            //6将查询到的店铺信息缓存到redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            unlock(huChiKey);
//        }
//        //7返回商品信息
//        return Result.ok(shop);

        //解决缓存击穿
        return queryWithLogicExpire(id);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("id不能为空");
        }
        //先修改数据库再删除redis数据
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }


    //获取互斥锁
    private Boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",50L,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //移除互斥锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 使用逻辑过期时间解决缓存击穿问题
     */

    //创建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public Result queryWithLogicExpire(Long id){
        String key = CACHE_SHOP_KEY+id;
        String lockKey = LOCK_SHOP_KEY+id;
        //1从redis中查询商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //redis中没有返回null
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        //有数据，判断是否过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject)redisData.getData(), Shop.class);
        int i = LocalDateTime.now().compareTo(redisData.getExpireTime());
        if (i<0){
            //数据没有过期直接返回
            return Result.ok(shop);
        }
        Boolean lock = tryLock(lockKey);
        //如果拿到锁开启缓存重建
        if (lock){
            //拿到则开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库并更新redis数据
                    this.saveShop2Redis(id,30L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(LOCK_SHOP_KEY+id);
                }
            });
            return null;
        }
        return Result.ok(shop);
    }

    /**
     * 热点key预热
     * @param id
     */
    public void saveShop2Redis(Long id,Long expireSeconds){
        //1查询shop信息
        Shop shop = getById(id);
        //2创建过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

}

package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryType() {
        //1从redis中查询
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY);
        //2判断redis中是否存在
        if(shopJson != null){
            //存在直接返回
            return Result.ok(JSONUtil.toList(shopJson,ShopType.class));
        }
        //3不存在就查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //4数据库中是否存在
        if (typeList == null){
            //不存在就返回错误信息
            return Result.fail("店铺不存在");
        }
        //4存在就将店铺信息保存至redis
        shopJson = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY,shopJson,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //5返回店铺信息
        return Result.ok(typeList);
    }
}

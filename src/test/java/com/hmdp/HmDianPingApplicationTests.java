package com.hmdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.entity.User;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private StringRedisTemplate StringRedisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedis() throws JsonProcessingException {
        User user = new User();
        user.setId(1L);

        //把对象序列化为字符串
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonUser = objectMapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set("User:100",jsonUser);

        //把字符串反序列化为对象
        String us = stringRedisTemplate.opsForValue().get("User:100");
        User user1 = objectMapper.readValue(us,User.class);
        System.out.println(user1);
    }

    @Resource
    private ShopServiceImpl service;
    @Test
    public void testSaveShop(){
        service.saveShop2Redis(1L,30L);
    }

}

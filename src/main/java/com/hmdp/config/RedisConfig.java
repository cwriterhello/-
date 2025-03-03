package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    //@Bean

    /**
     * redis默认将对象序列化为字符数组，
     * 1：RedisTemple,通过工厂修改RedisTemple的序列化器为GenericJackson2JsonRedisSerializer,存储对象时会保留对象的类路径
     * 2：StringRedisTemple,通过ObjectMapper类中的 writeValueAsString(对象) 和 readValue("key",类.class)手动序列化
     * @param redisConnectionFactory
     * @return
     */
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        //创建RedisTemple对象
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        //设置redisConnectFactory
        template.setConnectionFactory(redisConnectionFactory);
        //创建json序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        //设置key和hashkey
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        //设置val和hashvalue
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        return template;
    }
}

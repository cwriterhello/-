package com.hmdp.utils;

import jdk.vm.ci.meta.Local;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import sun.text.resources.cldr.ext.FormatData_ti;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Autowired
    private  StringRedisTemplate stringRedisTemplate;

    //redis自增id策略：31位时间戳-32位序列
    public Long netId(String preKey){

        //1时间戳=初始化时间戳-当前时间戳
        //1.1创建初始化时间戳
        LocalDateTime time = LocalDateTime.of(2002,10,27,10,10,10);
        Long second = time.toEpochSecond(ZoneOffset.UTC);
        //1.2创建当前时间戳
        LocalDateTime now = LocalDateTime.now();
        Long secondNow = now.toEpochSecond(ZoneOffset.UTC);
        //1.3得到时间戳
        Long timeStamp = secondNow - second;

        //2序列位
        //获取当前日期,精确到天
        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("inc:"+preKey+":"+format);

        //返回生成的id
        return timeStamp << 32 | count;
    }

}

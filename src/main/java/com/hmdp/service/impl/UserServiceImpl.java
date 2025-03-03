package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.deser.BuilderBasedDeserializer;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1判断手机号是否合法
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2如果不合法返回错误信息
        if (!phoneInvalid){
            return Result.fail("手机号不合法");
        }
        //3生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4保存验证码
        session.setAttribute("code",code);

//        //TODO 4保存验证码到redis ！优化
//        stringRedisTemplate.opsForValue().set(,code);

        //5发送验证码
        log.debug("发送验证码成功,验证码："+code);
        //6返回结果
        return  Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //1手机号是否合法
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2手机号不合法
        if (!phoneInvalid){
            return Result.fail("手机号错误");
        }
        //3校验验证码
        String scode = (String)session.getAttribute("code");
        String code = loginForm.getCode();
        //4验证码不一致
        if (!scode.equals(code)){
            return Result.fail("验证码错误");
        }
        //5查询手机号用户
        User user = query().eq("phone",phone).one();
        //6手机号用户不存在，创建用户并保存到数据库
        if (user == null){
            user = createUserByPhone(phone);
        }
        //7保存用户信息到session
        session.setAttribute("user",user);
        return Result.ok(session);
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        return user;
    }
}

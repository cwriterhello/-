package com.hmdp.interceptor;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    //这里应为LoginInterceptor不被spring接管，所以要使用构造函数，将stringRedisTemplate传进来
    private StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate s) {
        this.stringRedisTemplate = s;
    }

    //这段代码的主要目的是在处理请求之前，确保用户已经登录，
    // 如果未登录则拦截请求并返回相应的错误信息。
    // 如果用户已登录,则将用户信息存储以便后续使用
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response , Object handler ){

        String token = request.getHeader("authorization");
        //根据token从redis中获取user对象
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        //判断用户是否存在
        if (entries.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        //刷新token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

}

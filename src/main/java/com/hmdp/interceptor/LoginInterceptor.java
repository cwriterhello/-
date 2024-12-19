package com.hmdp.interceptor;


import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response , Object handler ){
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }
        //获取session
        HttpSession session = request.getSession();
        //从session中获取用户
        User user =(User) session.getAttribute("user");
        //判断用户是否存在
        if (user == null){
            //不存在，拦截返回错误信息
            response.setStatus(401);
            return false;
        }
        //存在将用户保存到ThreadLocal
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setIcon(userDTO.getIcon());
        userDTO.setNickName(user.getNickName());
        UserHolder.saveUser(userDTO);
        return true;
    }

}

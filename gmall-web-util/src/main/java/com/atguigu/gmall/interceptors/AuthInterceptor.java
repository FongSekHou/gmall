package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码

        //获取拦截方法中是否有带注解LoginRequired
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired loginRequired = hm.getMethodAnnotation(LoginRequired.class);
        if (loginRequired == null) {//如果不带注解LoginRequired，直接放行
            return true;
        }
        //以当前最新的token为验证标准，如果cookie和url里的token都是空的话那token也为空
        String token = "";
        String oldToken = CookieUtil.getCookieValue(request, "userToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if(StringUtils.isNotBlank(token)){//如果token为空无需httpclient验证，直接失败
            String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip，本项目没用nginx，无需通过该方式获得端口号
            ip = request.getRemoteAddr();// 从request中获取ip，如果ip为空要做异常处理，记录日志之类的

            String successJson  = HttpclientUtil.doGet("http://127.0.0.1:8085/verify?token=" + token + "&ip=" + request.getRemoteAddr());// 调用认证中心进行验证
            successMap = JSON.parseObject(successJson,Map.class);
            success = successMap.get("status");
        }

        if (loginRequired.loginSuccess()) {
            // 必须登录成功才能使用
            if (!success.equals("success")) {//登录失败
                //重定向会passport登录
                response.sendRedirect("http://127.0.0.1:8085/index?returnUrl=" + request.getRequestURL());
                return false;
            }

            // 需要将token携带的用户信息写入
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //验证通过，覆盖cookie中的token
            if(StringUtils.isNotBlank(token)){
                CookieUtil.setCookie(request,response,"userToken",token,60*60*2,true);
            }

        } else {
            // 没有登录也能用，但是必须验证
            if (success.equals("success")) {
                // 需要将token携带的用户信息写入
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));

                //验证通过，覆盖cookie中的token
                if(StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"userToken",token,60*60*2,true);
                }

            }
        }

        return true;
    }

}

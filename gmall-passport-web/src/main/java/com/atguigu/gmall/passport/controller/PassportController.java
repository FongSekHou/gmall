package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.constant.WebConst;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("/index")
    public String index(String returnUrl, ModelMap map) {
        map.put("returnUrl", returnUrl);
        return "index";
    }

    @RequestMapping("/login")
    @ResponseBody
    public String login(String username, String password, HttpServletRequest request) {
        UmsMember user = userService.getUserForLogin(username, password);
        String token;
        if (user != null) {//登录成功
            Map<String, Object> map = new HashMap<>();
            String memberId = user.getId();
            map.put("id", memberId);
            map.put("nickname", user.getNickname());
            String ip = request.getRemoteAddr();//获得ip地址
            //使用jwt制作令牌
            token = JwtUtil.encode(WebConst.getServerSecretCode(), map, ip);
            userService.saveUserToken(token, memberId);
        } else {//登录失败将token设为fail，可用常量类封装
            token = "fail";
        }
        return token;
    }

    @RequestMapping("/verify")
    @ResponseBody
    public Map<String, String> verify(String token, String ip) {
        Map<String, String> map = new HashMap<>();
        Map<String, Object> map1 = JwtUtil.decode(token, WebConst.getServerSecretCode(), ip);//只要jwt解密成功就证明登录成功
        if (map1 != null) {
            String memberId = (String) map1.get("id");
            String nickname = (String) map1.get("nickname");
            map.put("status", "success");
            map.put("memberId", memberId);
            map.put("nickname", nickname);
        } else {
            map.put("status", "fail");
        }
        return map;
    }

    /**
     * 第三方登录的地址
     * 该方法无法实现returnUrl的功能，因为第三方登录涉及到第三方的登录页面和服务器，returnUrl无法经过第三方的页面和服务传递到后台，也无法传给前台让前台重定向
     */
    @RequestMapping("/vlogin")
    public String vlogin(String code,HttpServletRequest request) {
        //1.发请求获得登陆页面，在页面中完成  https://api.weibo.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI

        //2.进入该方法获得授权码

        //3.根据授权码获得access_token,access_token是隐私的，要使用post请求获取
        //https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        Map<String, String> map = new HashMap<>();
        map.put("client_id",WebConst.getWeiboLoginClientID());
        map.put("client_secret",WebConst.getWeiboLoginClientSecret());
        map.put("redirect_uri",WebConst.getVloginRedirectURI());
        map.put("code",code);
        String accessTokenJson = HttpclientUtil.doPost(WebConst.getWeiboLoginAccessTokenURL(), map);
        if(accessTokenJson==null){//若是授权失败则直接重定向回登录页面
            return "redirect:http://127.0.0.1:8085/index";
        }
        Map<String,String> accessTokenMap = JSON.parseObject(accessTokenJson, Map.class);//返回的accessTokenMap里详细的参数内容前往微博开发者中心的api查看
        String uid = accessTokenMap.get("uid");
        String accessToken = accessTokenMap.get("access_token");
        //4.根据access_token获得用户信息
        String userInfoJson = HttpclientUtil.doGet(WebConst.getWeiboLoginUserInfoURL(accessToken, uid));
        Map<String,String> userInfo = JSON.parseObject(userInfoJson, Map.class);//返回的userInfoMap里详细的参数内容前往微博开发者中心的api查看
        //5.根据第三方uid去数据库查询是否曾经登陆过
        UmsMember umsMember = userService.getUserForSocialLogin(uid,WebConst.getWeiboSourceType());
        //6.已登录：制作token
        //7.未登录：将用户插入数据库并制作token
        if(umsMember==null){//未曾登录过，从userInfoMap中取数据插入mysql中
            umsMember = new UmsMember();
            umsMember.setSourceType(WebConst.getWeiboSourceType());
            umsMember.setAccessCode(code);
            umsMember.setAccessToken(accessToken);
            umsMember.setCity(userInfo.get("location"));
            umsMember.setNickname(userInfo.get("screen_name"));
            umsMember.setSourceUid(uid);
            int g = 0;
            String gender = userInfo.get("gender");
            if(gender.equals("m")){
                g = 1;
            }
            umsMember.setGender(g);

        }
        Map<String,Object> map1 = new HashMap<>();
        map1.put("memberId",umsMember.getId());
        map1.put("nickname",umsMember.getNickname());
        String token = JwtUtil.encode(WebConst.getServerSecretCode(),map1,request.getRemoteAddr());
        userService.saveUserToken(token,umsMember.getId());
        //8.重定向至search模块的index，前往该页面时会通过拦截器，会将token写入cookie
        return "redirect:http://127.0.0.1:8083/index?token="+token;
    }

    /**
     * 退出登录功能，将cookie删除即可
     * https://api.weibo.com/oauth2/revokeoauth2 授权回收接口，帮助开发者主动取消用户的授权。
     * @return
     */
    @RequestMapping("/logout")
    @ResponseBody
    public Object logout() {

        return null;
    }
}

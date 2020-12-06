package com.atguigu.gmall.constant;

public class WebConst {

    /**
     * 返回服务器端的密码，用于制作jwt令牌，可更改
     *
     * @return
     */
    public static final String getServerSecretCode() {
        return "www.gmall.com";
    }

    /**
     * 微博登录时用授权码换access_token的地址
     * @return
     */
    public static final String getWeiboLoginAccessTokenURL() {
        return "https://api.weibo.com/oauth2/access_token?";
    }

    /**
     * 向微博申请接入微博验证服务的客户端id
     */
    public static final String getWeiboLoginClientID() {
        return "3435275118";
    }

    /**
     * 向微博申请接入微博验证服务的客户端密码
     *
     * @return
     */
    public static final String getWeiboLoginClientSecret() {
        return "9570950210a2aaabcd48e0e13e912f22";
    }

    /**
     * 微博验证成功后，返回授权码的地址（本项目对外提供的接口，用于获取验证码以及执行其余的社交登录步骤）
     *
     * @return
     */
    public static final String getVloginRedirectURI() {
        return "http://127.0.0.1:8085/vlogin";
    }

    /**
     * 使用accesstoken和uid去微博获取用户在微博的信息
     *
     * @param accessToken
     * @param uid
     * @return
     */
    public static final String getWeiboLoginUserInfoURL(String accessToken, String uid) {
        return "https://api.weibo.com/2/users/show.json?access_token=" + accessToken + "&uid=" + uid;
    }

    public static final Integer getWeiboSourceType(){
        return 1;
    }
}

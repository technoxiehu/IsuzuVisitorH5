package com.netease.qiye.api.account;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 更新密码
 *
 * @author NetEase
 */
public class UpdatePassword extends TokenKeeper {

    public static final String apiPath = "/api/open/account/updatePassword";

    public static final String accessToken = "caecdc404e1044468b22e939b84e15b1";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "jy_pop")
                //首次登录修改密码,0-不需要，1-web登录需要，客户端不需要，2-web登录需要且未改密码前客户端不能登录
                .addParam("passChangeFirstLogin", 1)
                //密码类型，0 原文，1 md5的hex编码
                .addParam("passType", 0)
                .addParam("password", "123456.")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


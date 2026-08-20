package com.netease.qiye.api.account;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 更新账号登录权限
 *
 * @author NetEase
 */
public class UpdateAccountLoginPerm extends TokenKeeper {

    public static final String apiPath = "/api/open/account/updateAccountLoginPerm";

    public static final String accessToken = "caecdc404e1044468b22e939b84e15b1";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "jy")
                //登录权限名,web、flashmail、yixin、imap、pop、smtp
                .addParam("loginType", "web")
                //登录权限值,0-无法使用，1-可以使用
                .addParam("loginPerm", 1);

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


package com.netease.qiye.token;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.SsoLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

public class SsoAuthToken extends TokenKeeper {

    // 保密，授权码
    public static final String authCode = "YoC82sXw";

    public static void main(String[] args) {
        QiyeOpenPlatSDK instance = getInstance();
        R<SsoLoginResp> ssoLoginRespR = instance.ssoAuthToken(authCode);
        /**
         * {
         *     "data": {
         *         "ssoAuthToken": "3E4bM4pLs/n63ps05nW1MvdvAq5G7ELvExNRP2JLel7uZYO11Wl6P5E83vUYQ8c7",
         *         "ssoAuthTokenExpiredTime": "2021-12-01 13:25:08"
         *     },
         *     "success": true,
         *     "message": "",
         *     "code": 0
         * }
         */
        // 用于 sso 的获取单点登录签名链接构造
        String ssoAuthToken = ssoLoginRespR.getDataBean(SsoLoginResp.class).getSsoAuthToken();


    }

}

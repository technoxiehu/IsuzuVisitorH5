package com.netease.qiye.token;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.constant.ResultEnum;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginReq;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDKConfig;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 应用登录，获取token 获取token
 *
 * @author NetEase
 * @createtime 2021/11/22
 */
public class AcquireToken extends TokenKeeper {

    // 保密，授权码
    public static final String authCode = "YoC82sXw";

    public static void main(String[] args) {

        QiyeOpenPlatSDK instance = getInstance();

        // 查看文档，应用获取token
        R<AppLoginResp> appLoginR = instance.appLogin(authCode);

        // 复制access com.netease.qiye.token 和 refresh com.netease.qiye.token
        System.out.println(appLoginR.getResponseText());

        // 如果内存中持有该实例，则可以设置 com.netease.qiye.token ,重复调用
        AppLoginResp ar = appLoginR.getDataBean(AppLoginResp.class);
        instance.getQiyeOpenPlatSDKConfig().setupToken(ar);

    }
}

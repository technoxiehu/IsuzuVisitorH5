package com.netease.qiye.token;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 应用刷新当前token
 *
 * @author NetEase
 * @createtime 2021/11/22
 */
public class RefreshToken extends TokenKeeper {

    public static final String accessToken = "e438051805cc40e4853a6c6901d7deeb";
    public static final String refreshToken = "7f3ed4af0cc8491688138f86d00e2a0a";

    public static void main(String[] args) {

        QiyeOpenPlatSDK instance = getInstance();
        instance.getQiyeOpenPlatSDKConfig()
                //调用前设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
                //重点是 refresh com.netease.qiye.token
                .setupToken(new AppLoginResp(accessToken, refreshToken, null, null));

        //sh
        R r = instance.refreshToken();
        // 返回当前refresh com.netease.qiye.token 以及 新的 access com.netease.qiye.token
        System.out.println(r);

        // 如果内存中持有该实例，则可以设置 com.netease.qiye.token ,重复调用
        // instance.getQiyeOpenPlatSDKConfig().setupToken((AppLoginResp) r.getDataBean(AppLoginResp.class));
    }
}

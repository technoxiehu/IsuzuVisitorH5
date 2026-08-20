package com.netease.qiye.api.mailaccount;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 获取帐号收发限制
 *
 * @author NetEase
 */
public class GetMailAccount extends TokenKeeper {

    public static final String apiPath = "/api/open/mailaccount/getMailAccount";

    public static final String accessToken = "8a69178d71c6421795e5d1df459a1090";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "testruan")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


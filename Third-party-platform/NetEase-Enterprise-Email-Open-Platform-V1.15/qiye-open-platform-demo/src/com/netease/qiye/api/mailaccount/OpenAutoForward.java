package com.netease.qiye.api.mailaccount;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 添加自动转发邮箱
 *
 * @author NetEase
 */
public class OpenAutoForward extends TokenKeeper {

    public static final String apiPath = "/api/open/mailaccount/openAutoForward";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("accountName", "test111111223")
                //邮箱串，英文逗号拼接
                .addParam("forwardAddress", "abc@163.com,cxy@163.com")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


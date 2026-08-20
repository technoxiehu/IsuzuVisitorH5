package com.netease.qiye.api.mobile;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 绑定手机号码
 *
 * @author NetEase
 */
public class AddMobile extends TokenKeeper {

    public static final String apiPath = "/api/open/mobile/addMobile";

    public static final String accessToken = "0dff48acb5fc4478aaf4cd445106e3f5";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "1515")
                .addParam("mobile", "08088182958")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


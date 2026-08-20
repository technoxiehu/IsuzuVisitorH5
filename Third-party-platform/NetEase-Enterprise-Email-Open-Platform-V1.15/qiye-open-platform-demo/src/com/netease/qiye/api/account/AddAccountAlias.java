package com.netease.qiye.api.account;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 添加帐号别名
 *
 * @author NetEase
 */
public class AddAccountAlias extends TokenKeeper {

    public static final String apiPath = "/api/open/account/addAccountAlias";

    public static final String accessToken = "bf5ac4d9614c464dafe913e0a8773d87";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("accountName", "zhangsan")
                .addParam("aliasDomain", "qzwy.club")
                .addParam("aliasName", "zs01");

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


package com.netease.qiye.api.account;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

public class UpdateAccount extends TokenKeeper {

    public static final String apiPath = "/api/open/account/updateAccount";

    public static final String accessToken = "81c4e6f9922d4fac97dc0d3c7ee5d645";

    public static void main(String[] args) {

        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "jy_imap")
                //是否企业通讯录中显示 0 否
                .addParam("visibleInAddr", 0)
                .addParam("jobNumber", "111")
                .addParam("bindmobile", "17681872239")
                .addParam("name", "abc")
                //部门ID
                .addParam("unitId", "default")
                .addParam("mobilePrefix", "62")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);

        System.out.println(r);
    }
}



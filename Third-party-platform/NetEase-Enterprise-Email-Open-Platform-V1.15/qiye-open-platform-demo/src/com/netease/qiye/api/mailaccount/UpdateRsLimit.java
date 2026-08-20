package com.netease.qiye.api.mailaccount;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 更新帐号邮件收发限制
 *
 * @author NetEase
 */
public class UpdateRsLimit extends TokenKeeper {

    public static final String apiPath = "/api/open/mailaccount/updateRsLimit";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("accountName", "test111111223")
                //邮件收发限制类型,17-只能发送域内邮件，收信无限制 18-只能接受域内邮件，发信无限制 19-只能收发域内邮件 -1-无限制
                .addParam("rsLimitType", 17)
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


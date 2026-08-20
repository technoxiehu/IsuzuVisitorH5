package com.netease.qiye.api.push;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 查询动态订阅
 *
 * @author NetEase
 */
public class GetDynamicSubscribe extends TokenKeeper {

    public static final String apiPath = "/api/open/push/getDynamicSubscribe";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        QiyeOpenPlatSDK instance = getInstance();


        //根据文档添加参数
        Q q = Q.init(null)
                //本企业的OrgOpenId
                .addParam("orgOpenId", instance.getQiyeOpenPlatSDKConfig().getOrgOpenId())
                //推送消息类型,推送消息类型（新邮件到达为pushmail、组织变更orgevent）,默认为：pushmail
                .addParam("source", "pushmail")
                ;

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


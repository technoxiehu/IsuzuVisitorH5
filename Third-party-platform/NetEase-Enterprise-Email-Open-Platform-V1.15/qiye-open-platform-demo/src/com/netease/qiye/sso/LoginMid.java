package com.netease.qiye.sso;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

import java.util.Map;

/**
 * 单点登录至指定邮件，支持极速6及以上版本
 */
public class LoginMid extends TokenKeeper {

    public static final String ssoAuthToken = "kfS9xBRfRWNpf3pp1k8tMccFYrmTiGJdkLO3znHSm5dciUKSmoJbK8l7q2/UXxZ9";

    public static void main(String[] args) {

        String accountName = "zhangsan";
        String domain = "abc.com";
        Q q = Q.init(null)
                .addParam("domain", domain)
                .addParam("accountName", accountName)
                .addHeader("qiye-sso-auth-token", ssoAuthToken);

        R<Map> mapR = getInstance().commonInvoke(q, "/api/sso/ssoSign");
        String ssoSign = (String) mapR.getData().get("sign");
        String endpoint=(String) mapR.getData().get("endpoint");
        String mid = "ABsAMADBE3Y13ePXA4VlNaol";//通过/mailbox/listMessages接口，获取对应邮件的id
 
        String url = endpoint + "?sso_token=" + ssoSign + "&mid=" + mid;
        System.out.print(url);
    }
}

package com.netease.qiye.sso;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 单点登录至指定邮件，支持极速6及以上版本
 */
public class LoginFolder extends TokenKeeper {

    public static final String ssoAuthToken = "DL7L1ShSyZqjmTz+GBfLo+FMHM8UrEWjLsbCZ79gF2XqWjdfpDh01ATFehWhn0RW";

    public static void main(String[] args) throws UnsupportedEncodingException {
        //企业对应的私钥
        String accountName = "zhangsan";
        String domain = "abc.com";
        Q q = Q.init(null)
                .addParam("domain", domain)
                .addParam("accountName", accountName)
                .addHeader("qiye-sso-auth-token", ssoAuthToken);

        R<Map> mapR = getInstance().commonInvoke(q, "/api/sso/ssoSign");
        String ssoSign = (String) mapR.getData().get("sign");
        String endpoint=(String) mapR.getData().get("endpoint");
        String module = "mbox.ListModule|{\"fid\":1,\"order\":\"date\",\"desc\":true}";

        String url = endpoint + "?sso_token=" + ssoSign + "&module=" + URLEncoder.encode(module, "utf-8");
        System.out.print(url);
    }
}

package com.netease.qiye.sso;


import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.constant.ResultEnum;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

import java.util.Map;

public class Login extends TokenKeeper {

    public static final String ssoAuthToken = "aw8NtMqO4A8TXQCN5iEHWZJyjTZO1G+WOOIFrjiYdBWUtD6s8AlGZJy62Ffa5C9n";

    public static void main(String[] args) {

        String accountName = "jytest01";
        String domain = "hmail.my163mail.com";
        Q q = Q.init(null)
                .addParam("domain", domain)
                .addParam("accountName", accountName)
                .addHeader("qiye-sso-auth-token", ssoAuthToken);

        R<Map> mapR = getInstance().commonInvoke(q, "/api/sso/ssoSign");
        if (!mapR.getCode().equals(ResultEnum.SUCCESS.getCode())) {
        	throw new RuntimeException("调用单点登录异常，" + mapR.getCode() + ":" + mapR.getMessage());
        	}
        String ssoSign = (String) mapR.getData().get("sign");
        String endpoint = (String) mapR.getData().get("endpoint");
        String lang = "0";

        String url = endpoint + "?sso_token=" + ssoSign + "&lang=" + lang;
        System.out.println(url);

    }

}


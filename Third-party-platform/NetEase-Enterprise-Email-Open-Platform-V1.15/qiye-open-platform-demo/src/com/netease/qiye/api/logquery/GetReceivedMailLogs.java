package com.netease.qiye.api.logquery;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 获取收信日志
 *
 * @author NetEase
 */
public class GetReceivedMailLogs extends TokenKeeper {

    public static final String apiPath = "/api/open/logquery/receivedMail";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("account","test111111223")
                //时间格式 yyyy-MM-dd HH:mm:ss
                .addParam("startTime", "2021-9-1 10:1:1")
                .addParam("endTime", "2021-10-1 10:1:1")
                .addParam("pageNum", 1)
                .addParam("pageSize", 50);
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


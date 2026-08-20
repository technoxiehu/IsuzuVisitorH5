package com.netease.qiye.api.logquery;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

import java.util.ArrayList;

/**
 * 获取web端登陆记录
 *
 * @author NetEase
 */
public class GetWebLoginLogs extends TokenKeeper {

    public static final String apiPath = "/api/open/logquery/webLogin";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("accountNames", new ArrayList<String>() {{
                    //用户账号名，邮箱格式的前缀,用于筛选查询
                    //this.add("zs");
                    //this.add("ls");

                    //WARNING 必须带上accountNames 参数, 不想过滤账户,则如下只传一项，且为空字符串
                    this.add("");
                }})
                //时间格式 yyyy-MM-dd HH:mm:ss
                .addParam("startTime", "2021-9-1 10:1:1")
                .addParam("endTime", "2021-10-1 10:1:1")
                .addParam("pageNum", 1)
                .addParam("pageSize", 50);

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


package com.netease.qiye.api.unit;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 设置部门排序值
 *
 * @author NetEase
 */
public class UpdateUnitRank extends TokenKeeper {

    public static final String apiPath = "/api/open/unit/updateUnitRank";

    public static final String accessToken = "5d12dc034d4c42d3bdcfaa2b8c72e7e9";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                //排序值,1-表示第一位，以此类推
                .addParam("rank", 0)
                //当前部门ID
                .addParam("unitId", 1973569)
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


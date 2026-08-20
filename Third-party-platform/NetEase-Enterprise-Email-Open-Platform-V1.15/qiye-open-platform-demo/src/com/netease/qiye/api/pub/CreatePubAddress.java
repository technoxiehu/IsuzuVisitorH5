package com.netease.qiye.api.pub;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 创建公共联系人
 *
 * @author NetEase
 */
public class CreatePubAddress extends TokenKeeper {

    public static final String apiPath = "/api/open/pubaddress/createPubAddress";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("email", "zhangsan@163.com")
                .addParam("birthday",1536314172)
                .addParam("company","XX公司")
                .addParam("department","XX公司")
                .addParam("fax","888888")
                .addParam("mobile","18888888888")
                .addParam("name","ccc")
                //分组ID，多个使用英文逗号分隔
                .addParam("groupIds","1,2,3")

                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        // 返回 addressId 为 公共联系人ID
        System.out.println(r);


    }
}


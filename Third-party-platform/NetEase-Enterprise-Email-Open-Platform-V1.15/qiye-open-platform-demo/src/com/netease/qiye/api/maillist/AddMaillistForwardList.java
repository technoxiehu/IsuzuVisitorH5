package com.netease.qiye.api.maillist;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 新增邮件列表成员
 *
 * @author NetEase
 */
public class AddMaillistForwardList extends TokenKeeper {

    public static final String apiPath = "/api/open/maillist/addMaillistForwardList";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                //此处的账号为邮件列表，需先创建邮件列表
                .addParam("accountName", "maillist")
                //需要增加的邮件列表成员（邮箱）,用英文逗号 , 隔开的邮件地址列表 例如\ntest1@test.com,test2@test.com
                .addParam("members", "test1@test.com,test2@test.com");

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


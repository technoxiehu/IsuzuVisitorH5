package com.netease.qiye.api.mailbox;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 获取未读邮件数量
 *
 * @author NetEase
 */
public class GetUnreadMsg extends TokenKeeper {

    public static final String apiPath = "/api/open/mailbox/getUnreadMsg";

    public static final String accessToken = "c22e4e95621d434a98742e26de91e244";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                .addParam("accountName", "zhangsan")
                //邮箱文件夹ID,使用英文半角逗号分开， 默认传1,5 : 1-收件箱;2-草稿箱;3-已发送;4-已删除;5-垃圾邮件
                .addParam("fid", "1,5");

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


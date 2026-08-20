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
public class ListMessages extends TokenKeeper {

    public static final String apiPath = "/api/open/mailbox/listMessages";

    public static final String accessToken = "17214a9f7f344cfebf32028070eaccfa";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "adc.com")
                .addParam("accountName", "zhangsan")
                //邮箱文件夹ID,使用英文半角逗号分开， 默认传1,5 : 1-收件箱;2-草稿箱;3-已发送;4-已删除;5-垃圾邮件
                .addParam("fid", "1,5")
                .addParam("pageNum", 1)
                .addParam("pageSize", 50)
                //是否返回邮件总数, 默认不返回
                .addParam("returnTotal", false)
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


package com.netease.qiye.api.maillist;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 编辑邮件列表
 *
 * @author NetEase
 */
public class UpdateMaillist extends TokenKeeper {

    public static final String apiPath = "/api/open/maillist/updateMaillist";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        //根据文档添加参数
        Q q = Q.init(null)
                .addParam("domain", "abc.com")
                //此处的账号为邮件列表
                .addParam("accountName", "maillist")
                //授权使用邮件列表成员（邮箱）,选填，mailListUserType为2或3时有效 用英文逗号 , 隔开的帐号列表 例如\nadmin@test.com,test2@test.com
                .addParam("authMembers", "")
                .addParam("name", "XX邮件列表")
                //通讯录中是否可见,限定值：1 可见/ 0 不可见
                .addParam("visibleInAddr", 0)
                //群组列表使用者类型,限定值： 0 允许所有人/ 2 允许列表中的用户和授权用户/ 3 只允许授权用户/ 4 允许域内所有用户
                .addParam("mailListUserType", 0)
                //邮件列表成员（部门）,用英文逗号 , 隔开的部门unitid 例如123,123456，与members至少存在一个
                .addParam("memberUnits", "unitid")
                //需要增加的邮件列表成员（邮箱）,用英文逗号 , 隔开的邮件地址列表 例如\ntest1@test.com,test2@test.com
                .addParam("members", "test1@test.com,test2@test.com")
                ;

        QiyeOpenPlatSDK instance = getInstance();

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);


    }
}


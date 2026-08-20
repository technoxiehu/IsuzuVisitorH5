package com.netease.qiye.api.push;

import com.netease.qiye.TokenKeeper;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * 增加动态订阅
 *
 * @author NetEase
 */
public class AddDynamicSubscribe extends TokenKeeper {

    public static final String apiPath = "/api/open/push/addDynamicSubscribe";

    public static final String accessToken = "a8591675ea2f474293a8b4c90714e01f";

    public static void main(String[] args) {

        QiyeOpenPlatSDK instance = getInstance();


        //根据文档添加参数
        Q q = Q.init(null)
                //本企业的OrgOpenId
                .addParam("orgOpenId", instance.getQiyeOpenPlatSDKConfig().getOrgOpenId())
                //公网可访问的回调接口地址, 且能正常访问 比如 http://host:port/path/handler
                .addParam("url", "http://host:port/path/handler")
                //推送消息类型,推送消息类型（新邮件到达为pushmail、组织变更orgevent）,默认为：pushmail
                .addParam("source", "pushmail")
                ;

        //设置 access com.netease.qiye.token 和 refresh com.netease.qiye.token
        instance.getQiyeOpenPlatSDKConfig().setupToken(new AppLoginResp(accessToken, null, null, null));

        R r = instance.commonInvoke(q, apiPath);
        System.out.println(r);

        //动态订阅成功之后
        //当有对应的事件触发（推送消息类型），
        //会有相关事件消息主动调用 回调地址

        //3.1 邮件到达通知
        //From	发件人
        //Subject	标题
        //To	收件人
        //Content-Type	邮件内容类型
        //Content	邮件内容(摘要)
        //mailsizebyte	邮件大小
        //SentDate	发信时间
        //charset	字符集
        //attachment	附件
        //AttachmentEx
        //样例数据
        //
        //"fileName":"ReceiveMailParam.java","offsetInMail":"3095","mimeType":"application/octet-stream","encoding":"base64","storedSize":"940"
        //
        //fileName	文件名
        //offsetInMail
        //mimeType	mime类型
        //encoding	编码
        //storedSize	大小
        //partId	附件顺序


    }
}


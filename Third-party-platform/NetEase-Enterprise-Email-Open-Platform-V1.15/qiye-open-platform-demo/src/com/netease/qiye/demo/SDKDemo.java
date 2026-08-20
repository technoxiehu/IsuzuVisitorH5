package com.netease.qiye.demo;

import com.netease.qiye.qiyeopenplatform.common.constant.ResultEnum;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginReq;
import com.netease.qiye.qiyeopenplatform.common.dto.login.AppLoginResp;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDKConfig;
import com.netease.qiye.qiyeopenplatform.sdk.dto.Q;
import com.netease.qiye.qiyeopenplatform.sdk.dto.R;

/**
 * @author NetEase
 * @createtime 2021/11/26
 */
public class SDKDemo {

    public static void main(String[] args) {

        //1、从管理员或者网易企业邮处，获取调用 对外接口的必须参数
        // appId: 应用ID
        // orgOpenId: 企业开放ID
        // authCode:  授权码 (授权码，可能会变更，建议做成配置项)

        String appId = "qy202112160922309A15";
        String orgOpenId = "cbaf8e98a11d33e8";
        String authCode = "lDkMbiqZ";

        String serverUrl = "https://api.qiye.163.com";


        //2、Java 接入 SDK
        //init config
        QiyeOpenPlatSDKConfig qiyeOpenPlatSDKConfig = QiyeOpenPlatSDKConfig.builder()
                //应用ID
                .appId(appId)
                //企业OpenID
                .orgOpenId(orgOpenId)
                //服务地址信息
                .urlPrefix(serverUrl)
                .build();

        //创建SDK实例
        QiyeOpenPlatSDK qiyeOpenPlatSDK = new QiyeOpenPlatSDK("dev", qiyeOpenPlatSDKConfig);

        //3、登录/获取token , 参考 token 模块 com.netease.qiye.token.AcquireToken
        // ?? 为什么 authCode 不在配置中存储呢？ 因为 authCode 只有在第一次、refresh token过期时使用，且authCode可能会变更
        R r0 = qiyeOpenPlatSDK.appLogin(authCode);
        System.out.println("初次获取token");
        System.out.println(r0);

        //设置 token
        qiyeOpenPlatSDK.getQiyeOpenPlatSDKConfig().setupToken((AppLoginResp) r0.getDataBean(AppLoginResp.class));


        //4、任意调用 开放接口
        //可能存在的问题:
        // 4-1、api没有权限；咨询企业管理员或者网易企业邮处，重新进行授权，并提供新的授权码 authCode
        // 4-2、authCode失效，可能的原因是：企业管理员封禁、刷新授权码；咨询管理员或者网易企业邮处，重新进行授权，并提供新的授权码 authCode
        // 4-3、appId被冻结，咨询企业管理员或者网易企业邮处
        // 4-4、accessToken过期或被刷新，使用refreshToken获取新的 accessToken，参考com.netease.qiye.token.RefreshToken
        // 4-5. refreshToken过期，使用authCode，重新获取token（见步骤3）

        Q q = Q.init(null)
                .addParam("domain", "hmail.my163mail.com")
                .addParam("accountName", "jy_imap");
        R r1 = qiyeOpenPlatSDK.commonInvoke(q, "/api/open/account/getAccount");
        System.out.println(r1);

        // 根据code值，判断是否以上情况，详细见文档的异常响应码 或 com.netease.qiye.qiyeopenplatform.common.constant.ResultEnum
        if (ResultEnum.SUCCESS.getCode() == r1.getCode()) {
            System.out.println("调用成功");
        }
    }
}

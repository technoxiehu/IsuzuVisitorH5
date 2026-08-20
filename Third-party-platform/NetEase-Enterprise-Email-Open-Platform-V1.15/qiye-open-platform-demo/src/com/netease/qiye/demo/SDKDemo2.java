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
public class SDKDemo2 {

    // 保密，授权码
    public static final String authCode = "lDkMbiqZ";

    public static final String appId = "qy202112160922309A15";
    public static final String orgOpenId = "cbaf8e98a11d33e8";

    public static final String serverUrl = "https://api.qiye.163.com";

    public static void main(String[] args) {

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
        QiyeOpenPlatSDK instance = new QiyeOpenPlatSDK("dev", qiyeOpenPlatSDKConfig);

        // 查看文档，应用获取token
        R<AppLoginResp> appLoginR = instance.appLogin(authCode);

        // 复制access com.netease.qiye.token 和 refresh com.netease.qiye.token
        System.out.println(appLoginR.getResponseText());

        // 如果内存中持有该实例，则可以设置 com.netease.qiye.token ,重复调用
        AppLoginResp ar = appLoginR.getDataBean(AppLoginResp.class);
        instance.getQiyeOpenPlatSDKConfig().setupToken(ar);

        Q q = Q.init(null).addParam("domain", "hmail.my163mail.com").addParam("accountName", "jy_imap");

        // 调用 20次 ，模拟长时间调用接口
        int i = 20;
        while (i-- > 0) {

            // 模拟：调用的意外因素，比如过期
            accidentCall(instance, i);

            R openApiR = instance.commonInvoke(q, "/api/open/account/getAccount");
            // 打印调用 open-api 的结果
            System.out.println(openApiR);

            // 后面的 else-if 都是处理调用异常、过期
            if (ResultEnum.SUCCESS.getCode() == openApiR.getCode()) {
                System.out.println("调用成功");
            } else if (ResultEnum.TOKEN_ACCESS_EXPIRED.getCode() == openApiR.getCode()
                    || ResultEnum.TOKEN_NOT_AVAILABLE.getCode() == openApiR.getCode()
                    || ResultEnum.AUTH_DATA_NOT_FOUND.getCode() == openApiR.getCode()) {
                //演示用，仅判断accessToken过期的情况
                System.out.println("access token 过期啦，调用refresh");
                R refreshTokenR = instance.refreshToken();
                //重新设置token
                instance.getQiyeOpenPlatSDKConfig().setupToken((AppLoginResp) refreshTokenR.getDataBean(AppLoginResp.class));

            } else if (ResultEnum.TOKEN_REFRESH_EXPIRED.getCode() == openApiR.getCode()) {
                // refresh token 过期了，只能重新使用授权码 获取token
                appLoginR = instance
                        .appLogin(authCode);
                instance.getQiyeOpenPlatSDKConfig().setupToken((AppLoginResp) appLoginR.getDataBean(AppLoginResp.class));
            } else {
                //其他情况，按需处理
            }

            // 打印当前 token
            System.out.println(instance.getQiyeOpenPlatSDKConfig().getAccessToken());
        }

    }

    private static void accidentCall(QiyeOpenPlatSDK instance, int i) {
        System.out.println("第" + i + "号调用");
        try {
            // 模拟：随机延时 0-2
            Thread.sleep((long) (Math.abs(Math.random() * 2_000)));
            // 模拟：token过期（调用一下刷新接口，原token就过期了） !!!!!
            if (i % 7 == 0) {
                System.out.println("token过期。。。。。。。。。。。。。。。。。。。。。");
                instance.refreshToken();
            }
        } catch (Exception e) {

        }
    }
}

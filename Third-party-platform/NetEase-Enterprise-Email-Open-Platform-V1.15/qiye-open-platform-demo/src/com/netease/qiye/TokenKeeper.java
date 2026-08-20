package com.netease.qiye;

import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDK;
import com.netease.qiye.qiyeopenplatform.sdk.QiyeOpenPlatSDKConfig;

/**
 * API测试用
 * @author caixianyong
 * @createtime 2021/11/22
 */
public class TokenKeeper {

    public static final String appId = "qy20220411100420B6A6";
    public static final String domain = "hmail.my163mail.com";
    public static final String orgOpenId = "cbaf8e98a11d33e8";
    //开发内网
    public static final String serverUrl = "https://api.qiye.163.com";
//    public static final String serverUrl = "http://10.200.217.237:8081";


    public static QiyeOpenPlatSDK getInstance() {

        //init
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

        return qiyeOpenPlatSDK;
    }

}

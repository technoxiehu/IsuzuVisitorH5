package com.ruoyi.visitor.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * 访客审批 JWT 工具（与若依登录 token 体系解耦，见 docs/03_接口契约.md §5.1）
 *
 * @author isuzu
 */
@Component
public class ApproveTokenService
{
    private static final Logger log = LoggerFactory.getLogger(ApproveTokenService.class);

    /** claims 中申请单号键名 */
    public static final String CLAIM_APP_ID = "appId";

    /** 审批 token 签名密钥 */
    @Value("${approve.secret}")
    private String secret;

    /** 审批 token 有效期（分钟，默认7天） */
    @Value("${approve.expireTime:10080}")
    private int expireTime;

    /**
     * 生成审批 token（HS512 签名，含申请单号与过期时间）
     *
     * @param applicationId 申请单号
     * @return token 字符串
     */
    public String createToken(String applicationId)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_APP_ID, applicationId);
        Date expireDate = new Date(System.currentTimeMillis() + expireTime * 60 * 1000L);
        return Jwts.builder().setClaims(claims).setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 解析审批 token，返回申请单号
     *
     * @param token token 字符串
     * @return 申请单号；签名错误或已过期返回 null
     */
    public String parseApplicationId(String token)
    {
        try
        {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            Object appId = claims.get(CLAIM_APP_ID);
            return appId != null ? appId.toString() : null;
        }
        catch (Exception e)
        {
            log.warn("审批 token 解析失败: {}", e.getMessage());
            return null;
        }
    }
}

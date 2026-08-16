package com.ruoyi.visitor.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import jakarta.mail.internet.MimeMessage;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorCompanion;

/**
 * 访客审批邮件服务
 *
 * SMTP 未配置时（spring.mail.host 缺失）不注入 JavaMailSender，发送直接跳过并记录日志，
 * 不影响申请单提交（见 docs/03_接口契约.md §5.2）。
 *
 * @author isuzu
 */
@Component
public class VisitorMailService
{
    private static final Logger log = LoggerFactory.getLogger(VisitorMailService.class);

    /** 审批链接中的 H5 域名（开发占位，部署时改 application.yml 的 visitor.h5-domain） */
    @Value("${visitor.h5-domain}")
    private String h5Domain;

    @Autowired(required = false)
    private JavaMailSenderImpl mailSender;

    /**
     * 发送审批邮件至被访人邮箱
     *
     * @param application 申请单
     * @param visitor 访客信息
     * @param hostEmail 被访人邮箱
     * @param token 审批 token
     */
    public void sendApproveMail(VisitorApplication application, Visitor visitor, String hostEmail, String token)
    {
        if (mailSender == null)
        {
            log.warn("SMTP 未配置，跳过审批邮件发送。申请单号: {}", application.getApplicationId());
            return;
        }
        if (StringUtils.isEmpty(hostEmail))
        {
            log.warn("被访人邮箱为空，跳过审批邮件发送。申请单号: {}", application.getApplicationId());
            return;
        }

        String approveUrl = h5Domain + "/approve?token=" + token;
        String visitorName = visitor != null ? visitor.getName() : "";
        String visitorCompany = visitor != null ? visitor.getCompany() : "";

        StringBuilder text = new StringBuilder();
        text.append("您好，").append(application.getHostName()).append("：\n\n");
        text.append("您有一条访客访问申请待审批：\n");
        text.append("申请人：").append(visitorName).append("（").append(visitorCompany).append("）\n");
        text.append("访问时间：").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, application.getStartTime()))
                .append(" 至 ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, application.getEndTime())).append("\n");
        text.append("访问事由：").append(application.getReason()).append("\n");
        // 随行人员仅报人数，不列名单与证件号（docs/03_接口契约.md §5.2，名单在审批页查看）
        List<VisitorCompanion> companions = application.getCompanions();
        if (companions != null && !companions.isEmpty())
        {
            text.append("随行人员：").append(companions.size()).append(" 人\n");
        }
        text.append("\n请点击以下链接进入审批页面进行批准或拒绝：\n");
        text.append(approveUrl).append("\n");

        try
        {
            // 显式 UTF-8：MimeMessageHelper 构造时钉死编码，不依赖 defaultEncoding 注入链兜底；
            // multipart=false 纯文本单件邮件（无附件无 HTML），产出 Content-Type: text/plain; charset=UTF-8
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            // 显式发件人：原 SimpleMailMessage 路径未 setFrom，邮件无 From 头、仅靠 JavaMail 会话属性 mail.from 兜底信封；
            // 取 SMTP 账号补齐 From 头（与信封发件人一致），username 为空时跳过保持原行为
            String from = mailSender.getUsername();
            if (StringUtils.isNotEmpty(from))
            {
                helper.setFrom(from);
            }
            helper.setTo(hostEmail);
            // Subject 自动按 RFC 2047 编码为 =?UTF-8?B?...?=
            helper.setSubject("访客访问申请待审批");
            helper.setText(text.toString());
            mailSender.send(message);
            log.info("审批邮件已发送。申请单号: {}, 收件人: {}", application.getApplicationId(), hostEmail);
        }
        catch (Exception e)
        {
            // 邮件发送失败不影响申请单提交成功
            log.error("审批邮件发送失败。申请单号: {}", application.getApplicationId(), e);
        }
    }
}

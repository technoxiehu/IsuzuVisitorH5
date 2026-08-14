package com.ruoyi.web.controller.visitor;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.service.IVisitorApplicationService;

/**
 * 访客申请单接口（匿名访问，见 docs/03_接口契约.md §3.5~§3.7）
 *
 * @author isuzu
 */
@RestController
@RequestMapping("/visitor")
public class VisitorApplicationController
{
    @Autowired
    private IVisitorApplicationService visitorApplicationService;

    /**
     * 被访人查询（按姓名关键字搜索 sys_user 正常用户）
     */
    @GetMapping("/host/search")
    public AjaxResult searchHost(String keyword)
    {
        if (StringUtils.isEmpty(keyword))
        {
            throw new ServiceException("搜索关键字不能为空", 400);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", visitorApplicationService.selectHostList(keyword));
        return AjaxResult.success(data);
    }

    /**
     * 申请单提交（校验通过后发审批邮件）
     */
    @PostMapping("/application")
    public AjaxResult submit(@RequestBody VisitorApplication application)
    {
        String applicationId = visitorApplicationService.submitApplication(application);
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        return AjaxResult.success("提交成功", data);
    }

    /**
     * 审批记录查询（有效记录 + 当日拒绝数）
     */
    @GetMapping("/application/list")
    public AjaxResult list(String visitorId)
    {
        if (StringUtils.isEmpty(visitorId))
        {
            throw new ServiceException("访客ID不能为空", 400);
        }
        return AjaxResult.success(visitorApplicationService.selectValidList(visitorId));
    }
}

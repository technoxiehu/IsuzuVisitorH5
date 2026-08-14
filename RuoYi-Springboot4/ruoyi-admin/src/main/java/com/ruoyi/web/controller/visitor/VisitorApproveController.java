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
import com.ruoyi.visitor.service.IVisitorApplicationService;

/**
 * 审批接口（匿名访问 + 审批 token 鉴权，见 docs/03_接口契约.md §3.8~§3.9）
 *
 * @author isuzu
 */
@RestController
@RequestMapping("/visitor/approve")
public class VisitorApproveController
{
    @Autowired
    private IVisitorApplicationService visitorApplicationService;

    /**
     * 审批详情（token 鉴权；已审批返回 code=601，token 无效返回 code=401）
     */
    @GetMapping("/detail")
    public AjaxResult detail(String token)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("application", visitorApplicationService.getApproveDetail(token));
        return AjaxResult.success(data);
    }

    /**
     * 审批结果回写（只能审批一次，条件更新兜底）
     */
    @PostMapping
    public AjaxResult approve(@RequestBody Map<String, String> body)
    {
        visitorApplicationService.approve(body.get("token"), body.get("action"));
        return AjaxResult.success("审批完成");
    }
}

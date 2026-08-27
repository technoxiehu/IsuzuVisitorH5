package com.ruoyi.web.controller.visitor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.visitor.domain.GuardCardVo;
import com.ruoyi.visitor.domain.GuardEntryVo;
import com.ruoyi.visitor.service.IVisitorGuardService;

/**
 * 门卫电脑端核验接口（需若依登录态；权限 visitor:guard:list / visitor:guard:entry / visitor:entry:list，
 * 见 docs/03_接口契约.md §3.13~§3.15）
 *
 * @author isuzu
 */
@RestController
@RequestMapping("/visitor/guard")
public class VisitorGuardController extends BaseController
{
    @Autowired
    private IVisitorGuardService visitorGuardService;

    /**
     * 门卫有效单据查询（卡片墙数据源；展示当天有效期内审批通过的申请单，权限 visitor:guard:list）
     */
    @PreAuthorize("@ss.hasPermi('visitor:guard:list')")
    @GetMapping("/list")
    public TableDataInfo list(String keyword, String date)
    {
        startPage();
        List<GuardCardVo> list = visitorGuardService.selectGuardCardList(keyword, date);
        return getDataTable(list);
    }

    /**
     * 门卫放行（新增入场记录；操作人取当前登录用户，权限 visitor:guard:entry）
     */
    @PreAuthorize("@ss.hasPermi('visitor:guard:entry')")
    @PostMapping("/entry")
    public AjaxResult entry(@RequestBody Map<String, String> body)
    {
        String entryId = visitorGuardService.createEntry(body.get("applicationId"), SecurityUtils.getUserId(),
                SecurityUtils.getLoginUser().getUser().getNickName());
        Map<String, Object> data = new HashMap<>();
        data.put("entryId", entryId);
        return AjaxResult.success("放行成功", data);
    }

    /**
     * 入场记录查询（门卫可见全部，按放行时间倒序；权限 visitor:entry:list）
     */
    @PreAuthorize("@ss.hasPermi('visitor:entry:list')")
    @GetMapping("/entry/list")
    public TableDataInfo entryList(String keyword, String beginTime, String endTime)
    {
        startPage();
        Date begin = StringUtils.isNotEmpty(beginTime) ? DateUtils.parseDate(beginTime) : null;
        Date end = StringUtils.isNotEmpty(endTime) ? DateUtils.parseDate(endTime) : null;
        List<GuardEntryVo> list = visitorGuardService.selectEntryList(keyword, begin, end);
        return getDataTable(list);
    }
}

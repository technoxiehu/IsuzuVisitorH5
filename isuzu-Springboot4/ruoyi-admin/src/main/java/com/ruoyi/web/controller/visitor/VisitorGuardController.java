package com.ruoyi.web.controller.visitor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.visitor.domain.GuardCardVo;
import com.ruoyi.visitor.domain.GuardEntryVo;
import com.ruoyi.visitor.service.IVisitorGuardService;

/**
 * 门卫电脑端核验接口（需若依登录态；权限 visitor:guard:list / visitor:guard:entry / visitor:entry:list /
 * visitor:entry:export，见 docs/03_接口契约.md §3.13~§3.16）
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
     * 门卫进出登记（新增进场/离厂事件；操作人取当前登录用户，权限 visitor:guard:entry）
     * body.entryType：'0' 进场（缺省，兼容旧调用）/ '1' 离厂；在厂状态严格交替校验见 service
     */
    @PreAuthorize("@ss.hasPermi('visitor:guard:entry')")
    @PostMapping("/entry")
    public AjaxResult entry(@RequestBody Map<String, String> body)
    {
        String entryType = body.get("entryType");
        String entryId = visitorGuardService.createEntry(body.get("applicationId"), entryType,
                SecurityUtils.getUserId(), SecurityUtils.getLoginUser().getUser().getNickName());
        Map<String, Object> data = new HashMap<>();
        data.put("entryId", entryId);
        return AjaxResult.success("1".equals(entryType) ? "离厂成功" : "进场成功", data);
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

    /**
     * 入场记录导出（与列表查询同条件导出全部匹配行，手机号/身份证全量明文；权限 visitor:entry:export）
     */
    @PreAuthorize("@ss.hasPermi('visitor:entry:export')")
    @Log(title = "入场记录", businessType = BusinessType.EXPORT)
    @PostMapping("/entry/export")
    public void export(HttpServletResponse response, String keyword, String beginTime, String endTime)
    {
        Date begin = StringUtils.isNotEmpty(beginTime) ? DateUtils.parseDate(beginTime) : null;
        Date end = StringUtils.isNotEmpty(endTime) ? DateUtils.parseDate(endTime) : null;
        List<GuardEntryVo> list = visitorGuardService.selectEntryExportList(keyword, begin, end);
        ExcelUtil<GuardEntryVo> util = new ExcelUtil<GuardEntryVo>(GuardEntryVo.class);
        util.exportExcel(response, list, "入场记录数据");
    }
}

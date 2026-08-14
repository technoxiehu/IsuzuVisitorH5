package com.ruoyi.web.controller.visitor;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.service.IVisitorService;

/**
 * 访客信息接口（匿名访问，见 docs/03_接口契约.md §3.1~§3.4）
 *
 * @author isuzu
 */
@RestController
@RequestMapping("/visitor")
public class VisitorController
{
    @Autowired
    private IVisitorService visitorService;

    /**
     * 用户信息查询（老用户判断，查无时 data.user 为 null）
     */
    @GetMapping("/user")
    public AjaxResult getUser(String visitorId)
    {
        if (StringUtils.isEmpty(visitorId))
        {
            throw new ServiceException("访客ID不能为空", 400);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("user", visitorService.selectVisitor(visitorId));
        return AjaxResult.success(data);
    }

    /**
     * 新用户注册
     */
    @PostMapping("/user")
    public AjaxResult register(@RequestBody Visitor visitor)
    {
        visitorService.registerVisitor(visitor);
        return AjaxResult.success("注册成功");
    }

    /**
     * 用户信息更新（我的信息页）
     */
    @PutMapping("/user")
    public AjaxResult update(@RequestBody Visitor visitor)
    {
        visitorService.updateVisitor(visitor);
        return AjaxResult.success("更新成功");
    }

    /**
     * 头像上传（匿名，复用若依 FileUploadUtils）
     */
    @PostMapping("/upload")
    public AjaxResult upload(MultipartFile file) throws Exception
    {
        try
        {
            String filePath = RuoYiConfig.getUploadPath();
            String fileName = FileUploadUtils.upload(filePath, file);
            Map<String, Object> data = new HashMap<>();
            // 返回相对路径（/profile/...），由前端按页面源解析渲染，避免拼接请求 Host 导致的跨环境裂图
            data.put("url", fileName);
            return AjaxResult.success(data);
        }
        catch (Exception e)
        {
            return AjaxResult.error(400, e.getMessage());
        }
    }
}

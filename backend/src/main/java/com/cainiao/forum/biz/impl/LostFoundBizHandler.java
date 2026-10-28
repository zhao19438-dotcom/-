package com.cainiao.forum.biz.impl;

import com.cainiao.forum.biz.BizHandler;
import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.LostFoundMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.LostFound;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * 失物招领业务处理器（参考模块）。
 *
 * 目的：证明“新增业务模块无需改动核心 Service / Controller”。
 * 前端在失物招领版块发帖时，把 lostType / contact / location / itemName 以 JSON 放进 extJson，
 * 这里负责校验并把结构化信息写入 t_lost_found 扩展表。
 */
@Component
public class LostFoundBizHandler implements BizHandler {

    @Resource
    private LostFoundMapper lostFoundMapper;

    @Override
    public String bizType() {
        return "lostfound";
    }

    @Override
    public void validate(Article article, Map<String, Object> ext) {
        // 失物招领必须提供丢失/拾得类型与联系方式，否则拒绝发布。
        Object lostType = ext.get("lostType");
        Object contact = ext.get("contact");
        if (lostType == null || contact == null
                || String.valueOf(lostType).trim().isEmpty()
                || !("lost".equals(String.valueOf(lostType)) || "found".equals(String.valueOf(lostType)))
                || String.valueOf(contact).trim().isEmpty()
                || String.valueOf(contact).length() > 100
                || (ext.get("itemName") != null && String.valueOf(ext.get("itemName")).length() > 100)
                || (ext.get("location") != null && String.valueOf(ext.get("location")).length() > 200)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
    }

    @Override
    public void afterPublish(Article article, Map<String, Object> ext) {
        LostFound lf = new LostFound();
        lf.setArticleId(article.getId());
        lf.setLostType(String.valueOf(ext.getOrDefault("lostType", "lost")));
        lf.setItemName(ext.get("itemName") == null ? null : String.valueOf(ext.get("itemName")));
        lf.setContact(String.valueOf(ext.get("contact")));
        lf.setLocation(ext.get("location") == null ? null : String.valueOf(ext.get("location")));
        lf.setStatus((byte) 0);
        lf.setCreateTime(new Date());
        lostFoundMapper.insert(lf);
    }
}

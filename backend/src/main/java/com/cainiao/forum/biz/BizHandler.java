package com.cainiao.forum.biz;

import com.cainiao.forum.model.Article;

import java.util.Map;

/**
 * 业务处理器扩展点：每种业务类型（forum / lostfound / ...）对应一个实现。
 *
 * 新增业务模块时，只需新增一个 BizHandler 实现类并注册为 Spring Bean，
 * 核心 ArticleService.create 无需改动一行——这就是“模块化添加”的地基。
 */
public interface BizHandler {

    // 业务类型标识，必须与 t_board.biz_type 取值一致。
    String bizType();

    // 发布前校验扩展字段；校验失败抛出 ApplicationException，帖子不会入库。
    default void validate(Article article, Map<String, Object> ext) {
    }

    // 入库前处理：可改写 article 字段或准备关联数据。
    default void beforePublish(Article article, Map<String, Object> ext) {
    }

    // 入库后处理：写入业务扩展表等。此时 article.getId() 已由 MyBatis 回填。
    default void afterPublish(Article article, Map<String, Object> ext) {
    }
}

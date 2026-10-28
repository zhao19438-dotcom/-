package com.cainiao.forum.biz;

import com.cainiao.forum.biz.impl.ForumBizHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务处理器注册表：按 bizType 建立索引。
 *
 * Spring 启动时会把所有 BizHandler 实现（标注 @Component）自动注入到 List，
 * 由此实现“加一个模块只加一个 Bean，其余全自动生效”。
 */
@Component
public class BizHandlerRegistry {

    private final Map<String, BizHandler> registry = new HashMap<>();

    // 默认处理器：普通帖子，空实现。任何未匹配到专门处理器的类型都回退到这里，保证兼容。
    private final BizHandler defaultHandler = new ForumBizHandler();

    public BizHandlerRegistry(List<BizHandler> handlers) {
        for (BizHandler handler : handlers) {
            registry.put(handler.bizType(), handler);
        }
    }

    // 按业务类型取出处理器；未知类型回退到默认 forum 处理器。
    public BizHandler get(String bizType) {
        return registry.getOrDefault(bizType, defaultHandler);
    }
}

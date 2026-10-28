package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.SensitiveWordMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.SensitiveWord;
import com.cainiao.forum.services.ISensitiveWordService;
import com.cainiao.forum.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词服务实现：使用 DFA（Trie 树）做一次扫描完成多词匹配。
 *
 * 设计要点：
 * 1. 词库常驻内存，过滤时不查库，性能与文本长度线性相关。
 * 2. 刷新词库时构建一棵新树再整体替换引用，读写互不阻塞。
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements ISensitiveWordService {

    @Resource
    private SensitiveWordMapper sensitiveWordMapper;

    // DFA 树节点。
    private static class Node {
        Map<Character, Node> children = new HashMap<>();
        boolean end;
    }

    // 当前生效的词库根节点。
    private volatile Node root = new Node();

    // 是否已加载过，避免每次过滤都查库。
    private volatile boolean loaded = false;

    // 首次使用时懒加载词库。
    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (!loaded) {
                refreshInternal();
                loaded = true;
            }
        }
    }

    @Override
    public void refresh() {
        ensureLoaded();
        synchronized (this) {
            refreshInternal();
        }
    }

    // 真正的加载动作：查库 -> 建树 -> 替换根引用。
    private void refreshInternal() {
        List<String> words = sensitiveWordMapper.selectAllWords();
        Node newRoot = new Node();
        int count = 0;
        if (words != null) {
            for (String w : words) {
                if (w == null) {
                    continue;
                }
                String word = w.trim();
                if (word.isEmpty()) {
                    continue;
                }
                Node cur = newRoot;
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    Node next = cur.children.get(c);
                    if (next == null) {
                        next = new Node();
                        cur.children.put(c, next);
                    }
                    cur = next;
                }
                cur.end = true;
                count++;
            }
        }
        root = newRoot;
        log.info("敏感词库已加载, 共 " + count + " 条");
    }

    @Override
    public List<SensitiveWord> listAll() {
        ensureLoaded();
        return sensitiveWordMapper.selectAll();
    }

    @Override
    public void add(String word) {
        String trimmed = StringUtil.trim(word);
        if (trimmed == null || trimmed.isEmpty()) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        if (trimmed.length() > 50) {
            throw new ApplicationException(AppResult.failed("敏感词长度不能超过 50"));
        }
        SensitiveWord row = new SensitiveWord();
        row.setWord(trimmed);
        row.setCreateTime(new Date());
        sensitiveWordMapper.insertSelective(row);
        // 词库变化后立即刷新内存树。
        refresh();
    }

    @Override
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        sensitiveWordMapper.deleteByPrimaryKey(id);
        refresh();
    }

    @Override
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        ensureLoaded();
        Node r = root;
        if (r.children.isEmpty()) {
            return text;
        }
        char[] chars = text.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        int i = 0;
        while (i < chars.length) {
            int len = match(r, chars, i);
            if (len > 0) {
                for (int k = 0; k < len; k++) {
                    sb.append('*');
                }
                i += len;
            } else {
                sb.append(chars[i]);
                i++;
            }
        }
        return sb.toString();
    }

    // 从 begin 开始做最长匹配，返回命中长度；0 表示未命中。
    private int match(Node r, char[] chars, int begin) {
        Node cur = r;
        int last = 0;
        for (int i = begin; i < chars.length; i++) {
            Node next = cur.children.get(chars[i]);
            if (next == null) {
                break;
            }
            cur = next;
            if (cur.end) {
                last = i - begin + 1;
            }
        }
        return last;
    }
}

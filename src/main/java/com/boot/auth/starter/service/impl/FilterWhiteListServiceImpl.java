package com.boot.auth.starter.service.impl;

import com.boot.auth.starter.service.FilterWhiteListService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;

@Scope
@Component
public class FilterWhiteListServiceImpl implements FilterWhiteListService {
    /**
     * 白名单路由规则
     */
    private final static HashSet<String> root = new HashSet<>();
    /**
     * 匹配命中缓存
     */
    private final static HashSet<String> cache = new HashSet<>();


    @PostConstruct
    private void init() {
        addWhiteList("/actuator","/static/**","/public/**", "/webjars/**", "/v3/api-docs/**", "**.css", "**.js", "**.jpg", "**.ico", "**.png");
    }

    @Override
    public boolean isWhiteList(String path) {
        if (cache.contains(path)) return true;
        boolean fag = false;
        int index = path.lastIndexOf(".");
        // 处理文件后缀
        if (index > 0) {
            String suffix = path.substring(index);
            String routeKey = "/**" + suffix;
            fag = root.contains(routeKey);
        } else {
            StringBuilder route = new StringBuilder();
            String[] nodes = path.split("/");
            for (String node : nodes) {
                if (!StringUtils.hasText(node)) continue;
                // 匹配 ** 成功 直接成功
                String routeKey = route + "/" + "**";
                if (root.contains(routeKey)) {
                    cache.add(path);
                    return true;
                }
                // 匹配 * 成功 本轮 node 成功
                routeKey = route + "/" + "*";
                if (root.contains(routeKey)) {
                    fag = true;
                    route.append("/").append("*");
                } else {
                    // 匹配 实际的node地址
                    routeKey = route + "/" + node;
                    fag = root.contains(routeKey);
                    if (!fag) return false;
                    route.append("/").append(node);
                }
            }
        }
        if (fag) cache.add(path);
        return fag;
    }

    @Override
    public void addWhiteList(String... paths) {
        if (paths == null) return;
        Arrays.stream(paths)
                .filter(StringUtils::hasText)
                .forEach(path -> {
                    StringBuffer routeKeyBuffer = new StringBuffer();
                    Arrays.stream(path.split("/")).filter(StringUtils::hasText).forEach(node -> {
                        routeKeyBuffer.append("/").append(node);
                        String routeKey = routeKeyBuffer.toString();
                        root.add(routeKey);
                    });
                });
    }
}


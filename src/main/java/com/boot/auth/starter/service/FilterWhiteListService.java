package com.boot.auth.starter.service;

public interface FilterWhiteListService {

    /**
     * 匹配白名单
     * 规则:
     * 1、* 表示单级路由通配符
     * 匹配成功后会进入下一级路由匹配;
     * 匹配失败后会使用当前真实单级路由进行匹配;
     * 当真实单级路由匹配失败后本方法返回匹配失败,匹配成功则进入前两种场景
     * 2、** 表示多级路由通配符
     * 匹配成功后会直接返回成功,跳过后面的所有匹配逻辑;
     * 没有成功会进行 * 模式匹配
     * 3、开头带 / 或不带 效果相同
     *
     * @param path 要匹配的路由
     * @return true表示在白名单中
     */
    boolean isWhiteList(String path);

    /**
     * 增加白名单路由
     *
     * @param paths 要增加的白名单路由
     */
    void addWhiteList(String... paths);
}


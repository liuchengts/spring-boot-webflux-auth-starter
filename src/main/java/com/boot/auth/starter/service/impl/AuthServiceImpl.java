package com.boot.auth.starter.service.impl;

import com.boot.auth.starter.common.AuthConstant;
import com.boot.auth.starter.common.AuthProperties;
import com.boot.auth.starter.common.RestStatus;
import com.boot.auth.starter.exception.AuthException;
import com.boot.auth.starter.service.AuthService;
import com.boot.auth.starter.service.CacheService;
import com.boot.auth.starter.utils.AESUtil;
import com.boot.auth.starter.utils.CookieUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthServiceImpl implements AuthService {
    private final static Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    final
    CacheService cacheService;
    ObjectMapper objectMapper;
    AuthProperties authProperties;
    final static String TOKEN_NAME = AuthConstant.HEAD_TOKEN_NAME;

    public AuthServiceImpl(CacheService cacheService, ObjectMapper objectMapper, AuthProperties authProperties) {
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
    }

    @Override
    public String auth(String group, String userNo, String roles, Map<String, Object> parameters,
                       ServerHttpResponse response, ServerHttpRequest request) throws Exception {
        try {
            Map<String, String> oldTokenMap = analysisToken(request);
            //删除原有的token
            delToken(oldTokenMap, response, request);
        } catch (Exception e) {
            delToken(response, request);
        }
        String key = String.join(AuthConstant.HEAD_TOKEN_SEPARATOR, userNo, group, System.currentTimeMillis() + "");
        if (authProperties.getEnableExclude()) {
            key = String.join(AuthConstant.HEAD_TOKEN_SEPARATOR, key, "E" + System.currentTimeMillis());
        }
        //生成token
        String token = AESUtil.encrypt(key, authProperties.getDomain());
        if (parameters == null) parameters = new HashMap<>();
        parameters.put(AuthConstant.SESSION_USER_NO, userNo);
        parameters.put(AuthConstant.SESSION_ROLES, roles);
        cacheService.put(authProperties.getTokenPrefix() + key, objectMapper.writeValueAsString(parameters),
                authProperties.getOverdueTime());
        CookieUtils.setCookie(request, response, TOKEN_NAME, token, authProperties.getOverdueTime().intValue());
        response.getHeaders().set(TOKEN_NAME, token);
        return token;
    }

    private void delToken(Map<String, String> oldTokenMap, ServerHttpResponse response, ServerHttpRequest request) {
        if (oldTokenMap.isEmpty() || !oldTokenMap.containsKey(AuthConstant.MAP_KEY_KEY)) return;
        cacheService.remove(authProperties.getTokenPrefix() + oldTokenMap.get(AuthConstant.MAP_KEY_KEY));
        delToken(response, request);
    }

    @Override
    public Map<String, String> analysisToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(TOKEN_NAME);
        if (StringUtils.isEmpty(token)) token = CookieUtils.getCookieValue(request, TOKEN_NAME);
        return analysisToken(token);
    }

    @Override
    public Map<String, String> analysisToken(String token) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isEmpty(token)) return map;
        String decryptToken = AESUtil.decrypt(token, authProperties.getDomain());
        if (!StringUtils.hasText(decryptToken)) {
            throw new AuthException(RestStatus.USER_TOKEN_INVALID);
        }
        String[] keys = decryptToken.split(AuthConstant.HEAD_TOKEN_SEPARATOR);
        map.put(AuthConstant.MAP_KEY_TOKEN, token);
        map.put(AuthConstant.MAP_KEY_USER_NO, keys[0]);
        map.put(AuthConstant.MAP_KEY_GROUP, keys[1]);
        map.put(AuthConstant.MAP_KEY_TIME, keys[2]);
        checkOverdueTime(Long.parseLong(keys[2]));
        String key = "";
        if (authProperties.getEnableExclude() != null && authProperties.getEnableExclude()) {
            if (keys.length != 4) throw new AuthException(RestStatus.SYSTEM_CACHE_KEY_ERROR);
            key = String.join(AuthConstant.HEAD_TOKEN_SEPARATOR, keys[0], keys[1], keys[2], keys[3]);
        } else {
            key = String.join(AuthConstant.HEAD_TOKEN_SEPARATOR, keys[0], keys[1], keys[2]);
        }
        map.put(AuthConstant.MAP_KEY_KEY, key);
        return map;
    }

    /**
     * 校验 token 是否过期
     *
     * @param time token中记录的生成时间
     */
    private void checkOverdueTime(Long time) {
        LocalDateTime tokenTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        LocalDateTime overdueTime = tokenTime.plusSeconds(authProperties.getOverdueTime());
        if (LocalDateTime.now().compareTo(overdueTime) > 0) {
            throw new AuthException(RestStatus.USER_TOKEN_INVALID);
        }
    }

    @Override
    public Boolean deleteAuth(ServerHttpResponse response, ServerHttpRequest request) {
        try {
            delToken(analysisToken(request), response, request);
            return true;
        } catch (Exception e) {
            delToken(response, request);
            return false;
        }
    }

    private void delToken(ServerHttpResponse response, ServerHttpRequest request) {
        response.getHeaders().remove(TOKEN_NAME);
        CookieUtils.deleteCookie(request, response, TOKEN_NAME);
    }

    @Override
    public Boolean checkToken(ServerHttpRequest request) {
        Map<String, String> tokenMap;
        try {
            tokenMap = analysisToken(request);
        } catch (Exception e) {
            return false;
        }
        if (tokenMap.isEmpty() || !tokenMap.containsKey(AuthConstant.MAP_KEY_KEY)) return false;
        try {
            Long expire = cacheService.getExpire(authProperties.getTokenPrefix() +
                    tokenMap.get(AuthConstant.MAP_KEY_KEY));
            if (expire <= 0) return false;
        } catch (Exception e) {
            throw new AuthException(RestStatus.SYSTEM_ERROR);
        }
        return true;
    }
}

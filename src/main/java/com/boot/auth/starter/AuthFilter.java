package com.boot.auth.starter;

import com.boot.auth.starter.annotation.Auth;
import com.boot.auth.starter.annotation.IgnoreLogin;
import com.boot.auth.starter.annotation.NoAuthGetSession;
import com.boot.auth.starter.annotation.OperLog;
import com.boot.auth.starter.common.AuthConstant;
import com.boot.auth.starter.common.LogicSession;
import com.boot.auth.starter.common.Session;
import com.boot.auth.starter.model.OperLogAnnotationEntity;
import com.boot.auth.starter.service.AuthService;
import com.boot.auth.starter.service.LogService;
import com.boot.auth.starter.utils.IPUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
@Order(-1)
@Component
public class AuthFilter implements WebFilter {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private final SessionResolver sessionResolver;
    private final String loginRequired;
    private final String tokenInvalid;
    private final String authNoInvalid;
    private final AuthService authService;
    private final LogService logService;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public AuthFilter(SessionResolver sessionResolver,
                      String loginRequired,
                      String tokenInvalid,
                      String authNoInvalid,
                      AuthService authService,
                      LogService logService,
                      RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.sessionResolver = sessionResolver;
        this.loginRequired = loginRequired;
        this.tokenInvalid = tokenInvalid;
        this.authNoInvalid = authNoInvalid;
        this.authService = authService;
        this.logService = logService;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return requestMappingHandlerMapping.getHandler(exchange).switchIfEmpty(chain.filter(exchange)).flatMap(handler -> {
            HandlerMethod handlerMethod;
            if (!(handler instanceof HandlerMethod)) {
                return chain.filter(exchange);
            } else {
                handlerMethod = (HandlerMethod) handler;
            }
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            LogicSession logicSession = getSession(response, request);
            Auth auth = handlerMethod.getMethod().getDeclaringClass().getAnnotation(Auth.class);
            if (auth == null) auth = handlerMethod.getMethodAnnotation(Auth.class);
            //没有auth直接认证通过
            if (auth == null) {
                requestAttribute(exchange, logicSession);
                return chain.filter(exchange);
            }

            // 不校验登录信息
            IgnoreLogin ignoreToken = handlerMethod.getMethod().getDeclaringClass().getAnnotation(IgnoreLogin.class);
            if (ignoreToken == null) {
                ignoreToken = handlerMethod.getMethodAnnotation(IgnoreLogin.class);
            }
            //不校验登录信息通过
            if (null != ignoreToken && ignoreToken.ignore()) {
                requestAttribute(exchange, logicSession);
                return chain.filter(exchange);
            }
            //不强制校验权限
            NoAuthGetSession noAuthGetSession = handlerMethod.getMethod().getDeclaringClass().getAnnotation(NoAuthGetSession.class);
            if (noAuthGetSession == null) noAuthGetSession = handlerMethod.getMethodAnnotation(NoAuthGetSession.class);
            //不强制校验权限通过
            if (noAuthGetSession != null) {
                if (noAuthGetSession.loginRequired()) {
                    requestAttribute(exchange, logicSession);
                }
                return chain.filter(exchange);
            }
            if (logicSession == null || !logicSession.getValidLogin()) {
                //未登录
                log.warn("用户未登录,拒绝访问[" + request + "]");
                return send(response, loginRequired);
            }
            if (!logicSession.getValidToken()) {
                //token失效
                log.warn("用户token失效,拒绝访问[" + request.getURI() + "]");
                return send(response, tokenInvalid);
            }
            //开始校验权限
            List<String> roles = logicSession.getSessionOptional()
                    .map(session -> Arrays.asList(session.getRoles().split(",")))
                    .orElse(new ArrayList<>());
            Optional<String> optionalRole = Arrays.stream(auth.roles()).filter(roles::contains).findFirst();
            if (optionalRole.isEmpty()) {
                log.warn("用户不具备访问权限,拒绝访问[" + request.getURI() + "]");
                return send(response, authNoInvalid);
            }
            requestAttribute(exchange, logicSession);
            return chain.filter(exchange);
        });


    }

    /**
     * 向当前会话中写入内容
     *
     * @param exchange     会话
     * @param logicSession 内容
     */
    private void requestAttribute(ServerWebExchange exchange, LogicSession logicSession) {
        if (logicSession == null) return;
        Optional<Session> sessionOptional = logicSession.getSessionOptional();
        sessionOptional.ifPresent(s -> exchange.getAttributes().put(AuthConstant.ATTR_SESSION, s));
    }

    /**
     * 往客户端回写消息
     *
     * @param response 返回
     * @param json     输出的json信息
     * @throws Exception
     */
    private Mono<Void> send(ServerHttpResponse response, String json) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * 获得session
     *
     * @param request ServerHttpRequest
     * @return 返回逻辑session对象
     */
    private LogicSession getSession(ServerHttpResponse response, ServerHttpRequest request) {
        try {
            return sessionResolver.resolve(authService.analysisToken(request),
                    getHeaderValue(request, AuthConstant.HEADER_KEY_PLATFORM),
                    getHeaderValue(request, AuthConstant.HEADER_KEY_VERSION),
                    IPUtils.getClientIP(request));
        } catch (Exception e) {
            authService.deleteAuth(response, request);
            return null;
        }
    }

    /**
     * 记录用户操作日志
     */
    private void saveOperLog(ServerHttpRequest request, ServerHttpResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            OperLog operLog = handlerMethod.getMethod().getDeclaringClass().getAnnotation(OperLog.class);
            if (operLog == null) operLog = handlerMethod.getMethodAnnotation(OperLog.class);
            if (operLog != null && operLog.flag()) {
                OperLogAnnotationEntity logEntity = new OperLogAnnotationEntity();
                logEntity.setOperType(operLog.operType());
                logEntity.setChannel(getHeaderValue(request, AuthConstant.HEADER_KEY_CHANNEL));
                logEntity.setDeviceId(getHeaderValue(request, AuthConstant.HEADER_KEY_DEVICEID));
                LogicSession logicSession = getSession(response, request);
                Optional<Session> sessionOptional = null;
                if (logicSession != null) {
                    sessionOptional = logicSession.getSessionOptional();
                }
                if (sessionOptional != null && sessionOptional.isPresent()) {//当前访问者信息
                    Session session = sessionOptional.get();
                    logEntity.setUserNo(session.getUserNo());
                    logEntity.setUsername(session.getUsername());
                    logEntity.setRoles(session.getRoles());
                    logEntity.setObj(session.getObj());
                    logEntity.setVersion(session.getVersion());
                    logEntity.setPlatform(session.getPlatform());
                }
                logService.addLog(logEntity);
            }
        }
    }

    /**
     * 获取 header的内容
     *
     * @param request ServerHttpRequest
     * @param key     key
     * @return 返回对应的值
     */
    private String getHeaderValue(ServerHttpRequest request, String key) {
        String value = "";
        try {
            value = request.getHeaders().getFirst(key);
        } catch (Exception e) {
            log.warn("header key is null", e);
        }
        return value;
    }
}
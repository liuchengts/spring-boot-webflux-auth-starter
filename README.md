# spring-boot-webflux-auth-starter

基于`spring boot webflux`的权限拦截验证

## 重要声明

* 本分支由`1.0.0.0`为第一个版本，此版本默认提供`guava`存储令牌的功能
* 根据`guava`的缓存特性，提供了`2`种缓存方案(`LoadingCache、Cache`)
* 其他方式的缓存方案请参考   [spring-boot-auth-starter README.md](https://github.com/liuchengts/spring-boot-auth-starter/blob/master/README.md)
## 一、使用前需要的准备

* maven中央仓库地址 [其他方式集成](https://search.maven.org/artifact/io.github.liuchengts/spring-boot-webflux-auth-starter)

```
<dependency>
    <groupId>io.github.liuchengts</groupId>
    <artifactId>spring-boot-webflux-auth-starter</artifactId>
    <version>1.0.0.4</version>
</dependency>
```
## 二、使用
请参考 [spring-boot-auth-starter README.md](https://github.com/liuchengts/spring-boot-auth-starter/blob/master/README.md)

## 三、白名单功能
由于`webflux`使用的是`WebFilter`来实现权限,所以增加白名单功能,用于根据访问路由判定是否需要进入权限验证业务中
- 实现:
  - 默认的实现在 `com.boot.auth.starter.service.impl.FilterWhiteListServiceImpl`
  - 已包括的白名单列表如下:`"/actuator","/static/**","/public/**", "/webjars/**", "/v3/api-docs/**", "**.css", "**.js", "**.jpg", "**.ico", "**.png"`
- 自定义白名单列表:
  - 正常情况只需要将`FilterWhiteListService` 通过`spring ioc`注入对应的自定义类中,调用其`addWhiteList`方法来扩展白名单列表
  - 下面是一个配置白名单的例子(只需要调用`addWhiteList`方法即可,调用方式随意):
    ```
    
    import com.boot.auth.starter.service.FilterWhiteListService
    import jakarta.annotation.PostConstruct
    import org.springframework.beans.factory.annotation.Autowired
    import org.springframework.context.annotation.Configuration
    
    
    @Configuration
    class AuthConfiguration @Autowired constructor(
        private val filterWhiteListService: FilterWhiteListService
    )  {
    
        @PostConstruct
        fun init() {
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
             * 注意:请忽略示例中的星号前后空格(与注释冲突,必须要空格)
             * 例1: /a/b/c     等效 a/b/c
             * 例2: /**/b/c    效果 ** 开头的全部放行,不会再验证 b/c 任意级路由任意字符
             * 例3: /a/ * /c   效果  中间a和b中间的路由可以是任意字符
             * 例4: /a/b/ *    效果  验证完/a/b 之后 后面的最后一级可以是任意字符
             * 例5: /a/b/ **   效果  验证完/a/b 之后 后面的可以是任意级路由任意字符
             *
             */
            filterWhiteListService.addWhiteList("白名单1","白名单2")
        }
    }
    
    ```
- 自定义白名单功能:
  - 依旧是老规矩,继承其`com.boot.auth.starter.service.impl.FilterWhiteListServiceImpl`类,可以根据需要自己覆盖里面的方法即可.
  - 可以参考[spring-boot-auth-starter README.md](https://github.com/liuchengts/spring-boot-auth-starter/blob/master/README.md)中`自定义输出、自定义缓存方案`的模式
## 四、版本发布说明

* 1.0.0.0 根据 [spring-boot-auth-starter-1.0.7.4](https://github.com/liuchengts/spring-boot-auth-starter) 更改 `servlet` 为`webflux`
* 1.0.0.1 增加白名单功能、暂不实现日志记录功能
* 1.0.0.2 修复cookie工具bug
* 1.0.0.3 修复若干bug
* 1.0.0.4 修复HEAD_TOKEN_NAME 识别问题
* 1.0.0.5 修复无效 token 缓存问题

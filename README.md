# spring-boot-webflux-auth-starter

基于`spring boot webflux`的权限拦截验证

## 重要声明

* 本分支由`1.0.0.0`为第一个版本，此版本默认提供`guava`存储令牌的功能
* 根据`guava`的缓存特性，提供了`2`种缓存方案(`LoadingCache、Cache`)
* 其他方式的缓存方案请参考 [spring-boot-auth-starter README.md](https://github.com/liuchengts/spring-boot-auth-starter/README.md)
## 一、使用前需要的准备

* maven中央仓库地址 [其他方式集成](https://search.maven.org/artifact/com.github.liuchengts/spring-boot-webflux-auth-starter)

```
<dependency>
    <groupId>io.github.liuchengts</groupId>
    <artifactId>spring-boot-webflux-auth-starter</artifactId>
    <version>1.0.0.0</version>
</dependency>
```
## 二、使用
请参考 [spring-boot-auth-starter README.md](https://github.com/liuchengts/spring-boot-auth-starter/README.md)

## 三、版本发布说明

* 1.0.0.0 根据 [spring-boot-auth-starter-1.0.7.4](https://github.com/liuchengts/spring-boot-auth-starter) 更改 `servlet` 为`webflux`

package com.cainiao.forum.controller;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.User;
import io.swagger.annotations.*;
import lombok.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;


@Api(tags = "测试类的相关接口")

@RestController

@Profile("dev")
// 测试控制器只在 dev 环境启用，生产打包后这些调试接口不会注册。

@RequestMapping("/test")
// 测试控制器，用于验证接口返回、异常处理和 Swagger 参数展示。
public class TestController {

    @ApiOperation("测试接口1，显示你好Spring boot")
    @GetMapping("/hello")
    // 最简单的字符串返回示例。
    public String hello () {
        return "hello, Spring boot...";
    }

    @ApiOperation("测试接口4，按传入的姓名显示你好信息")
    @PostMapping("/helloByName")
    // 演示如何接收表单参数并返回统一 AppResult。
    public AppResult helloByName (@ApiParam(value = "姓名") @RequestParam("name") @NonNull String name) {
        return AppResult.success("hello : " + name);
    }


    @ApiOperation("测试接口2，显示抛出的异常信息")
    @GetMapping("/exception")
    // 演示普通 Exception 会被全局异常处理器捕获。
    public AppResult testException () throws Exception {
        throw new Exception("这是一个Exception...");
    }

    @ApiOperation("测试接口2，显示抛出的自定义的异常信息")
    @GetMapping("/appException")
    // 演示自定义业务异常 ApplicationException 的返回效果。
    public AppResult testApplicationException () {
        throw new ApplicationException("这是一个ApplicationException...");
    }

    @ApiImplicitParams({
            @ApiImplicitParam(value = "第一个参数", name = "first"),
            @ApiImplicitParam(value = "用户信息", name = "user")
    })
    @ApiOperation("API参数测试")
    @GetMapping("/testApi")
    // 演示 Swagger 对普通参数和 JSON 请求体的展示方式。
    public AppResult testApiParams (String first, @RequestBody User user) {
        return AppResult.success(first);
    }
}


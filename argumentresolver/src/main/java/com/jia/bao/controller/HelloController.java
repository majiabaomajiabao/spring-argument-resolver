package com.jia.bao.controller;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.jia.bao.config.RequestParamNotEmpty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author majiabao on 2017/12/9.
 */
@Controller
@RequestMapping("/hello")
public class HelloController {
    private final String GREETING = "hello,";

    @RequestMapping("/sayHiv1")
    @ResponseBody
    public String sayHiv1(@RequestParam(value = "name", required = false) String name) {

        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name不能为空");
        return GREETING + name;
    }

    @RequestMapping("/sayHiv2")
    @ResponseBody
    public String sayHiv2(@RequestParamNotEmpty(value = "name") String name) {

        return GREETING + name;
    }
}

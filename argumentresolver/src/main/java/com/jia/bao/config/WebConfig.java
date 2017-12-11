package com.jia.bao.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author majiabao on 2017/12/9.
 */
@Configuration
public class WebConfig extends DelegatingWebMvcConfiguration {
    @Resource
    private ConfigurableBeanFactory configurableBeanFactory;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(requestParamNotEmptyResolver());
    }

    @Bean
    public RequestParamNotEmptyResolver requestParamNotEmptyResolver() {
        return new RequestParamNotEmptyResolver(configurableBeanFactory, false);
    }

}

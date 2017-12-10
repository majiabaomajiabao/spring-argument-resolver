package com.jia.bao.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author majiabao on 2017/12/9.
 */
@Configuration
public class WebConfig extends DelegatingWebMvcConfiguration {
    @Resource
    private ConfigurableBeanFactory configurableBeanFactory;

    @Bean
    @Override
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        RequestMappingHandlerAdapter adapter = super.requestMappingHandlerAdapter();
        List<HandlerMethodArgumentResolver> argumentResolverList;
        try {
            Method method = RequestMappingHandlerAdapter.class.getDeclaredMethod("getDefaultArgumentResolvers");
            method.setAccessible(true);
            argumentResolverList = (List<HandlerMethodArgumentResolver>) method.invoke(adapter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        argumentResolverList.add(0, argumentResolver());
        adapter.setArgumentResolvers(argumentResolverList);
        return adapter;
    }

    @Bean
    public ArgumentResolver argumentResolver() {
        return new ArgumentResolver(configurableBeanFactory);
    }

}

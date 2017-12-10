package com.jia.bao.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;

import java.util.Objects;

/**
 * @author majiabao on 2017/12/9.
 */
public class ArgumentResolver extends RequestParamMethodArgumentResolver {

    public ArgumentResolver(ConfigurableBeanFactory beanFactory) {
        super(beanFactory, false);
    }

    @Override
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        ArgumentNotEmpty argumentNotEmptyAn = parameter.getParameterAnnotation(ArgumentNotEmpty.class);
        Object result = super.resolveName(name, parameter, webRequest);
        if (Objects.nonNull(argumentNotEmptyAn)) {
            if (result instanceof String) {
                Preconditions.checkArgument(!Strings.isNullOrEmpty((String) result), name + argumentNotEmptyAn.tip());
            } else {
                Preconditions.checkArgument(Objects.nonNull(result), name + argumentNotEmptyAn.tip());
            }
            return result;
        }
        return result;
    }
}


package com.jia.bao.config;

import java.lang.annotation.*;

/**
 * @author majiabao on 2017/12/9.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**import!!!!!  must   add    @Target    */
public @interface RequestParamNotEmpty {

    String tip() default "不能为空";

    /**
     * The name of the request parameter to bind to.
     */
    String value() default "";

}

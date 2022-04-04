package com.apt.app.aspect_j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author wonderful
 * @Date 2020-6-8
 * @Version 1.0
 * @Description Http错误响应统计注解
 */
@BaseAnnotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpErrorStatistical {
    //切入点名称
    String pointcutName() default "";
    //切入点代码
    int code() default -1;
    //切入处理模式
    //0: 在方法执行后处理 1: 在方法执行前处理 2： 在方法执行前后都处理,默认为0
    int dealMode() default 0;
}

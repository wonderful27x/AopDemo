package com.apt.app.aspect_j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author wonderful
 * @Date 2020-6-8
 * @Version 1.0
 * @Description AOP基础注解-元注解，不过注解好像不能像类一样被继承，
 * 但是我们仍然希望统一标准，因此给出一个模板：
 *
 * @Target(ElementType.METHOD)        //注解范围：方法
 * @Retention(RetentionPolicy.RUNTIME)//作用范围：运行时
 * public @interface YourAnnotation {
 *
 *     //公共属性
 *     //切入点名称
 *     String pointcutName() default "";
 *     //切入点代码，默认为-1
 *     int code() default -1;
 *     //切入处理模式
 *     //0: 在方法执行后处理 1: 在方法执行前处理 2： 在方法执行前后都处理,默认为0
 *     int dealMode() default 0;
 *
 *     //特有属性
 *     ...
 * }
 *
 */
@Target(ElementType.TYPE)          //注解范围：注解
@Retention(RetentionPolicy.RUNTIME)//作用范围：运行时
public @interface BaseAnnotation {

}

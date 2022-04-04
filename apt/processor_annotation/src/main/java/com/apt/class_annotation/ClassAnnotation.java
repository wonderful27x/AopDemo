package com.apt.class_annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author wonderful
 * @Date 2020-7-3
 * @Version 1.0
 * @Description 自定义class注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ClassAnnotation {
    //类名
    String className();
    //字段名集合
    String[] fields();
    //字段对应的类型,默认全为String
    Class<?>[] fieldsType() default {};
    //TODO 待开发
    //字段对应的参数化类型,格式 key-value，如有一个Map<Integer,String> map 的字段
    //parameterizedType = {"map-java.lang.Integer","map-java.lang.String"}
    String[] parameterizedType() default {};
}

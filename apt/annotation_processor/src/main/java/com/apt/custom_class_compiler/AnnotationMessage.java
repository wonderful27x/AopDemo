package com.apt.custom_class_compiler;

import com.apt.class_annotation.ClassAnnotation;
import com.squareup.javapoet.ClassName;
import java.util.IdentityHashMap;
import javax.lang.model.element.TypeElement;

/**
 * @Author wonderful
 * @Date 2020-7-3
 * @Version 1.0
 * @Description 注解信息
 */
public class AnnotationMessage {
    //注解
    ClassAnnotation annotation;
    //注解的类节点
    TypeElement typeElement;
    //需要生成的类名
    String className;
    //字段名集合
    String[] fields;
    //对应的字段类型
    ClassName[] fieldsType;
    //对于的字段参数化类型
    //IdentityHashMap 一个key，多个value,但是key必须是不同的对象
    IdentityHashMap<String,Class<?>> parameterizedTypes;
}

package com.apt.app.aspect_j.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import java.lang.annotation.Annotation;

/**
 * @Author wonderful
 * @Date 2020-6-8
 * @Version 1.0
 * @Description Http错误响应统计
 */
@Aspect
public final class HttpErrorStatisticalAspect extends BaseAspect {

    @Pointcut("execution(@com.apt.app.aspect_j.annotation.HttpErrorStatistical * *(..))")
    @Override
    protected void addYourAnnotationForPointCut() {

    }

    @Override
    protected void dealBeforeTarget(Annotation annotation, Object[] args) {
        logD("切面处理: " + pointcutName);
        logD(pointcutName + " >>> " + annotation.annotationType().getName() + ": " + args[0]);
    }

    @Override
    protected Object dealAfterTarget(Annotation annotation, Object[] args, Object targetReturn) {
        logD("切面处理: " + pointcutName);
        logD(pointcutName + " >>> " + annotation.annotationType().getName() + ": " + args[1]);
        return targetReturn;
    }
}


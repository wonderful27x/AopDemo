package com.apt.app.aspect_j.aspect;

import android.util.Log;
import com.apt.app.aspect_j.annotation.BaseAnnotation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Author wonderful
 * @Date 2020-6-8
 * update to 1.1 2020-9-27
 * @Version 1.1
 * @Description AOP基础切面
 *
 * https://www.jianshu.com/p/f9b02def3a9d
 */
//测试发现基类必须加上这个注解
@Aspect
public abstract class BaseAspect {

    private static final String TAG = "AspectJ";
    private static boolean LOG_PRINT = true;

    //切入点名称
    protected String pointcutName;
    //切入点代码，默认为-1
    protected int code;
    //切入处理模式
    //0: 在方法执行后处理 1: 在方法执行前处理 3： 在方法执行前后都处理,默认为0
    protected int dealMode;

    protected ProceedingJoinPoint proceedingJoinPoint;

    //1.指定切入点Pointcut：找到需要处理的切入点，这个方法名可以取任意值
    //"execution(@com.apt.app.aspect_j.annotation.CaseNumberRefresh * *(..))"
    //execution: 以方法执行时作为切入点触发AspectJ
    //* *(..) ：可以处理被BaseAnnotation注解的所有方法
    //TODO 超级大坑，只有两个点（..） 不是（...）!!!由子类来指定具体的切入点注解:
    @Pointcut()
    protected abstract void addYourAnnotationForPointCut();

    //2.定义消息Advice：如何处理切入点，方法名可取任意值
    // 没有throw情况下：Before 和 Around 谁定义在前面谁先执行，AfterReturning 和 After 谁定义在前面谁先执行
    // 有throw情况下：Before 和 Around 谁定义在前面谁先执行，AfterThrowing 和 After 谁定义在前面谁先执行，AfterReturning不被执行
    // 无论哪种情况下Before 和 Around中间不能定义任何其他Advice,否则什么也不执行

    //方法执行前的处理,必须是public
    @Before("addYourAnnotationForPointCut()")
    public void beforeTarget(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        logD(TAG + " @Before: " + pointcutName);
    }

    //执行目标方法,必须是public
    //TODO 经测试，只有在@Around方法中才能获取到ProceedingJoinPoint，其他方法中均为null
    @Around("addYourAnnotationForPointCut()")
    public Object invokeTarget(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        logD(TAG + " @Around: " + pointcutName);
        this.proceedingJoinPoint = proceedingJoinPoint;
        //方法执行前的处理，之所以要放在这里是因为需要得到注解信息
        //而只有在@Around方法中才能获取到proceedingJoinPoint

        //解析并返回注解
        Annotation annotation = getAnnotation(proceedingJoinPoint);
        //获取实参列表
        Object[] args = getArguments(proceedingJoinPoint);

        //执行目标方法之前的处理
        if (dealMode == 1 || dealMode ==2){
            dealBeforeTarget(annotation,args);
        }

        //执行目标方法
        Object object = proceedingJoinPoint.proceed();
        logD("方法返回值： " + object);

        //执行目标方法之后的处理
        //TODO 这里我们甚至能修改原方法的返回值
        if (dealMode == 0 || dealMode == 2){
            object = dealAfterTarget(annotation,args,object);
        }

        return object;
    }

    //方法执行后的处理,必须是public
    @AfterReturning("addYourAnnotationForPointCut()")
    public void afterTarget(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        logD(TAG + " @AfterReturning: " + pointcutName);
    }

    //异常处理,必须是public
    @AfterThrowing("addYourAnnotationForPointCut()")
    public void jointAfterThrowing(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        logD(TAG + " @AfterThrowing: " + pointcutName);
    }

    //最后的处理,必须是public
    @After("addYourAnnotationForPointCut()")
    public void jointAfter(ProceedingJoinPoint proceedingJoinPoint) throws Throwable{
        //用完解除引用
        this.proceedingJoinPoint = null;
        logD(TAG + " @After: " + pointcutName);
    }

    //获取注解,并解析注解关键数据
    private Annotation getAnnotation(ProceedingJoinPoint proceedingJoinPoint){
        if (proceedingJoinPoint == null)return null;
        //获取方法签名
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (methodSignature == null)return null;
        //获取方法上的所有注解
        Annotation[] annotations = methodSignature.getMethod().getAnnotations();
        Annotation targetAnnotation = null;
        for (Annotation annotation:annotations){
            //获取注解对象的Class
            Class<? extends Annotation> annotationClass = annotation.annotationType();
            //获取元注解
            BaseAnnotation baseAnnotation = annotationClass.getAnnotation(BaseAnnotation.class);
            if (baseAnnotation != null){
                targetAnnotation = annotation;
                break;
            }
        }

        //没有获取到被元注解BaseAnnotation注解的目标注解，说明没有按照规范，则抛出异常
        String methodName = proceedingJoinPoint.getThis().getClass().getName() + "$$" + methodSignature.getMethod().getName();
        if (targetAnnotation == null){
            throw new IllegalArgumentException("\nerror method: " + methodName + ",在方法上使用注解做Aspect切面处理时你的注解必须被元注解@BaseAnnotation所注解!");
        }

        //获取到了被元注解BaseAnnotation注解的目标注解
        try {
            //通过反射拿到注解里属性pointcutName
            Method annotationMethod = targetAnnotation.annotationType().getDeclaredMethod("pointcutName");
            annotationMethod.setAccessible(true);
            this.pointcutName = (String) annotationMethod.invoke(targetAnnotation);
            //如果没有指定切入点名称则默认为全方法名
            if (this.pointcutName == null || pointcutName.isEmpty()){
                this.pointcutName = methodName;
            }

            //通过反射拿到注解里属性code
            annotationMethod = targetAnnotation.annotationType().getDeclaredMethod("code");
            annotationMethod.setAccessible(true);
            this.code = (int) annotationMethod.invoke(targetAnnotation);

            //通过反射拿到注解里属性dealMode
            annotationMethod = targetAnnotation.annotationType().getDeclaredMethod("dealMode");
            annotationMethod.setAccessible(true);
            this.dealMode = (int) annotationMethod.invoke(targetAnnotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            annotationCheck(e,targetAnnotation);
        }

        return targetAnnotation;
    }

    //校验注解,规范写法
    private void annotationCheck(Exception exception,Annotation annotation){
        if (exception instanceof NoSuchMethodException){
            throw new IllegalArgumentException(
                    "\nerror annotation： " + annotation.annotationType().getName() + "\n" +
                    "你的Aspect注解没有按照规范写，其中value、code、dealMode是必须的，下面是一个参考模板： \n" +
                            "    @Target(ElementType.METHOD)        //注解范围：方法\n" +
                            "    @Retention(RetentionPolicy.RUNTIME)//作用范围：运行时\n" +
                            "    public @interface YourAnnotation {\n" +
                            "        //公共属性\n" +
                            "        String pointcutName() default \"\";  //切入点名称\n" +
                            "        int code() default -1;      //切入点代码，默认为-1\n" +
                            "        int dealMode() default 0;   //切入处理模式,0: 在方法执行后处理 1: 在方法执行前处理 2： 在方法执行前后都处理,默认为0\n" +
                            "        \n" +
                            "        //特有属性\n" +
                            "        //...\n" +
                            "    }"
            );
        }
    }

    /**
     * 获取方法的实参列表
     * * AspectJ使用org.aspectj.lang.JoinPoint接口表示目标类连接点对象，如果是环绕增强时，使用org.aspectj.lang.ProceedingJoinPoint表示连接点对象，该类是JoinPoint的子接口。任何一个增强方法都可以通过将第一个入参声明为JoinPoint访问到连接点上下文的信息。我们先来了解一下这两个接口的主要方法：
     * * 1)JoinPoint
     * *    java.lang.Object[] getArgs()：获取连接点方法运行时的入参列表；
     * *    Signature getSignature() ：获取连接点的方法签名对象；
     * *    java.lang.Object getTarget() ：获取连接点所在的目标对象；
     * *    java.lang.Object getThis() ：获取代理对象本身；
     * * 2)ProceedingJoinPoint
     * * ProceedingJoinPoint继承JoinPoint子接口，它新增了两个用于执行连接点方法的方法：
     * *    java.lang.Object proceed() throws java.lang.Throwable：通过反射执行目标对象的连接点处的方法；
     * *    java.lang.Object proceed(java.lang.Object[] args) throws java.lang.Throwable：通过反射执行目标对象连接点处的方法，不过使用新的入参替换原来的入参。
     *
     * @param proceedingJoinPoint
     * @return
     */
    private Object[] getArguments(ProceedingJoinPoint proceedingJoinPoint){
        if (proceedingJoinPoint == null)return null;

        logD("代理对象: " + proceedingJoinPoint.getThis().getClass().getName());
        logD("连接点所在的目标对象: " + proceedingJoinPoint.getTarget().getClass().getName());
        logD("连接点的方法名: " + proceedingJoinPoint.getSignature().getName());

        Object[] objectArray = proceedingJoinPoint.getArgs();
        for (Object object:objectArray){
            logD("实参列表: " + object);
        }

        return objectArray;
    }

    //日志打印方法
    protected void logD(String message){
        if (LOG_PRINT){
            Log.d(TAG,message);
        }
    }

    //日志打印方法
    //TODO that the fuck is this ,it calls exception when invoke this method !!!
    //TODO I really do not know what goes wrong
    protected void logD(String tag,String message){
        if (LOG_PRINT){
            Log.d(tag,message);
        }
    }

    /**
     * 目标方法执行前的处理
     * @param annotation 目标方法的注解
     * @param args 目标方法的实参列表
     */
    protected abstract void dealBeforeTarget(Annotation annotation,Object[] args);

    /**
     * 目标方法执行后的处理
     * @param annotation 目标方法的注解
     * @param args 目标方法的实参列表
     * @param targetReturn 目标方法执行后的返回值
     * @return 当前方法的返回值，这个返回值会替换目标方法的返回值，如果希望保持目标方法返回值一致，则返回targetReturn
     */
    protected abstract Object dealAfterTarget(Annotation annotation,Object[] args,Object targetReturn);
}

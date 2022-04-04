package com.apt.custom_class_compiler;

import com.apt.class_annotation.ClassAnnotation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @Author wonderful
 * @Date 2020-7-3
 * @Version 1.0
 * @Description 自定义class注解处理器
 */

// AutoService则是固定的写法，加个注解即可
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器，用来注册
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理（新增annotation module）
@SupportedAnnotationTypes(Constants.ANNOTATION)
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CustomClassCompiler extends AbstractProcessor {

    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementsUtil;
    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;
    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;
    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;
    //注解信息临时存放容器
    private List<AnnotationMessage> annotationMessages = new ArrayList<>();
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 父类受保护属性，可以直接拿来使用。
        // 其实就是init方法的参数ProcessingEnvironment
        elementsUtil = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        messager.printMessage(Diagnostic.Kind.NOTE,"CustomClassCompiler init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty())return false;

//        //TODO test
//        //ClassAnnotation本身，猜测是支持的注解
//        for (TypeElement typeElement:annotations){
//            messager.printMessage(Diagnostic.Kind.NOTE,"annotations: " + typeElement.getSimpleName().toString());
//            //打印全类名
//            messager.printMessage(Diagnostic.Kind.NOTE,"annotations: " + typeElement.getQualifiedName().toString());
//        }
//
//        //TODO test
//        //注解的类节点和BuildConfig
//        Set<? extends Element> rootElements = roundEnv.getRootElements();
//        for (Element element:rootElements){
//            messager.printMessage(Diagnostic.Kind.NOTE,"rootElements: " + element.getSimpleName().toString());
//            //打印全类名
//            if (element.getKind() == ElementKind.CLASS){
//                messager.printMessage(Diagnostic.Kind.NOTE,"rootElements: " + ((TypeElement)element).getQualifiedName().toString());
//            }
//        }
//
//        //TODO test
//        //获取所有被@ClassAnnotation注解的元素集合
//        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ClassAnnotation.class);
//        for (Element element:elements){
//            messager.printMessage(Diagnostic.Kind.NOTE,"elements: " + element.getSimpleName().toString());
//            //打印全类名
//            if (element.getKind() == ElementKind.CLASS){
//                messager.printMessage(Diagnostic.Kind.NOTE,"elements: " + ((TypeElement)element).getQualifiedName().toString());
//            }
//            //获取包节点
//            getPackageElement(element);
//        }

        //获取所有被@ClassAnnotation注解的元素集合
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ClassAnnotation.class);
        //解析注解的元素并保存
        try {
            if (!parseElement(elements))return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //校验
        annotationCheck();

        //生存java文件
        try {
            javaFileCreate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    //获取包节点,javaPoet源码
    private PackageElement getPackageElement(Element element){
        while (element.getKind() != ElementKind.PACKAGE){
            element = element.getEnclosingElement();
        }
        PackageElement packageElement = (PackageElement) element;
        messager.printMessage(Diagnostic.Kind.NOTE,"packageElement: " + packageElement.getQualifiedName().toString());
        return packageElement;
    }

    //获取包名
    private String getPackageName(Element element){
        return getPackageElement(element).getQualifiedName().toString();
    }

    //解析注解的元素并保存
    //TODO（超级大坑）注解处理器在处理Class<?>[] value(); 这样的注解时会发生异常：
    // Attempt to access Class objects for TypeMirrors...
    // 我在stackoverflow中找到了答案
    // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
    // 项目中只使用了最简单的方法，链接中还有更正确的写法
    private boolean parseElement(Set<? extends Element> elements) throws ClassNotFoundException {
        if (elements == null || elements.isEmpty())return false;
        //理论上来说注解的element都是类节点，因为注解作用在类之上
        for (Element element:elements){
            //通过注解获取关键信息
            ClassAnnotation annotation = element.getAnnotation(ClassAnnotation.class);
            if (annotation == null)continue;
            AnnotationMessage annotationMessage = new AnnotationMessage();
            annotationMessage.annotation = annotation;
            annotationMessage.typeElement = (TypeElement) element;
            annotationMessage.className = annotation.className();
            annotationMessage.fields = annotation.fields();
            annotationMessage.fieldsType = getFieldType(getFieldClassTypeMirrorFromAnnotation(annotation));
            annotationMessage.parameterizedTypes = getFieldParameterizedTypes(annotationMessage);
            annotationMessages.add(annotationMessage);
        }
        return true;
    }

    //从注解中获取字段类型的TypeMirror，我也不知道这是个啥
    private List<? extends TypeMirror> getFieldClassTypeMirrorFromAnnotation(ClassAnnotation annotation){
        try{
            annotation.fieldsType();
        }catch (MirroredTypesException e){
            messager.printMessage(Diagnostic.Kind.NOTE,"error: " + e.getClass().getName());
            messager.printMessage(Diagnostic.Kind.NOTE,"error: " + e.getMessage());
            return e.getTypeMirrors();
        }
        return null;
    }

    //获取字段类型
    private ClassName[] getFieldType(List<? extends TypeMirror> typeMirrors) throws ClassNotFoundException {
        if (typeMirrors == null)return null;
        ClassName[] fieldClass = new ClassName[typeMirrors.size()];
        for (int i=0; i<typeMirrors.size(); i++){
            TypeMirror typeMirror = typeMirrors.get(i);
            TypeElement typeElement = asTypeElement(typeMirror);
            messager.printMessage(Diagnostic.Kind.NOTE,"typeMirror: " + typeElement.getQualifiedName().toString());
            messager.printMessage(Diagnostic.Kind.NOTE,"typeMirror: " + typeElement.toString());
            fieldClass[i] = ClassName.get(typeElement);
        }
        return fieldClass;
    }

    //获取字段参数化类型
    private IdentityHashMap<String,Class<?>> getFieldParameterizedTypes(AnnotationMessage annotationMessage) throws ClassNotFoundException {
        IdentityHashMap<String,Class<?>> identityHashMap = new IdentityHashMap<>();
        String[] parameterizedTypes = annotationMessage.annotation.parameterizedType();
        for (int i=0; i<parameterizedTypes.length; i++){
            parameterizedTypesCheck(parameterizedTypes[i],annotationMessage);
            String[] content = parameterizedTypes[i].split("-");
            String key = new String(content[0]);
            Class<?> value = Class.forName(content[1]);
            identityHashMap.put(key,value);
        }
        return identityHashMap;
    }

    //转成TypeElement
    private TypeElement asTypeElement(TypeMirror typeMirror){
        return (TypeElement) typeUtils.asElement(typeMirror);
    }

    //生成java文件
    private void javaFileCreate() throws IOException {
        for (AnnotationMessage message:annotationMessages){
            //构造字段
            List<FieldSpec> fieldSpecs = new ArrayList<>();
            for (int i=0; i<message.fields.length; i++){
                String fieldName = message.fields[i];
                ClassName type;
                //如果没有指定字段类型，默认为String
                if (message.fieldsType != null && message.fieldsType.length >0){
                    type = message.fieldsType[i];
                    messager.printMessage(Diagnostic.Kind.NOTE,"ClassName: " + type.toString());
                }else {
                    type = ClassName.get(String.class);
                }

                //TODO 泛型不知道如何添加,待扩展

                FieldSpec fieldSpec = FieldSpec
                        .builder(type,fieldName)
                        .addModifiers(Modifier.PUBLIC)
                        .build();

                fieldSpecs.add(fieldSpec);
            }

            //构造类
            TypeSpec typeSpec = TypeSpec
                    .classBuilder(message.className) //类名
                    .addModifiers(Modifier.PUBLIC)   //public属性
                    .addFields(fieldSpecs)           //字段
                    .build();

            //生成文件
            JavaFile javaFile = JavaFile.builder(getPackageName(message.typeElement),typeSpec).build();
            javaFile.writeTo(filer);
        }
    }

    //对注解的合法性进行校验
    private void annotationCheck(){

        messager.printMessage(Diagnostic.Kind.NOTE,"CustomClassCompiler: " + annotationMessages.size()  + " class will be created");

        for (AnnotationMessage message:annotationMessages){

            if(message.fields == null){
                throw new RuntimeException("error: fields is null!");
            }

            if (message.fieldsType.length >0 && message.fieldsType.length != message.fields.length){
                throwAnnotationError(message,"fields和fieldsType数量不一致！！！");
            }

            parameterizedTypesMatchCheck(message);

            if (message.parameterizedTypes != null){
                for (IdentityHashMap.Entry<String,Class<?>> map:message.parameterizedTypes.entrySet()){
                    messager.printMessage(Diagnostic.Kind.NOTE,"parameterizedTypes: key=" + map.getKey() + " value=" + map.getValue());
                }
            }
        }
    }

    //泛型可行性校验,基础类型和String不能指定泛型
    private void parameterizedTypesMatchCheck(AnnotationMessage message){
        //如果parameterizedTypes为空说明没有指定任何泛型，则无需校验了
        if (message.parameterizedTypes == null || message.parameterizedTypes.size() == 0)return;
        //如果fieldsType为空说明默认是String类型，而parameterizedTypes一定有值，则抛出异常
        if (message.fieldsType == null || message.fieldsType.length == 0){
            throwAnnotationError(message,"String不能指定泛型！！！");
        }
        //否则fieldsType不为空，则遍历判断
        for (int i=0; i<message.fields.length; i++){
            //获取当前的字段名称和字段类型
            String fieldName = message.fields[i];
            ClassName fieldType = message.fieldsType[i];
            //如果不是基础类型continue
            if (!isBaseType(fieldType))continue;
            //如果当前字段是基础类型，遍历泛型容器（无法直接get,因为IdentityHashMap的特殊性），
            //如果有对应key=fieldName，则说明指定了该字段的泛型，则抛出异常
            for (String key:message.parameterizedTypes.keySet()){
                if (key.equals(fieldName)){
                    throwAnnotationError(message,fieldType.toString() + "不能指定泛型！！！");
                }
            }
        }
    }

    //是否为基础类型，这里包含了String
    private boolean isBaseType(ClassName fieldType){
        String className = fieldType.toString();
        return
            className.equals(String.class.getName()) ||
            className.equals(Character.class.getName()) ||
            className.equals(Byte.class.getName()) ||
            className.equals(Long.class.getName()) ||
            className.equals(Integer.class.getName()) ||
            className.equals(Short.class.getName()) ||
            className.equals(Double.class.getName()) ||
            className.equals(Float.class.getName()) ||
            className.equals(Boolean.class.getName()) ;
    }

    //校验参数化类型的格式是否正确
    private void parameterizedTypesCheck(String parameterizedTypes, AnnotationMessage annotationMessage){
        String rule = "^.+-.+$";
        if (!Pattern.matches(rule,parameterizedTypes)){
            String errorMessage = "正确格式： parameterizedType = {\"fieldName-classType\"}" +
                    "\n如： parameterizedType = {\"map-java.lang.Integer\"}";
            throwAnnotationError(annotationMessage,errorMessage);
        }
    }

    //抛出注解异常
    private void throwAnnotationError(AnnotationMessage annotationMessage,String errorMessage){
        throw new IllegalArgumentException(
                annotationMessage.typeElement.getQualifiedName()
                        + "\n"
                        + "注解错误：" + annotationMessage.annotation.toString()
                        + "\n"
                        + errorMessage );
    }
}

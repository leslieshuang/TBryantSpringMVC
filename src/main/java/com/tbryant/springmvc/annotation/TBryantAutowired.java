package com.tbryant.springmvc.annotation;

import java.lang.annotation.*;

//描述注解使用范围
//          构造方法 CONSTRUCTOR         方法 METHOD         参数 PARAMETER           域 FIELD       注解类 ANNOTATION_TYPE
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
//描述注解生命周期
//            在运行时有效 RUNTIME
@Retention(RetentionPolicy.RUNTIME)
//自动添加注释
@Documented
public @interface TBryantAutowired {
	String value() default "";
}

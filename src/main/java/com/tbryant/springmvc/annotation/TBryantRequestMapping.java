package com.tbryant.springmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TBryantRequestMapping {
	String value() default "";
}

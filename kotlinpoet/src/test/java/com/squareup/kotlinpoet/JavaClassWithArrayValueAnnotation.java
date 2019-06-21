package com.squareup.kotlinpoet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.squareup.kotlinpoet.JavaClassWithArrayValueAnnotation.AnnotationWithArrayValue;

@AnnotationWithArrayValue({
    Object.class, Boolean.class
})
public class JavaClassWithArrayValueAnnotation {

  @Retention(RetentionPolicy.RUNTIME)
  @interface AnnotationWithArrayValue {
    Class[] value();
  }

}
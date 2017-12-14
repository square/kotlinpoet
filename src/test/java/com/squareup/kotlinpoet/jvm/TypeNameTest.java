/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.jvm;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.ParameterizedTypeName;
import com.squareup.kotlinpoet.TypeName;
import com.squareup.kotlinpoet.TypeNames;
import com.squareup.kotlinpoet.TypeVariableName;
import com.squareup.kotlinpoet.WildcardTypeName;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class TypeNameTest {

  protected <E extends Enum<E>> E generic(E[] values) {
    return values[0];
  }

  protected static class TestGeneric<T> {
    class Inner {}

    class InnerGeneric<T2> {}

    static class NestedNonGeneric {}
  }

  protected static TestGeneric<String>.Inner testGenericStringInner() {
    return null;
  }

  protected static TestGeneric<Integer>.Inner testGenericIntInner() {
    return null;
  }

  protected static TestGeneric<Short>.InnerGeneric<Long> testGenericInnerLong() {
    return null;
  }

  protected static TestGeneric<Short>.InnerGeneric<Integer> testGenericInnerInt() {
    return null;
  }

  protected static TestGeneric.NestedNonGeneric testNestedNonGeneric() {
    return null;
  }

  @Test public void genericType() throws Exception {
    Method recursiveEnum = getClass().getDeclaredMethod("generic", Enum[].class);
    TypeNamesJvm.get(recursiveEnum.getReturnType());
    TypeNamesJvm.get(recursiveEnum.getGenericReturnType());
    TypeName genericTypeName = TypeNamesJvm.get(recursiveEnum.getParameterTypes()[0]);
    TypeNamesJvm.get(recursiveEnum.getGenericParameterTypes()[0]);

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).contains("Enum");
  }

  @Test public void innerClassInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericStringInner");
    TypeNamesJvm.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeNamesJvm.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeNamesJvm.get(genericStringInner.getGenericReturnType()),
        TypeNamesJvm.get(getClass().getDeclaredMethod("testGenericIntInner").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + "<java.lang.String>.Inner");
  }

  @Test public void innerGenericInGenericType() throws Exception {
    Method genericStringInner = getClass().getDeclaredMethod("testGenericInnerLong");
    TypeNamesJvm.get(genericStringInner.getReturnType());
    TypeName genericTypeName = TypeNamesJvm.get(genericStringInner.getGenericReturnType());
    assertNotEquals(TypeNamesJvm.get(genericStringInner.getGenericReturnType()),
        TypeNamesJvm.get(getClass().getDeclaredMethod("testGenericInnerInt").getGenericReturnType()));

    // Make sure the generic argument is present
    assertThat(genericTypeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + "<java.lang.Short>.InnerGeneric<java.lang.Long>");
  }

  @Test public void innerStaticInGenericType() throws Exception {
    Method staticInGeneric = getClass().getDeclaredMethod("testNestedNonGeneric");
    TypeNamesJvm.get(staticInGeneric.getReturnType());
    TypeName typeName = TypeNamesJvm.get(staticInGeneric.getGenericReturnType());

    // Make sure there are no generic arguments
    assertThat(typeName.toString()).isEqualTo(
        TestGeneric.class.getCanonicalName() + ".NestedNonGeneric");
  }

  @Test public void equalsAndHashCodePrimitive() {
    assertEqualsHashCodeAndToString(TypeNames.BOOLEAN, TypeNames.BOOLEAN);
    assertEqualsHashCodeAndToString(TypeNames.BYTE, TypeNames.BYTE);
    assertEqualsHashCodeAndToString(TypeNames.CHAR, TypeNames.CHAR);
    assertEqualsHashCodeAndToString(TypeNames.DOUBLE, TypeNames.DOUBLE);
    assertEqualsHashCodeAndToString(TypeNames.FLOAT, TypeNames.FLOAT);
    assertEqualsHashCodeAndToString(TypeNames.INT, TypeNames.INT);
    assertEqualsHashCodeAndToString(TypeNames.LONG, TypeNames.LONG);
    assertEqualsHashCodeAndToString(TypeNames.SHORT, TypeNames.SHORT);
    assertEqualsHashCodeAndToString(TypeNames.UNIT, TypeNames.UNIT);
  }

  @Test public void equalsAndHashCodeClassName() {
    assertEqualsHashCodeAndToString(ClassNamesJvm.get(Object.class), ClassNamesJvm.get(Object.class));
    assertEqualsHashCodeAndToString(TypeNamesJvm.get(Object.class), ClassNamesJvm.get(Object.class));
    assertEqualsHashCodeAndToString(ClassName.bestGuess("java.lang.Object"),
        ClassNamesJvm.get(Object.class));
  }

  @Test public void equalsAndHashCodeParameterizedTypeName() {
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(List.class, Object.class),
        ParameterizedTypeName.get(List.class, Object.class));
    assertEqualsHashCodeAndToString(ParameterizedTypeName.get(Set.class, UUID.class),
        ParameterizedTypeName.get(Set.class, UUID.class));
    assertNotEquals(ClassNamesJvm.get(List.class), ParameterizedTypeName.get(List.class,
        String.class));
  }

  @Test public void equalsAndHashCodeTypeVariableName() {
    assertEqualsHashCodeAndToString(TypeVariableName.get("A"),
        TypeVariableName.get("A"));
    TypeVariableName typeVar1 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    TypeVariableName typeVar2 = TypeVariableName.get("T", Comparator.class, Serializable.class);
    assertEqualsHashCodeAndToString(typeVar1, typeVar2);
  }

  @Test public void equalsAndHashCodeWildcardTypeName() {
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Object.class),
        WildcardTypeName.subtypeOf(Object.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.subtypeOf(Serializable.class),
        WildcardTypeName.subtypeOf(Serializable.class));
    assertEqualsHashCodeAndToString(WildcardTypeName.supertypeOf(String.class),
        WildcardTypeName.supertypeOf(String.class));
  }

  private void assertEqualsHashCodeAndToString(TypeName a, TypeName b) {
    assertEquals(a.toString(), b.toString());
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertFalse(a.equals(null));
  }
}

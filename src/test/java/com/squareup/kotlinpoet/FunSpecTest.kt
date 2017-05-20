/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.kotlinpoet

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables.getOnlyElement
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationRule
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.io.Closeable
import java.io.IOException
import java.util.Arrays
import java.util.concurrent.Callable
import java.util.concurrent.TimeoutException
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter.methodsIn
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class FunSpecTest {
  @Rule @JvmField val compilation = CompilationRule()

  private lateinit var elements: Elements
  private lateinit var types: Types

  @Before fun setUp() {
    elements = compilation.elements
    types = compilation.types
  }

  private fun getElement(clazz: Class<*>): TypeElement {
    return elements.getTypeElement(clazz.canonicalName)
  }

  private fun findFirst(elements: Collection<ExecutableElement>, name: String): ExecutableElement {
    for (executableElement in elements) {
      if (executableElement.simpleName.toString() == name) {
        return executableElement
      }
    }
    throw IllegalArgumentException(name + " not found in " + elements)
  }

  @Target(AnnotationTarget.VALUE_PARAMETER)
  internal annotation class Nullable

  internal abstract class Everything {
    @Deprecated("") @Throws(IOException::class, SecurityException::class)
    protected abstract fun <T> everything(
        @Nullable thing: String, things: List<T>): Runnable where T : Runnable, T : Closeable
  }

  internal abstract class HasAnnotation {
    abstract override fun toString(): String
  }

  internal interface ExtendsOthers : Callable<Int>, Comparable<Long>

  internal interface ExtendsIterableWithDefaultMethods : Iterable<Any>

  @Test fun overrideEverything() {
    val classElement = getElement(Everything::class.java)
    val methodElement = getOnlyElement(methodsIn(classElement.enclosedElements))
    val funSpec = FunSpec.overriding(methodElement).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@java.lang.Override
        |protected fun <T : java.lang.Runnable & java.io.Closeable> everything(arg0: java.lang.String,
        |    arg1: java.util.List<out T>): java.lang.Runnable throws java.io.IOException,
        |    java.lang.SecurityException {
        |}
        |""".trimMargin())
  }

  @Test fun overrideDoesNotCopyOverrideAnnotation() {
    val classElement = getElement(HasAnnotation::class.java)
    val exec = getOnlyElement(methodsIn(classElement.enclosedElements))
    val funSpec = FunSpec.overriding(exec).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@java.lang.Override
        |fun toString(): java.lang.String {
        |}
        |""".trimMargin())
  }

  @Test fun overrideExtendsOthersWorksWithActualTypeParameters() {
    val classElement = getElement(ExtendsOthers::class.java)
    val classType = classElement.asType() as DeclaredType
    val methods = methodsIn(elements.getAllMembers(classElement))
    var exec = findFirst(methods, "call")
    var funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@java.lang.Override
        |fun call(): java.lang.Integer throws java.lang.Exception {
        |}
        |""".trimMargin())
    exec = findFirst(methods, "compareTo")
    funSpec = FunSpec.overriding(exec, classType, types).build()
    assertThat(funSpec.toString()).isEqualTo("""
        |@java.lang.Override
        |fun compareTo(arg0: java.lang.Long): kotlin.Int {
        |}
        |""".trimMargin())
  }

  @Test fun overrideInvalidModifiers() {
    val method = mock(ExecutableElement::class.java)
    whenMock(method.modifiers).thenReturn(ImmutableSet.of(Modifier.FINAL))
    val element = mock(Element::class.java)
    whenMock(element.asType()).thenReturn(mock(DeclaredType::class.java))
    whenMock(method.enclosingElement).thenReturn(element)
    try {
      FunSpec.overriding(method)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [final]")
    }

    whenMock(method.modifiers).thenReturn(ImmutableSet.of(Modifier.PRIVATE))
    try {
      FunSpec.overriding(method)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [private]")
    }

    whenMock(method.modifiers).thenReturn(ImmutableSet.of(Modifier.STATIC))
    try {
      FunSpec.overriding(method)
      fail()
    } catch (expected: IllegalArgumentException) {
      assertThat(expected).hasMessage("cannot override method with modifiers: [static]")
    }
  }

  @Test fun nullableParam() {
    val funSpec = FunSpec.builder("foo")
        .addParameter(ParameterSpec.builder(TypeName.get(String::class).asNullable(), "string")
            .build())
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(string: java.lang.String?) {
      |}
      |""".trimMargin())
  }

  @Test fun nullableReturnType() {
    val funSpec = FunSpec.builder("foo")
        .returns(TypeName.get(String::class).asNullable())
        .build()
    assertThat(funSpec.toString()).isEqualTo("""
      |fun foo(): java.lang.String? {
      |}
      |""".trimMargin())
  }

  @Test fun equalsAndHashCode() {
    var a = FunSpec.constructorBuilder().build()
    var b = FunSpec.constructorBuilder().build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = FunSpec.builder("taco").build()
    b = FunSpec.builder("taco").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    val classElement = getElement(Everything::class.java)
    val methodElement = getOnlyElement(methodsIn(classElement.enclosedElements))
    a = FunSpec.overriding(methodElement).build()
    b = FunSpec.overriding(methodElement).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun duplicateExceptionsIgnored() {
    val ioException = ClassName.get(IOException::class)
    val timeoutException = ClassName.get(TimeoutException::class)
    val funSpec = FunSpec.builder("duplicateExceptions")
        .addException(ioException)
        .addException(timeoutException)
        .addException(timeoutException)
        .addException(ioException)
        .build()
    assertThat(funSpec.exceptions).isEqualTo(Arrays.asList(ioException, timeoutException))
    assertThat(funSpec.toBuilder().addException(ioException).build().exceptions)
        .isEqualTo(Arrays.asList(ioException, timeoutException))
  }

  private fun whenMock(any: Any?) = Mockito.`when`(any)
}

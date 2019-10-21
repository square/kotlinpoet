/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.kotlinpoet.metadata.specs.test

import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.classinspector.reflective.ReflectiveClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import org.junit.Assume
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass

@KotlinPoetMetadataPreview
abstract class MultiClassInspectorTest {
  companion object {
    @Suppress("RedundantLambdaArrow") // Needed for lambda type resolution
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<*>> {
      return listOf(
          arrayOf<Any>(
              ClassInspectorType.REFLECTIVE,
              { _: MultiClassInspectorTest -> { ReflectiveClassInspector.create() } }
          ),
          arrayOf<Any>(
              ClassInspectorType.ELEMENTS,
              { test: MultiClassInspectorTest -> {
                ElementsClassInspector.create(test.compilation.elements, test.compilation.types)
              } }
          )
      )
    }
  }

  enum class ClassInspectorType {
    REFLECTIVE, ELEMENTS
  }

  @Retention(RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  @Inherited
  annotation class IgnoreForHandlerType(
      val reason: String,
      val handlerType: ClassInspectorType
  )

  class IgnoreForElementsRule(private val handlerType: ClassInspectorType) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
      return object : Statement() {
        override fun evaluate() {
          val annotation = description.getAnnotation(
              IgnoreForHandlerType::class.java)
          val shouldIgnore = annotation?.handlerType == handlerType
          Assume.assumeTrue(
              "Ignoring ${description.methodName}: ${annotation?.reason}",
              !shouldIgnore
          )
          base.evaluate()
        }
      }
    }
  }
  
  abstract val classInspectorType: ClassInspectorType
  abstract val classInspectorFactoryCreator: (MultiClassInspectorTest) -> (() -> ClassInspector)

  @Rule
  @JvmField
  protected val compilation = CompilationRule()

  @Rule
  @JvmField
  protected val ignoreForElementsRule = IgnoreForElementsRule(
      classInspectorType)

  protected fun KClass<*>.toTypeSpecWithTestHandler(): TypeSpec {
    return toTypeSpec(classInspectorFactoryCreator(this@MultiClassInspectorTest)())
  }
}

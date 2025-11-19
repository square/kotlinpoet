/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.metadata.specs

import com.google.testing.compile.CompilationRule
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.classinspectors.ReflectiveClassInspector
import com.squareup.kotlinpoet.metadata.specs.MultiClassInspectorTest.ClassInspectorType
import com.squareup.kotlinpoet.metadata.toKotlinClassMetadata
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.metadata.jvm.KotlinClassMetadata.FileFacade
import kotlin.reflect.KClass
import org.junit.Assume
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.model.Statement

/** Base test class that runs all tests with multiple [ClassInspectorTypes][ClassInspectorType]. */
@RunWith(Parameterized::class)
abstract class MultiClassInspectorTest {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<ClassInspectorType>> {
      return listOf(arrayOf(ClassInspectorType.REFLECTIVE), arrayOf(ClassInspectorType.ELEMENTS))
    }
  }

  enum class ClassInspectorType {
    NONE {
      override fun create(testInstance: MultiClassInspectorTest): ClassInspector {
        throw IllegalStateException(
          "Should not be called, just here to default the jvmfield to something."
        )
      }
    },
    REFLECTIVE {
      override fun create(testInstance: MultiClassInspectorTest): ClassInspector {
        return ReflectiveClassInspector.create(lenient = false)
      }
    },
    ELEMENTS {
      override fun create(testInstance: MultiClassInspectorTest): ClassInspector {
        return ElementsClassInspector.create(
          lenient = false,
          testInstance.compilation.elements,
          testInstance.compilation.types,
        )
      }
    };

    abstract fun create(testInstance: MultiClassInspectorTest): ClassInspector
  }

  @Retention(RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  @Inherited
  annotation class IgnoreForHandlerType(val reason: String, val handlerType: ClassInspectorType)

  @JvmField @Parameter var classInspectorType: ClassInspectorType = ClassInspectorType.NONE

  @Rule @JvmField val compilation = CompilationRule()

  @Rule
  @JvmField
  val ignoreForElementsRule = TestRule { base, description ->
    object : Statement() {
      override fun evaluate() {
        val annotation = description.getAnnotation(IgnoreForHandlerType::class.java)
        val shouldIgnore = annotation?.handlerType == classInspectorType
        Assume.assumeTrue(
          "Ignoring ${description.methodName}: ${annotation?.reason}",
          !shouldIgnore,
        )
        base.evaluate()
      }
    }
  }

  protected fun KClass<*>.toTypeSpecWithTestHandler(): TypeSpec {
    return toTypeSpec(lenient = false, classInspectorType.create(this@MultiClassInspectorTest))
  }

  protected fun KClass<*>.toFileSpecWithTestHandler(): FileSpec {
    val classInspector = classInspectorType.create(this@MultiClassInspectorTest)
    return java.annotations
      .filterIsInstance<Metadata>()
      .first()
      .toKotlinClassMetadata<FileFacade>(lenient = false)
      .kmPackage
      .toFileSpec(classInspector, asClassName())
  }
}

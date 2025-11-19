/*
 * Copyright (C) 2021 Square, Inc.
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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.classinspectors.ReflectiveClassInspector
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test

/**
 * Class to test the new functionality of Issue#1036.
 *
 * @see <a href="https://github.com/square/kotlinpoet/issues/1036">issue</a>
 * @author oberstrike
 */
class ReflectiveClassInspectorTest {

  data class Person(val name: String)

  /**
   * Tests if the [ReflectiveClassInspector] can be created without a custom ClassLoader and still
   * works.
   */
  @Test
  fun standardClassLoaderTest() {
    val classInspector = ReflectiveClassInspector.create(lenient = false)
    val className = Person::class.asClassName()
    val declarationContainer = classInspector.declarationContainerFor(className)
    assertNotNull(declarationContainer)
  }

  /** Tests if the [ReflectiveClassInspector] can be created with a custom ClassLoader. */
  @Test
  fun useACustomClassLoaderTest() {
    val testClass = "Person"
    val testPropertyName = "name"
    val testPropertyType = "String"
    val testPackageName = "com.test"
    val testClassName = ClassName(testPackageName, testClass)
    val testKtFileName = "KClass.kt"

    val kotlinSource =
      SourceFile.kotlin(
        testKtFileName,
        """
            package $testPackageName
            data class $testClass(val $testPropertyName: $testPropertyType)
      """
          .trimIndent(),
      )

    val result = KotlinCompilation().apply { sources = listOf(kotlinSource) }.compile()

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val classLoader = result.classLoader
    val classInspector = ReflectiveClassInspector.create(lenient = false, classLoader)

    val declarationContainer = classInspector.declarationContainerFor(testClassName)

    val properties = declarationContainer.properties
    assertEquals(1, properties.size)

    val testProperty = properties.findLast { it.name == testPropertyName }
    assertNotNull(testProperty)

    val returnType = testProperty.returnType
    assertNotNull(returnType)
  }
}

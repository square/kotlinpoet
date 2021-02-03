package com.squareup.kotlinpoet.metadata.specs.test

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.classinspector.reflective.ReflectiveClassInspector
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Class to test the new functionality of Issue#1036.
 * @see <a href="https://github.com/square/kotlinpoet/issues/1036">issue</a>
 * @author oberstrike
 */
@KotlinPoetMetadataPreview
class ReflectiveClassInspectorTest {

    data class Person(val name: String)

    /**
     * Tests if the [ReflectiveClassInspector] can be created without a
     * custom ClassLoader and still works.
     */
    @Test
    fun standardClassLoaderTest() {
        val classInspector = ReflectiveClassInspector.create()
        val className = Person::class.asClassName()
        val declarationContainer = classInspector.declarationContainerFor(className)
        assertNotNull(declarationContainer)
    }

    /**
     * Tests if the [ReflectiveClassInspector] can be created with a
     * custom ClassLoader.
     */
    @Test
    fun useACustomClassLoaderTest() {
        val testClass = "Person"
        val testPropertyName = "name"
        val testPropertyType = "String"
        val testPackageName = "com.test"
        val testClassName = ClassName(testPackageName, testClass)
        val testKtFileName = "KClass.kt"

        val kotlinSource = SourceFile.kotlin(
            testKtFileName,
            """
            package $testPackageName
            data class $testClass(val $testPropertyName: $testPropertyType)
            """.trimIndent()
        )

        val result = KotlinCompilation().apply {
            sources = listOf(kotlinSource)
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val classLoader = result.classLoader
        val classInspector = ReflectiveClassInspector.create(classLoader)

        val declarationContainer = classInspector.declarationContainerFor(testClassName)

        val properties = declarationContainer.properties
        assertEquals(1, properties.size)

        val testProperty = properties.findLast { it.name == testPropertyName }
        assertNotNull(testProperty)

        val returnType = testProperty.returnType
        assertNotNull(returnType)
    }

}

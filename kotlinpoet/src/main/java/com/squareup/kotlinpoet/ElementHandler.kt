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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.ElementHandler.Companion.fromElements
import com.squareup.kotlinpoet.ElementHandler.Companion.reflective
import com.squareup.kotlinpoet.km.ImmutableKmClass
import com.squareup.kotlinpoet.km.KotlinPoetKm
import com.squareup.kotlinpoet.km.toImmutableKmClass
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * A basic interface for looking up information about JVM elements.
 *
 * @see [reflective] for a reflection-based implementation or [fromElements] for an [Elements]-based
 * implementation.
 */
@KotlinPoetKm
interface ElementHandler {

  interface JvmModifier {
    fun annotationSpec(): AnnotationSpec
  }

  /** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
  enum class JvmFieldModifier : JvmModifier {
    STATIC {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(JvmStatic::class.asClassName()).build()
    },
    TRANSIENT {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Transient::class.asClassName()).build()
    },
    VOLATILE {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Volatile::class.asClassName()).build()
    };
  }

  /** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
  enum class JvmMethodModifier : JvmModifier {
    STATIC {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(JvmStatic::class.asClassName()).build()
    },
    SYNCHRONIZED {
      override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(Synchronized::class.asClassName()).build()
    }
  }

  /**
   * Looks up other classes, such as for nested members. Note that this class would always be
   * Kotlin, so Metadata can be relied on for this.
   *
   * @param jvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @return the read [ImmutableKmClass] from its metadata. If no class was found, this should throw
   *         an exception.
   */
  fun classFor(jvmName: String): ImmutableKmClass

  /**
   * Looks up a class and returns whether or not it is an interface. Note that this class can be
   * Java or Kotlin, so Metadata should not be relied on for this.
   *
   * @param jvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @return whether or not it is an interface.
   */
  fun isInterface(jvmName: String): Boolean

  /**
   * Looks up a given class field given its [JvmFieldSignature] and returns any [JvmModifier]s
   * found on it.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field to look up.
   * @return the set of found modifiers.
   */
  fun fieldJvmModifiers(classJvmName: String, fieldSignature: JvmFieldSignature): Set<JvmFieldModifier>

  /**
   * Looks up the annotations on a given class field given its [JvmFieldSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target field.
   */
  fun fieldAnnotations(classJvmName: String, fieldSignature: JvmFieldSignature): List<AnnotationSpec>

  /**
   * Looks up a given class method given its [JvmMethodSignature] and returns any [JvmMethodModifier]s
   * found on it.
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method with annotations to look up.
   * @return the set of found modifiers.
   */
  fun methodJvmModifiers(classJvmName: String, methodSignature: JvmMethodSignature): Set<JvmMethodModifier>

  /**
   * Looks up the annotations on a given class constructor given its [JvmMethodSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param constructorSignature The constructor with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target method.
   */
  fun constructorAnnotations(classJvmName: String, constructorSignature: JvmMethodSignature): List<AnnotationSpec>

  /**
   * Looks up the annotations on a given class method given its [JvmMethodSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param methodSignature The method with annotations to look up.
   * @return the [AnnotationSpec] representations of the annotations on the target method.
   */
  fun methodAnnotations(classJvmName: String, methodSignature: JvmMethodSignature): List<AnnotationSpec>

  /**
   * Looks up the enum entry on a given enum given its member name.
   *
   * @param enumClassJvmName The JVM name of the enum class (example: `"org/foo/bar/Baz$Nested"`).
   * @param memberName The simple member name.
   * @return the read [ImmutableKmClass] from its metadata, if any. For simple enum members with no
   *         class bodies, this should always be null.
   */
  fun enumEntry(enumClassJvmName: String, memberName: String): ImmutableKmClass?

  /**
   * Looks up the constant value on a given class field given its [JvmFieldSignature].
   *
   * @param classJvmName The JVM name of the class (example: `"org/foo/bar/Baz$Nested"`).
   * @param fieldSignature The field with annotations to look up.
   * @return the [CodeBlock] representation of the constant on the target field. Null if the value
   *         cannot be resolved.
   */
  fun fieldConstant(classJvmName: String, fieldSignature: JvmFieldSignature): CodeBlock?

  companion object {
    private fun Any.asLiteralCodeBlock(): CodeBlock {
      return when (this) {
        is String -> CodeBlock.of("%S", this)
        is Long -> CodeBlock.of("%LL", this)
        is Float -> CodeBlock.of("%LF", this)
        else -> CodeBlock.of("%L", this)
      }
    }

    private val String.canonicalName get() = replace("/", ".").replace("$", ".")

    private val KOTLIN_INTRINSIC_INTERFACES = setOf(
        "kotlin.CharSequence",
        "kotlin.Comparable",
        "kotlin.collections.Iterable",
        "kotlin.collections.Collection",
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Map",
        "kotlin.collections.Map.Entry",
        "kotlin.collections.MutableIterable",
        "kotlin.collections.MutableCollection",
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet",
        "kotlin.collections.MutableMap",
        "kotlin.collections.MutableMap.Entry"
    )

    private val KOTLIN_NULLABILITY_ANNOTATIONS = setOf(
        "org.jetbrains.annotations.NotNull",
        "org.jetbrains.annotations.Nullable"
    )

    private fun List<AnnotationSpec>.filterOutNullabilityAnnotations() = filterNot { it.className.canonicalName in KOTLIN_NULLABILITY_ANNOTATIONS }

    /** A reflection-based implementation of [ElementHandler]. */
    fun reflective(): ElementHandler = object : ElementHandler {

      private val classCache = ConcurrentHashMap<String, Optional<Class<*>>>()
      private val methodCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Method>>()
      private val constructorCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Constructor<*>>>()
      private val fieldCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Field>>()
      private val enumCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Any>>()

      private fun lookupClass(jvmName: String): Class<*>? {
        return classCache.getOrPut(jvmName) {
          try {
            Class.forName(jvmName.replace("/", "."))
          } catch (e: ClassNotFoundException) {
            null
          }.toOptional()
        }.nullableValue
      }

      override fun classFor(jvmName: String): ImmutableKmClass {
        return lookupClass(jvmName)?.toImmutableKmClass() ?: error("No class found for: $jvmName.")
      }

      override fun isInterface(jvmName: String): Boolean {
        if (jvmName.canonicalName in KOTLIN_INTRINSIC_INTERFACES) {
          return true
        }
        return lookupClass(jvmName)?.isInterface ?: false
      }

      private fun lookupField(classJvmName: String, fieldSignature: JvmFieldSignature): Field? {
        return try {
          val clazz = lookupClass(classJvmName) ?: error("No class found for: $classJvmName.")
          val signatureString = fieldSignature.asString()
          fieldCache.getOrPut(clazz to signatureString) {
            clazz.declaredFields
                .asSequence()
                .onEach { it.isAccessible = true }
                .find { signatureString == it.jvmFieldSignature }.toOptional()
          }.nullableValue
        } catch (e: ClassNotFoundException) {
          null
        }
      }

      private fun lookupMethod(
        classJvmName: String,
        methodSignature: JvmMethodSignature
      ): Method? {
        val clazz = lookupClass(classJvmName) ?: error("No class found for: $classJvmName.")
        val signatureString = methodSignature.asString()
        return methodCache.getOrPut(clazz to signatureString) {
          clazz.declaredMethods
              .asSequence()
              .onEach { it.isAccessible = true }
              .find { signatureString == it.jvmMethodSignature }.toOptional()
        }.nullableValue
      }

      override fun fieldJvmModifiers(
        classJvmName: String,
        fieldSignature: JvmFieldSignature
      ): Set<JvmFieldModifier> {
        return lookupField(classJvmName, fieldSignature)?.modifiers.let { modifiers ->
          if (modifiers != null) {
            return mutableSetOf<JvmFieldModifier>().apply {
              if (Modifier.isTransient(modifiers)) {
                add(JvmFieldModifier.TRANSIENT)
              }
              if (Modifier.isVolatile(modifiers)) {
                add(JvmFieldModifier.VOLATILE)
              }
            }
          }
          return@let emptySet()
        }
      }

      override fun fieldAnnotations(classJvmName: String, fieldSignature: JvmFieldSignature): List<AnnotationSpec> {
        return lookupField(classJvmName, fieldSignature)?.declaredAnnotations
            .orEmpty()
            .map { AnnotationSpec.get(it, true) }
            .filterOutNullabilityAnnotations()
      }

      override fun constructorAnnotations(
        classJvmName: String,
        constructorSignature: JvmMethodSignature
      ): List<AnnotationSpec> {
        val clazz = lookupClass(classJvmName) ?: error("No class found for: $classJvmName.")
        val signatureString = constructorSignature.asString()
        val constructor = constructorCache.getOrPut(clazz to signatureString) {
          clazz.declaredConstructors
              .asSequence()
              .onEach { it.isAccessible = true }
              .find { signatureString == it.jvmMethodSignature }.toOptional()
        }.nullableValue
        return constructor?.declaredAnnotations.orEmpty()
            .map { AnnotationSpec.get(it, true) }
            .filterOutNullabilityAnnotations()
      }

      override fun methodJvmModifiers(
        classJvmName: String,
        methodSignature: JvmMethodSignature
      ): Set<JvmMethodModifier> {
        return lookupMethod(classJvmName, methodSignature)?.modifiers.let { modifiers ->
          val jvmMethodModifiers = mutableSetOf<JvmMethodModifier>()
          if (modifiers != null) {
            if (Modifier.isSynchronized(modifiers)) {
              jvmMethodModifiers += JvmMethodModifier.SYNCHRONIZED
            }
            if (Modifier.isStatic(modifiers)) {
              jvmMethodModifiers += JvmMethodModifier.STATIC
            }
          }
          return@let jvmMethodModifiers
        }
      }

      override fun methodAnnotations(classJvmName: String, methodSignature: JvmMethodSignature): List<AnnotationSpec> {
        return try {
          lookupMethod(classJvmName, methodSignature)
              ?.declaredAnnotations
              .orEmpty()
              .map { AnnotationSpec.get(it, true) }
              .filterOutNullabilityAnnotations()
        } catch (e: ClassNotFoundException) {
          emptyList()
        }
      }

      override fun enumEntry(enumClassJvmName: String, memberName: String): ImmutableKmClass? {
        val clazz = lookupClass(enumClassJvmName)
            ?: error("No class found for: $enumClassJvmName.")
        check(clazz.isEnum) {
          "Class must be an enum but isn't: $clazz"
        }
        val enumEntry = enumCache.getOrPut(clazz to memberName) {
          clazz.enumConstants.find { (it as Enum<*>).name == memberName }.toOptional()
        }.nullableValue
        checkNotNull(enumEntry) {
          "Could not find $memberName on $enumClassJvmName"
        }
        if (enumEntry.javaClass == clazz) {
          // For simple enums with no class bodies, the entry class will be the same as the original
          // class.
          return null
        }
        return enumEntry.javaClass.getAnnotation(Metadata::class.java)?.toImmutableKmClass()
      }

      override fun fieldConstant(
        classJvmName: String,
        fieldSignature: JvmFieldSignature
      ): CodeBlock? {
        val field = lookupField(classJvmName, fieldSignature) ?: error("No field $fieldSignature found in $classJvmName.")
        if (!Modifier.isStatic(field.modifiers)) {
          return null
        }
        return field
            .get(null) // Constant means we can do a static get on it.
            .asLiteralCodeBlock()
      }
    }

    /** @return an [Elements]-based implementation of [ElementHandler]. */
    fun fromElements(elements: Elements, types: Types): ElementHandler {
      return object : ElementHandler {

        private val typeElementCache = ConcurrentHashMap<String, Optional<TypeElement>>()
        private val methodCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<ExecutableElement>>()
        private val variableElementCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<VariableElement>>()

        private fun lookupTypeElement(jvmName: String): TypeElement? {
          return typeElementCache.getOrPut(jvmName) {
            elements.getTypeElement(jvmName.canonicalName).toOptional()
          }.nullableValue
        }

        override fun classFor(jvmName: String): ImmutableKmClass {
          return lookupTypeElement(jvmName)?.toImmutableKmClass() ?: error("No type element found for: $jvmName.")
        }

        override fun isInterface(jvmName: String): Boolean {
          if (jvmName.canonicalName in KOTLIN_INTRINSIC_INTERFACES) {
            return true
          }
          return lookupTypeElement(jvmName)?.kind == INTERFACE
        }

        private fun lookupField(classJvmName: String, fieldSignature: JvmFieldSignature): VariableElement? {
          return lookupTypeElement(classJvmName)?.let {
            val signatureString = fieldSignature.asString()
            variableElementCache.getOrPut(it to signatureString) {
              ElementFilter.fieldsIn(it.enclosedElements)
                  .find { signatureString == it.jvmFieldSignature(types) }.toOptional()
            }.nullableValue
          }
        }

        private fun lookupMethod(classJvmName: String,
            methodSignature: JvmMethodSignature,
            elementFilter: (Iterable<Element>) -> List<ExecutableElement>
        ): ExecutableElement? {
          return lookupTypeElement(classJvmName)?.let {
            val signatureString = methodSignature.asString()
            methodCache.getOrPut(it to signatureString) {
              elementFilter(it.enclosedElements)
                  .find { signatureString == it.jvmMethodSignature(types) }.toOptional()
            }.nullableValue
          }
        }

        override fun fieldJvmModifiers(
          classJvmName: String,
          fieldSignature: JvmFieldSignature
        ): Set<JvmFieldModifier> {
          return lookupField(classJvmName, fieldSignature)?.modifiers?.let { modifiers ->
            modifiers.mapNotNullTo(mutableSetOf()) {
              when (it) {
                javax.lang.model.element.Modifier.TRANSIENT -> JvmFieldModifier.TRANSIENT
                javax.lang.model.element.Modifier.VOLATILE -> JvmFieldModifier.VOLATILE
                javax.lang.model.element.Modifier.STATIC -> JvmFieldModifier.STATIC
                else -> null
              }
            }
          }.orEmpty()
        }

        override fun fieldAnnotations(
          classJvmName: String,
          fieldSignature: JvmFieldSignature
        ): List<AnnotationSpec> {
          return lookupField(classJvmName, fieldSignature)
              ?.annotationMirrors
              .orEmpty()
              .map { AnnotationSpec.get(it) }
              .filterOutNullabilityAnnotations()
        }

        override fun methodJvmModifiers(
          classJvmName: String,
          methodSignature: JvmMethodSignature
        ): Set<JvmMethodModifier> {
          return lookupMethod(classJvmName, methodSignature, ElementFilter::methodsIn)?.modifiers?.let { modifiers ->
            modifiers.mapNotNullTo(mutableSetOf()) {
              when (it) {
                javax.lang.model.element.Modifier.SYNCHRONIZED -> JvmMethodModifier.SYNCHRONIZED
                javax.lang.model.element.Modifier.STATIC -> JvmMethodModifier.STATIC
                else -> null
              }
            }
          }.orEmpty()
        }

        override fun constructorAnnotations(
          classJvmName: String,
          constructorSignature: JvmMethodSignature
        ): List<AnnotationSpec> {
          return lookupMethod(classJvmName, constructorSignature, ElementFilter::constructorsIn)
                ?.annotationMirrors
                .orEmpty()
                .map { AnnotationSpec.get(it) }
                .filterOutNullabilityAnnotations()
        }

        override fun methodAnnotations(
          classJvmName: String,
          methodSignature: JvmMethodSignature
        ): List<AnnotationSpec> {
          return lookupMethod(classJvmName, methodSignature, ElementFilter::methodsIn)
                ?.annotationMirrors
                .orEmpty()
                .map { AnnotationSpec.get(it) }
                .filterOutNullabilityAnnotations()
        }

        override fun enumEntry(enumClassJvmName: String, memberName: String): ImmutableKmClass? {
          return lookupTypeElement(enumClassJvmName)?.let { enumType ->
            val enumTypeAsType = enumType.asType()
            val member = typeElementCache.getOrPut("$enumClassJvmName.$memberName") {
              ElementFilter.typesIn(enumType.enclosedElements)
                  .asSequence()
                  .filter { types.isSubtype(enumTypeAsType, it.superclass) }
                  .find { it.simpleName.contentEquals(memberName) }.toOptional()
            }.nullableValue
            member?.getAnnotation(Metadata::class.java)
                ?.toImmutableKmClass()
          }
        }

        override fun fieldConstant(
          classJvmName: String,
          fieldSignature: JvmFieldSignature
        ): CodeBlock? {
          return lookupField(classJvmName, fieldSignature)?.constantValue
              ?.asLiteralCodeBlock()
              ?: error("No field $fieldSignature found in $classJvmName.")
        }
      }
    }
  }
}

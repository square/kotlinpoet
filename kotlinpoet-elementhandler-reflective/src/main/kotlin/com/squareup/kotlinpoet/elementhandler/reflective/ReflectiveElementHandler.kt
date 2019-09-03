package com.squareup.kotlinpoet.elementhandler.reflective

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ElementHandler
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

@KotlinPoetMetadataPreview
class ReflectiveElementHandler private constructor() : ElementHandler {

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

  override val supportsNonRuntimeRetainedAnnotations: Boolean = false

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
    return clazz.lookupMethod(methodSignature)
  }

  private fun Class<*>.lookupMethod(
    methodSignature: JvmMethodSignature
  ): Method? {
    val signatureString = methodSignature.asString()
    return methodCache.getOrPut(this to signatureString) {
      declaredMethods
          .asSequence()
          .onEach { it.isAccessible = true }
          .find { signatureString == it.jvmMethodSignature }.toOptional()
    }.nullableValue
  }

  private fun lookupConstructor(
    classJvmName: String,
    constructorSignature: JvmMethodSignature
  ): Constructor<*>? {
    val clazz = lookupClass(classJvmName) ?: error("No class found for: $classJvmName.")
    return clazz.lookupConstructor(constructorSignature)
  }

  private fun Class<*>.lookupConstructor(
    constructorSignature: JvmMethodSignature
  ): Constructor<*>? {
    val signatureString = constructorSignature.asString()
    return constructorCache.getOrPut(this to signatureString) {
      declaredConstructors
          .asSequence()
          .onEach { it.isAccessible = true }
          .find { signatureString == it.jvmMethodSignature }.toOptional()
    }.nullableValue
  }

  override fun fieldJvmModifiers(
    classJvmName: String,
    fieldSignature: JvmFieldSignature,
    isJvmField: Boolean
  ): Set<JvmFieldModifier> {
    return lookupField(classJvmName, fieldSignature)?.modifiers.let { modifiers ->
      if (modifiers != null) {
        return mutableSetOf<JvmFieldModifier>().apply {
          if (Modifier.isTransient(modifiers)) {
            add(TRANSIENT)
          }
          if (Modifier.isVolatile(modifiers)) {
            add(VOLATILE)
          }
        }
      }
      return@let emptySet()
    }
  }

  override fun fieldAnnotations(
    classJvmName: String,
    fieldSignature: JvmFieldSignature
  ): List<AnnotationSpec> {
    return lookupField(classJvmName, fieldSignature)?.declaredAnnotations
        .orEmpty()
        .map { AnnotationSpec.get(it, true) }
        .filterOutNullabilityAnnotations()
  }

  override fun isFieldSynthetic(classJvmName: String, fieldSignature: JvmFieldSignature): Boolean {
    return lookupField(classJvmName, fieldSignature)?.isSynthetic ?: false
  }

  override fun constructorAnnotations(
    classJvmName: String,
    constructorSignature: JvmMethodSignature
  ): List<AnnotationSpec> {
    return lookupConstructor(classJvmName, constructorSignature)
        ?.declaredAnnotations.orEmpty()
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
          jvmMethodModifiers += SYNCHRONIZED
        }
        if (Modifier.isStatic(modifiers)) {
          jvmMethodModifiers += STATIC
        }
      }
      return@let jvmMethodModifiers
    }
  }

  override fun methodAnnotations(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): List<AnnotationSpec> {
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

  override fun isMethodSynthetic(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): Boolean {
    return lookupMethod(classJvmName, methodSignature)?.isSynthetic ?: false
  }

  override fun methodExceptions(
    classJvmName: String,
    methodSignature: JvmMethodSignature,
    isConstructor: Boolean
  ): Set<TypeName> {
    val exceptions = if (isConstructor) {
      lookupConstructor(classJvmName, methodSignature)?.exceptionTypes
    } else {
      lookupMethod(classJvmName, methodSignature)?.exceptionTypes
    }
    return exceptions.orEmpty().mapTo(mutableSetOf()) { it.asTypeName() }
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
    val field = lookupField(classJvmName, fieldSignature) ?: error(
        "No field $fieldSignature found in $classJvmName.")
    if (!Modifier.isStatic(field.modifiers)) {
      return null
    }
    return field
        .get(null) // Constant means we can do a static get on it.
        .asLiteralCodeBlock()
  }

  override fun isMethodOverride(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): Boolean {
    val clazz = lookupClass(classJvmName) ?: error("No class found for: $classJvmName.")
    val signatureString = methodSignature.asString()
    val classPackage = clazz.`package`.name
    val interfaceMethods = clazz.interfaces.asSequence()
        .flatMap { it.methods.asSequence() }
    val superClassMethods = clazz.superclass?.methods.orEmpty().asSequence()
    return interfaceMethods.plus(superClassMethods)
        .filterNot { Modifier.isFinal(it.modifiers) }
        .filterNot { Modifier.isStatic(it.modifiers) }
        .filterNot { Modifier.isPrivate(it.modifiers) }
        .filter {
          Modifier.isPublic(it.modifiers) ||
              Modifier.isProtected(it.modifiers) ||
              // Package private
              it.declaringClass.`package`.name == classPackage
        }
        .map { it.jvmMethodSignature }
        .any { it == signatureString }
  }

  override fun methodExists(classJvmName: String, methodSignature: JvmMethodSignature): Boolean {
    return lookupClass(classJvmName)?.lookupMethod(methodSignature) != null
  }

  companion object {
    @JvmStatic
    @KotlinPoetMetadataPreview
    fun create(): ElementHandler {
      return ReflectiveElementHandler()
    }

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

    private val Class<*>.descriptor: String get() {
      return when {
        isPrimitive -> when (kotlin) {
          Byte::class -> "B"
          Char::class -> "C"
          Double::class -> "D"
          Float::class -> "F"
          Int::class -> "I"
          Long::class -> "J"
          Short::class -> "S"
          Boolean::class -> "Z"
          Void::class -> "V"
          else -> throw RuntimeException("Unrecognized primitive $this")
        }
        isArray -> name.replace('.', '/')
        else -> "L$name;".replace('.', '/')
      }
    }

    private val Method.descriptor: String get() {
      return buildString {
        append('(')
        parameterTypes.joinTo(this, separator = "", transform = { it.descriptor })
        append(')')
        append(returnType.descriptor)
      }
    }

    /**
     * Returns the JVM signature in the form "$Name$MethodDescriptor", for example: `equals(Ljava/lang/Object;)Z`.
     *
     * Useful for comparing with [JvmMethodSignature].
     *
     * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Method.jvmMethodSignature: String get() = "$name$descriptor"

    private val Constructor<*>.descriptor: String get() {
      return buildString {
        append('(')
        parameterTypes.joinTo(this, separator = "", transform = { it.descriptor })
        append(')')
        append('V')
      }
    }

    /**
     * Returns the JVM signature in the form "<init>$MethodDescriptor", for example: `"<init>(Ljava/lang/Object;)V")`.
     *
     * Useful for comparing with [JvmMethodSignature].
     *
     * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Constructor<*>.jvmMethodSignature: String get() = "<init>$descriptor"

    /**
     * Returns the JVM signature in the form "$Name:$FieldDescriptor", for example: `"value:Ljava/lang/String;"`.
     *
     * Useful for comparing with [JvmFieldSignature].
     *
     * For reference, see the [JVM specification, section 4.3](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Field.jvmFieldSignature: String get() = "$name:${type.descriptor}"
  }
}

/**
 * Simple `Optional` implementation for use in collections that don't allow `null` values.
 *
 * TODO: Make this an inline class when inline classes are stable.
 */
private data class Optional<out T : Any>(val nullableValue: T?)
private fun <T : Any> T?.toOptional(): Optional<T> = Optional(this)

package com.squareup.kotlinpoet.elementhandler.reflective

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.hasAnnotations
import com.squareup.kotlinpoet.metadata.isAnnotation
import com.squareup.kotlinpoet.metadata.isCompanionObject
import com.squareup.kotlinpoet.metadata.isDeclaration
import com.squareup.kotlinpoet.metadata.isInline
import com.squareup.kotlinpoet.metadata.isSynthesized
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.ConstructorData
import com.squareup.kotlinpoet.metadata.specs.ElementHandler
import com.squareup.kotlinpoet.metadata.specs.ElementHandler.Companion.computeIsJvmField
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.specs.MethodData
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.jvmInternalName
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.NONE

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

  private fun Class<*>.lookupField(fieldSignature: JvmFieldSignature): Field? {
    return try {
      val signatureString = fieldSignature.asString()
      fieldCache.getOrPut(this to signatureString) {
        declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .find { signatureString == it.jvmFieldSignature }.toOptional()
      }.nullableValue
    } catch (e: ClassNotFoundException) {
      null
    }
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

  private fun Field.jvmModifiers(): Set<JvmFieldModifier> {
    return return mutableSetOf<JvmFieldModifier>().apply {
      if (Modifier.isTransient(modifiers)) {
        add(TRANSIENT)
      }
      if (Modifier.isVolatile(modifiers)) {
        add(VOLATILE)
      }
      if (Modifier.isStatic(modifiers)) {
        add(JvmFieldModifier.STATIC)
      }
    }
  }

  private fun Field.annotationSpecs(): List<AnnotationSpec> {
    return declaredAnnotations
        .orEmpty()
        .map { AnnotationSpec.get(it, includeDefaultValues = true) }
        .filterOutNullabilityAnnotations()
  }

  private fun Constructor<*>.annotationSpecs(): List<AnnotationSpec> {
    return declaredAnnotations.orEmpty()
        .map { AnnotationSpec.get(it, true) }
        .filterOutNullabilityAnnotations()
  }

  private fun Method.jvmModifiers(): Set<JvmMethodModifier> {
    return methodJvmModifiers(modifiers)
  }

  private fun Constructor<*>.jvmModifiers(): Set<JvmMethodModifier> {
    return methodJvmModifiers(modifiers)
  }

  private fun methodJvmModifiers(modifiers: Int): Set<JvmMethodModifier> {
    val jvmMethodModifiers = mutableSetOf<JvmMethodModifier>()
    if (Modifier.isSynchronized(modifiers)) {
      jvmMethodModifiers += SYNCHRONIZED
    }
    if (Modifier.isStatic(modifiers)) {
      jvmMethodModifiers += STATIC
    }
    return jvmMethodModifiers
  }

  private fun Method.annotationSpecs(): List<AnnotationSpec> {
    return declaredAnnotations
        .orEmpty()
        .map { AnnotationSpec.get(it, includeDefaultValues = true) }
        .filterOutNullabilityAnnotations()
  }

  private fun Parameter.annotationSpecs(): List<AnnotationSpec> {
    return declaredAnnotations
        .map { AnnotationSpec.get(it, includeDefaultValues = true) }
        .filterOutNullabilityAnnotations()
  }

  private fun Method.exceptionTypeNames(): List<TypeName> {
    return exceptionTypes.orEmpty().mapTo(mutableListOf()) { it.asTypeName() }
  }

  private fun Constructor<*>.exceptionTypeNames(): List<TypeName> {
    return exceptionTypes.orEmpty().mapTo(mutableListOf()) { it.asTypeName() }
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

  private fun Field.constantValue(): CodeBlock? {
    if (!Modifier.isStatic(modifiers)) {
      return null
    }
    return get(null) // Constant means we can do a static get on it.
        .asLiteralCodeBlock()
  }

  private fun JvmMethodSignature.isOverriddenIn(clazz: Class<*>): Boolean {
    val signatureString = asString()
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

  override fun classData(
      kmClass: ImmutableKmClass,
      parentName: String?,
      simpleName: String
  ): ClassData {
    val targetClass = lookupClass(kmClass.name.jvmInternalName)
        ?: error("No class found for: ${kmClass.name}.")

    // Should only be called if parentName has been null-checked
    val classIfCompanion by lazy(NONE) {
      if (kmClass.isCompanionObject && parentName != null) {
        lookupClass(parentName)
            ?: error("No class found for: ${parentName}.")
      } else {
        targetClass
      }
    }

    val propertyData = kmClass.properties
        .asSequence()
        .filter { it.isDeclaration }
        .filterNot { it.isSynthesized }
        .associateWith { property ->
          val isJvmField = property.computeIsJvmField(
              elementHandler = this,
              isCompanionObject = kmClass.isCompanionObject,
              hasGetter = property.getterSignature != null,
              hasSetter = property.setterSignature != null,
              hasField = property.fieldSignature != null
          )

          val fieldData = TODO()

          val getterData = TODO()

          val setterData = TODO()

          val annotations = TODO()

          PropertyData(
              annotations = annotations,
              fieldData = fieldData,
              getterData = getterData,
              setterData = setterData,
              isJvmField = isJvmField
          )
        }

    val constructorData = kmClass.constructors.associateWith { kmConstructor ->
      if (kmClass.isAnnotation || kmClass.isInline) {
        //
        // Annotations are interfaces in reflection, but kotlin metadata will still report a
        // constructor signature
        //
        // Inline classes have no constructors at runtime
        //
        return@associateWith ConstructorData.EMPTY
      }
      val signature = kmConstructor.signature
      if (signature != null) {
        val constructor = targetClass.lookupConstructor(signature)
            ?: error("No constructor $signature found in $targetClass.")
        ConstructorData(
            annotations = if (kmConstructor.hasAnnotations) {
              constructor.annotationSpecs()
            } else {
              emptyList()
            },
            parameterAnnotations = constructor.parameters.indexedAnnotationSpecs(),
            isSynthetic = constructor.isSynthetic,
            jvmModifiers = constructor.jvmModifiers(),
            exceptions = constructor.exceptionTypeNames()
        )
      } else {
        ConstructorData.EMPTY
      }
    }

    val methodData = kmClass.functions.associateWith { kmFunction ->
      val signature = kmFunction.signature
      if (signature != null) {
        val method = targetClass.lookupMethod(signature)
        method?.methodData(
            clazz = targetClass,
            signature = signature,
            hasAnnotations = kmFunction.hasAnnotations,
            jvmInformationMethod = classIfCompanion.takeIf { it != targetClass }?.lookupMethod(signature) ?: method
            )
            ?: error("No method $signature found in $targetClass.")
      } else {
        MethodData.EMPTY
      }
    }

    return ClassData(
        kmClass = kmClass,
        simpleName = simpleName,
        properties = propertyData,
        constructors = constructorData,
        methods = methodData
    )
  }

  private fun Array<Parameter>.indexedAnnotationSpecs(): Map<Int, List<AnnotationSpec>> {
    return withIndex().associate { (index, parameter) ->
          index to parameter.annotationSpecs()
        }
  }

  private fun Method.methodData(
      clazz: Class<*>,
      signature: JvmMethodSignature,
      hasAnnotations: Boolean,
      jvmInformationMethod: Method = this,
      knownIsOverride: Boolean? = null
  ): MethodData {
    return MethodData(
        annotations = if (hasAnnotations) annotationSpecs() else emptyList(),
        parameterAnnotations = parameters.indexedAnnotationSpecs(),
        isSynthetic = isSynthetic,
        jvmModifiers = jvmInformationMethod.jvmModifiers(),
        isOverride = knownIsOverride?.let { it } ?: signature.isOverriddenIn(clazz),
        exceptions = exceptionTypeNames()
    )
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

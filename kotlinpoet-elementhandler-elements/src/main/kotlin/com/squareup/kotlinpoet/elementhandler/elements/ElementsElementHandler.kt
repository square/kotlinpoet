package com.squareup.kotlinpoet.elementhandler.elements

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.common.Visibility
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
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
import java.util.concurrent.ConcurrentHashMap
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.INTERFACE
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

private typealias ElementsModifier = javax.lang.model.element.Modifier

/**
 * An [Elements]-based implementation of [ElementHandler].
 */
@KotlinPoetMetadataPreview
class ElementsElementHandler private constructor(
  private val elements: Elements,
  private val types: Types
) : ElementHandler {
  private val typeElementCache = ConcurrentHashMap<String, Optional<TypeElement>>()
  private val methodCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<ExecutableElement>>()
  private val variableElementCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<VariableElement>>()

  private fun lookupTypeElement(jvmName: String): TypeElement? {
    return typeElementCache.getOrPut(jvmName) {
      elements.getTypeElement(jvmName.canonicalName).toOptional()
    }.nullableValue
  }

  override val supportsNonRuntimeRetainedAnnotations: Boolean = true

  override fun classFor(jvmName: String): ImmutableKmClass {
    return lookupTypeElement(jvmName)?.toImmutableKmClass() ?: error(
        "No type element found for: $jvmName.")
  }

  override fun isInterface(jvmName: String): Boolean {
    if (jvmName.canonicalName in KOTLIN_INTRINSIC_INTERFACES) {
      return true
    }
    return lookupTypeElement(jvmName)?.kind == INTERFACE
  }

  private fun lookupField(
    classJvmName: String,
    fieldSignature: JvmFieldSignature
  ): VariableElement? {
    return lookupTypeElement(classJvmName)?.let {
      val signatureString = fieldSignature.asString()
      variableElementCache.getOrPut(it to signatureString) {
        ElementFilter.fieldsIn(it.enclosedElements)
            .find { signatureString == it.jvmFieldSignature(types) }.toOptional()
      }.nullableValue
    }
  }

  private fun lookupMethod(
    classJvmName: String,
    methodSignature: JvmMethodSignature,
    elementFilter: (Iterable<Element>) -> List<ExecutableElement>
  ): ExecutableElement? {
    return lookupTypeElement(classJvmName)?.lookupMethod(methodSignature, elementFilter)
  }

  private fun TypeElement.lookupMethod(
    methodSignature: JvmMethodSignature,
    elementFilter: (Iterable<Element>) -> List<ExecutableElement>
  ): ExecutableElement? {
    val signatureString = methodSignature.asString()
    return methodCache.getOrPut(this to signatureString) {
      elementFilter(enclosedElements)
          .find { signatureString == it.jvmMethodSignature(types) }.toOptional()
    }.nullableValue
  }

  override fun fieldJvmModifiers(
    classJvmName: String,
    fieldSignature: JvmFieldSignature,
    isJvmField: Boolean
  ): Set<JvmFieldModifier> {
    return lookupField(classJvmName, fieldSignature)?.modifiers?.let { modifiers ->
      modifiers.mapNotNullTo(mutableSetOf()) {
        when {
          it == ElementsModifier.TRANSIENT -> TRANSIENT
          it == ElementsModifier.VOLATILE -> VOLATILE
          !isJvmField && it == ElementsModifier.STATIC -> JvmFieldModifier.STATIC
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

  override fun isFieldSynthetic(classJvmName: String, fieldSignature: JvmFieldSignature): Boolean {
    // Elements can't see synthetic methods since they don't exist yet, so their absence here
    // makes them implicitly synthetic
    return lookupField(classJvmName, fieldSignature) == null
  }

  override fun methodJvmModifiers(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): Set<JvmMethodModifier> {
    return lookupMethod(classJvmName, methodSignature,
        ElementFilter::methodsIn)?.modifiers?.let { modifiers ->
      modifiers.mapNotNullTo(mutableSetOf()) {
        when (it) {
          ElementsModifier.SYNCHRONIZED -> SYNCHRONIZED
          ElementsModifier.STATIC -> STATIC
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

  override fun isMethodSynthetic(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): Boolean {
    // Elements can't see synthetic methods since they don't exist yet, so their absence here
    // makes them implicitly synthetic
    return lookupMethod(classJvmName, methodSignature, ElementFilter::methodsIn) == null
  }

  override fun methodExceptions(
    classJvmName: String,
    methodSignature: JvmMethodSignature,
    isConstructor: Boolean
  ): Set<TypeName> {
    val elementFilter: (Iterable<Element>) -> List<ExecutableElement> = if (isConstructor) {
      ElementFilter::constructorsIn
    } else {
      ElementFilter::methodsIn
    }
    val exceptions = lookupMethod(classJvmName, methodSignature, elementFilter)?.thrownTypes
    return exceptions.orEmpty().mapTo(mutableSetOf()) { it.asTypeName() }
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

  override fun isMethodOverride(
    classJvmName: String,
    methodSignature: JvmMethodSignature
  ): Boolean {
    if (isMethodSynthetic(classJvmName, methodSignature)) {
      return false
    }
    val typeElement = lookupTypeElement(classJvmName)
        ?: error("No type element found for: $classJvmName.")
    val method = typeElement.lookupMethod(methodSignature, ElementFilter::methodsIn)
        ?: error("No ExecutableElement found for: $methodSignature.")
    return method.isOverriddenIn(typeElement)
  }

  override fun methodExists(classJvmName: String, methodSignature: JvmMethodSignature): Boolean {
    return lookupMethod(classJvmName, methodSignature, ElementFilter::methodsIn) != null
  }

  /**
   * Detects whether [this] given method is overridden in [type].
   *
   * Adapted and simplified from AutoCommon's private
   * [MoreElements.getLocalAndInheritedMethods] methods implementations for detecting
   * overrides.
   */
  private fun ExecutableElement.isOverriddenIn(type: TypeElement): Boolean {
    val methodMap = LinkedHashMultimap.create<String, ExecutableElement>()
    type.getAllMethods(MoreElements.getPackage(type), methodMap)
    // Find methods that are overridden using `Elements.overrides`. We reduce the performance
    // impact by:
    //   (a) grouping methods by name, since a method cannot override another method with a
    //       different name. Since we know the target name, we just inspect the methods with
    //       that name.
    //   (b) making sure that methods in ancestor types precede those in descendant types,
    //       which means we only have to check a method against the ones that follow it in
    //       that order. Below, this means we just need to find the index of our target method
    //       and compare against only preceding ones.
    val methodList = methodMap.asMap()[simpleName.toString()]?.toList()
        ?: return false
    val indexOfPossibleOverrider = methodList.indexOf(this)
    return (indexOfPossibleOverrider downTo 0)
        .asSequence()
        .map { methodList[it] }
        .any { elements.overrides(this, it, type) }
  }

  /**
   * Add to [methodsAccumulator] the instance methods from [this] that are visible to code in
   * the package [pkg]. This means all the instance methods from [this] itself and all
   * instance methods it inherits from its ancestors, except private methods and
   * package-private methods in other packages. This method does not take overriding into
   * account, so it will add both an ancestor method and a descendant method that overrides
   * it. [methodsAccumulator] is a multimap from a method name to all of the methods with
   * that name, including methods that override or overload one another. Within those
   * methods, those in ancestor types always precede those in descendant types.
   *
   * Adapted from AutoCommon's private [MoreElements.getLocalAndInheritedMethods] methods'
   * implementations, before overridden methods are stripped.
   */
  private fun TypeElement.getAllMethods(
    pkg: PackageElement,
    methodsAccumulator: SetMultimap<String, ExecutableElement>
  ) {
    for (superInterface in interfaces) {
      MoreTypes.asTypeElement(superInterface).getAllMethods(pkg, methodsAccumulator)
    }
    if (superclass.kind != TypeKind.NONE) {
      // Visit the superclass after superinterfaces so we will always see the implementation of a
      // method after any interfaces that declared it.
      MoreTypes.asTypeElement(superclass).getAllMethods(pkg, methodsAccumulator)
    }
    for (method in ElementFilter.methodsIn(enclosedElements)) {
      if (ElementsModifier.STATIC !in method.modifiers &&
          ElementsModifier.FINAL !in method.modifiers &&
          ElementsModifier.PRIVATE !in method.modifiers &&
          method.isVisibleFrom(pkg)) {
        methodsAccumulator.put(method.simpleName.toString(), method)
      }
    }
  }

  private fun ExecutableElement.isVisibleFrom(pkg: PackageElement): Boolean {
    // We use Visibility.ofElement rather than [MoreElements.effectiveVisibilityOfElement]
    // because it doesn't really matter whether the containing class is visible. If you
    // inherit a public method then you have a public method, regardless of whether you
    // inherit it from a public class.
    return when (Visibility.ofElement(this)) {
      Visibility.PRIVATE -> false
      Visibility.DEFAULT -> MoreElements.getPackage(this) == pkg
      else -> true
    }
  }

  companion object {
    /** @return an [Elements]-based implementation of [ElementHandler]. */
    @JvmStatic
    @KotlinPoetMetadataPreview
    fun create(elements: Elements, types: Types): ElementHandler {
      return ElementsElementHandler(elements, types)
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
  }
}

/**
 * Simple `Optional` implementation for use in collections that don't allow `null` values.
 *
 * TODO: Make this an inline class when inline classes are stable.
 */
private data class Optional<out T : Any>(val nullableValue: T?)
private fun <T : Any> T?.toOptional(): Optional<T> = Optional(this)

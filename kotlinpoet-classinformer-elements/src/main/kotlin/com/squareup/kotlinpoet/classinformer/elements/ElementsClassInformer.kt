package com.squareup.kotlinpoet.classinformer.elements

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.common.Visibility
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.hasAnnotations
import com.squareup.kotlinpoet.metadata.hasConstant
import com.squareup.kotlinpoet.metadata.isAnnotation
import com.squareup.kotlinpoet.metadata.isCompanionObject
import com.squareup.kotlinpoet.metadata.isConst
import com.squareup.kotlinpoet.metadata.isDeclaration
import com.squareup.kotlinpoet.metadata.isInline
import com.squareup.kotlinpoet.metadata.isSynthesized
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.ConstructorData
import com.squareup.kotlinpoet.metadata.specs.ClassInformer
import com.squareup.kotlinpoet.metadata.specs.FieldData
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.specs.MethodData
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.specs.internal.ElementHandlerUtil
import com.squareup.kotlinpoet.metadata.specs.internal.ElementHandlerUtil.filterOutNullabilityAnnotations
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
import kotlin.LazyThreadSafetyMode.NONE

private typealias ElementsModifier = javax.lang.model.element.Modifier

/**
 * An [Elements]-based implementation of [ClassInformer].
 */
@KotlinPoetMetadataPreview
class ElementsClassInformer private constructor(
  private val elements: Elements,
  private val types: Types
) : ClassInformer {
  private val typeElementCache = ConcurrentHashMap<ClassName, Optional<TypeElement>>()
  private val methodCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<ExecutableElement>>()
  private val variableElementCache = ConcurrentHashMap<Pair<TypeElement, String>, Optional<VariableElement>>()

  private fun lookupTypeElement(className: ClassName): TypeElement? {
    return typeElementCache.getOrPut(className) {
      elements.getTypeElement(className.canonicalName).toOptional()
    }.nullableValue
  }

  override val supportsNonRuntimeRetainedAnnotations: Boolean = true

  override fun classFor(className: ClassName): ImmutableKmClass {
    return lookupTypeElement(className)?.toImmutableKmClass() ?: error(
        "No type element found for: $className.")
  }

  override fun isInterface(className: ClassName): Boolean {
    if (className in ElementHandlerUtil.KOTLIN_INTRINSIC_INTERFACES) {
      return true
    }
    return lookupTypeElement(className)?.kind == INTERFACE
  }

  private fun TypeElement.lookupField(fieldSignature: JvmFieldSignature): VariableElement? {
    val signatureString = fieldSignature.asString()
    return variableElementCache.getOrPut(this to signatureString) {
      ElementFilter.fieldsIn(enclosedElements)
          .find { signatureString == it.jvmFieldSignature(types) }.toOptional()
    }.nullableValue
  }

  private fun lookupMethod(
    className: ClassName,
    methodSignature: JvmMethodSignature,
    elementFilter: (Iterable<Element>) -> List<ExecutableElement>
  ): ExecutableElement? {
    return lookupTypeElement(className)?.lookupMethod(methodSignature, elementFilter)
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

  private fun VariableElement.jvmModifiers(isJvmField: Boolean): Set<JvmFieldModifier> {
    return modifiers.mapNotNullTo(mutableSetOf()) {
      when {
        it == ElementsModifier.TRANSIENT -> TRANSIENT
        it == ElementsModifier.VOLATILE -> VOLATILE
        !isJvmField && it == ElementsModifier.STATIC -> JvmFieldModifier.STATIC
        else -> null
      }
    }
  }

  private fun VariableElement.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
        annotationMirrors.map { AnnotationSpec.get(it) }
    )
  }

  private fun ExecutableElement.jvmModifiers(): Set<JvmMethodModifier> {
    return modifiers.mapNotNullTo(mutableSetOf()) {
      when (it) {
        ElementsModifier.SYNCHRONIZED -> SYNCHRONIZED
        ElementsModifier.STATIC -> STATIC
        else -> null
      }
    }
  }

  private fun ExecutableElement.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
        annotationMirrors.map { AnnotationSpec.get(it) }
    )
  }

  private fun ExecutableElement.exceptionTypeNames(): List<TypeName> {
    return thrownTypes.map { it.asTypeName() }
  }

  override fun enumEntry(enumClassName: ClassName, memberName: String): ImmutableKmClass? {
    return lookupTypeElement(enumClassName)?.let { enumType ->
      val enumTypeAsType = enumType.asType()
      val member = typeElementCache.getOrPut(enumClassName.nestedClass(memberName)) {
        ElementFilter.typesIn(enumType.enclosedElements)
            .asSequence()
            .filter { types.isSubtype(enumTypeAsType, it.superclass) }
            .find { it.simpleName.contentEquals(memberName) }.toOptional()
      }.nullableValue
      member?.getAnnotation(Metadata::class.java)
          ?.toImmutableKmClass()
    }
  }

  private fun VariableElement.constantValue(): CodeBlock? {
    return constantValue?.let(ElementHandlerUtil::codeLiteralOf)
  }

  override fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean {
    return lookupMethod(className, methodSignature, ElementFilter::methodsIn) != null
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

  override fun classData(
    kmClass: ImmutableKmClass,
    className: ClassName,
    parentClassName: ClassName?
  ): ClassData {
    val typeElement = lookupTypeElement(className)
        ?: error("No class found for: ${kmClass.name}.")

    // Should only be called if parentName has been null-checked
    val classIfCompanion by lazy(NONE) {
      if (kmClass.isCompanionObject && parentClassName != null) {
        lookupTypeElement(parentClassName)
            ?: error("No class found for: $parentClassName.")
      } else {
        typeElement
      }
    }

    val classAnnotations = if (kmClass.hasAnnotations) {
      ElementHandlerUtil.createAnnotations {
        addAll(typeElement.annotationMirrors.map { AnnotationSpec.get(it) })
      }
    } else {
      emptyList()
    }

    val propertyData = kmClass.properties
        .asSequence()
        .filter { it.isDeclaration }
        .filterNot { it.isSynthesized }
        .associateWith { property ->
          val isJvmField = ElementHandlerUtil.computeIsJvmField(
              property = property,
              classInformer = this,
              isCompanionObject = kmClass.isCompanionObject,
              hasGetter = property.getterSignature != null,
              hasSetter = property.setterSignature != null,
              hasField = property.fieldSignature != null
          )

          val fieldData = property.fieldSignature?.let fieldDataLet@{ fieldSignature ->
            // Check the field in the parent first. For const/static/jvmField elements, these only
            // exist in the parent and we want to check that if necessary to avoid looking up a
            // non-existent field in the companion.
            val parentModifiers = if (kmClass.isCompanionObject && parentClassName != null) {
              classIfCompanion.lookupField(fieldSignature)?.jvmModifiers(isJvmField).orEmpty()
            } else {
              emptySet()
            }

            val isStatic = JvmFieldModifier.STATIC in parentModifiers

            // TODO we looked up field once, let's reuse it
            val classForOriginalField = typeElement.takeUnless {
              kmClass.isCompanionObject &&
                  (property.isConst || isJvmField || isStatic)
            } ?: classIfCompanion

            val field = classForOriginalField.lookupField(fieldSignature)
                ?: return@fieldDataLet FieldData.SYNTHETIC
            val constant = if (property.hasConstant) {
              val fieldWithConstant = classIfCompanion.takeIf { it != typeElement }?.let {
                if (it.kind.isInterface) {
                  field
                } else {
                  // const properties are relocated to the enclosing class
                  it.lookupField(fieldSignature)
                      ?: return@fieldDataLet FieldData.SYNTHETIC
                }
              } ?: field
              fieldWithConstant.constantValue()
            } else {
              null
            }

            val jvmModifiers = field.jvmModifiers(isJvmField) + parentModifiers

            FieldData(
                annotations = field.annotationSpecs(),
                isSynthetic = false,
                jvmModifiers = jvmModifiers.filterNotTo(mutableSetOf()) {
                  // JvmField companion objects don't need JvmStatic, it's implicit
                  kmClass.isCompanionObject && isJvmField && it == JvmFieldModifier.STATIC
                },
                constant = constant
            )
          }

          val getterData = property.getterSignature?.let { getterSignature ->
            val method = classIfCompanion.lookupMethod(getterSignature, ElementFilter::methodsIn)
            method?.methodData(
                typeElement = typeElement,
                hasAnnotations = property.getterFlags.hasAnnotations,
                jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                    ?.lookupMethod(getterSignature, ElementFilter::methodsIn)
                    ?: method
            )
                ?: return@let MethodData.SYNTHETIC
          }

          val setterData = property.setterSignature?.let { setterSignature ->
            val method = classIfCompanion.lookupMethod(setterSignature, ElementFilter::methodsIn)
            method?.methodData(
                typeElement = typeElement,
                hasAnnotations = property.setterFlags.hasAnnotations,
                jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                    ?.lookupMethod(setterSignature, ElementFilter::methodsIn)
                    ?: method,
                knownIsOverride = getterData?.isOverride
            )
                ?: return@let MethodData.SYNTHETIC
          }

          val annotations = mutableListOf<AnnotationSpec>()
          if (property.hasAnnotations) {
            property.syntheticMethodForAnnotations?.let { annotationsHolderSignature ->
              val method = typeElement.lookupMethod(annotationsHolderSignature, ElementFilter::methodsIn)
                  ?: return@let MethodData.SYNTHETIC
              annotations += method.annotationSpecs()
            }
          }

          // If a field is static in a companion object, remove the modifier and add the annotation
          // directly on the top level. Otherwise this will generate `@field:JvmStatic`, which is
          // not legal
          var finalFieldData = fieldData
          fieldData?.jvmModifiers?.let {
            if (kmClass.isCompanionObject && JvmFieldModifier.STATIC in it) {
              finalFieldData = fieldData.copy(jvmModifiers = fieldData.jvmModifiers
                  .filterNotTo(LinkedHashSet()) { it == JvmFieldModifier.STATIC })
              annotations += AnnotationSpec.builder(JVM_STATIC).build()
            }
          }

          PropertyData(
              annotations = annotations,
              fieldData = finalFieldData,
              getterData = getterData,
              setterData = setterData,
              isJvmField = isJvmField
          )
        }

    val constructorData = kmClass.constructors.associateWith { kmConstructor ->
      if (kmClass.isAnnotation || kmClass.isInline) {
        //
        // Annotations are interfaces in bytecode, but kotlin metadata will still report a
        // constructor signature
        //
        // Inline classes have no constructors at runtime
        //
        return@associateWith ConstructorData.EMPTY
      }
      val signature = kmConstructor.signature
      if (signature != null) {
        val constructor = typeElement.lookupMethod(signature, ElementFilter::constructorsIn)
            ?: return@associateWith ConstructorData.EMPTY
        ConstructorData(
            annotations = if (kmConstructor.hasAnnotations) {
              constructor.annotationSpecs()
            } else {
              emptyList()
            },
            parameterAnnotations = constructor.parameters.indexedAnnotationSpecs(),
            isSynthetic = false,
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
        val method = typeElement.lookupMethod(signature, ElementFilter::methodsIn)
        method?.methodData(
            typeElement = typeElement,
            hasAnnotations = kmFunction.hasAnnotations,
            jvmInformationMethod = classIfCompanion.takeIf { it != typeElement }
                ?.lookupMethod(signature, ElementFilter::methodsIn)
                ?: method
        )
            ?: return@associateWith MethodData.SYNTHETIC
      } else {
        MethodData.EMPTY
      }
    }

    return ClassData(
        kmClass = kmClass,
        className = className,
        annotations = classAnnotations,
        properties = propertyData,
        constructors = constructorData,
        methods = methodData
    )
  }

  private fun List<VariableElement>.indexedAnnotationSpecs(): Map<Int, Collection<AnnotationSpec>> {
    return withIndex().associate { (index, parameter) ->
      index to ElementHandlerUtil.createAnnotations { addAll(parameter.annotationSpecs()) }
    }
  }

  private fun ExecutableElement.methodData(
    typeElement: TypeElement,
    hasAnnotations: Boolean,
    jvmInformationMethod: ExecutableElement = this,
    knownIsOverride: Boolean? = null
  ): MethodData {
    return MethodData(
        annotations = if (hasAnnotations) annotationSpecs() else emptyList(),
        parameterAnnotations = parameters.indexedAnnotationSpecs(),
        isSynthetic = false,
        jvmModifiers = jvmInformationMethod.jvmModifiers(),
        isOverride = knownIsOverride?.let { it } ?: isOverriddenIn(typeElement),
        exceptions = exceptionTypeNames()
    )
  }

  companion object {
    /** @return an [Elements]-based implementation of [ClassInformer]. */
    @JvmStatic
    @KotlinPoetMetadataPreview
    fun create(elements: Elements, types: Types): ClassInformer {
      return ElementsClassInformer(elements, types)
    }

    private val JVM_STATIC = JvmStatic::class.asClassName()
  }
}

/**
 * Simple `Optional` implementation for use in collections that don't allow `null` values.
 *
 * TODO: Make this an inline class when inline classes are stable.
 */
private data class Optional<out T : Any>(val nullableValue: T?)
private fun <T : Any> T?.toOptional(): Optional<T> = Optional(this)

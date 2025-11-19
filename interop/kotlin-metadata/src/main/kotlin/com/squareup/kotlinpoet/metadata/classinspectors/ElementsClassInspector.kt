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
package com.squareup.kotlinpoet.metadata.classinspectors

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.auto.common.Visibility
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.JAVA_DEPRECATED
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.JVM_NAME
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.filterOutNullabilityAnnotations
import com.squareup.kotlinpoet.metadata.isDeclaration
import com.squareup.kotlinpoet.metadata.readKotlinClassMetadata
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.ConstructorData
import com.squareup.kotlinpoet.metadata.specs.ContainerData
import com.squareup.kotlinpoet.metadata.specs.EnumEntryData
import com.squareup.kotlinpoet.metadata.specs.FieldData
import com.squareup.kotlinpoet.metadata.specs.FileData
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.DEFAULT
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.specs.KM_CONSTRUCTOR_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.KM_FUNCTION_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.KM_PROPERTY_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.MethodData
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.toKmClass
import java.util.TreeMap
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
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmPackage
import kotlin.metadata.hasAnnotations
import kotlin.metadata.hasConstant
import kotlin.metadata.isConst
import kotlin.metadata.isValue
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.hasAnnotationsInBytecode
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.kind

private typealias ElementsModifier = javax.lang.model.element.Modifier

/** An [Elements]-based implementation of [ClassInspector]. */
public class ElementsClassInspector
private constructor(
  private val lenient: Boolean,
  private val elements: Elements,
  private val types: Types,
) : ClassInspector {
  private val typeElementCache = ConcurrentHashMap<ClassName, Optional<TypeElement>>()
  private val methodCache =
    ConcurrentHashMap<Pair<TypeElement, String>, Optional<ExecutableElement>>()
  private val variableElementCache =
    ConcurrentHashMap<Pair<TypeElement, String>, Optional<VariableElement>>()
  private val jvmNameType = elements.getTypeElement(JVM_NAME.canonicalName)
  private val jvmNameName =
    ElementFilter.methodsIn(jvmNameType.enclosedElements).first {
      it.simpleName.toString() == "name"
    }

  private fun lookupTypeElement(className: ClassName): TypeElement? {
    return typeElementCache
      .getOrPut(className) { elements.getTypeElement(className.canonicalName).toOptional() }
      .nullableValue
  }

  override val supportsNonRuntimeRetainedAnnotations: Boolean = true

  override fun declarationContainerFor(className: ClassName): KmDeclarationContainer {
    val typeElement =
      lookupTypeElement(className) ?: error("No type element found for: $className.")

    val metadata = typeElement.getAnnotation(Metadata::class.java)
    return when (val kotlinClassMetadata = metadata.readKotlinClassMetadata(lenient)) {
      is KotlinClassMetadata.Class -> kotlinClassMetadata.kmClass
      is KotlinClassMetadata.FileFacade -> kotlinClassMetadata.kmPackage
      else -> TODO("Not implemented yet: ${kotlinClassMetadata.javaClass.simpleName}")
    }
  }

  override fun isInterface(className: ClassName): Boolean {
    if (className in ClassInspectorUtil.KOTLIN_INTRINSIC_INTERFACES) {
      return true
    }
    return lookupTypeElement(className)?.kind == INTERFACE
  }

  private fun TypeElement.lookupField(fieldSignature: JvmFieldSignature): VariableElement? {
    val signatureString = fieldSignature.toString()
    return variableElementCache
      .getOrPut(this to signatureString) {
        ElementFilter.fieldsIn(enclosedElements)
          .find { signatureString == it.jvmFieldSignature(types) }
          .toOptional()
      }
      .nullableValue
  }

  private fun lookupMethod(
    className: ClassName,
    methodSignature: JvmMethodSignature,
    elementFilter: (Iterable<Element>) -> List<ExecutableElement>,
  ): ExecutableElement? {
    return lookupTypeElement(className)?.lookupMethod(methodSignature, elementFilter)
  }

  private fun TypeElement.lookupMethod(
    methodSignature: JvmMethodSignature,
    elementFilter: (Iterable<Element>) -> List<ExecutableElement>,
  ): ExecutableElement? {
    val signatureString = methodSignature.toString()
    return methodCache
      .getOrPut(this to signatureString) {
        elementFilter(enclosedElements)
          .find { signatureString == it.jvmMethodSignature(types) }
          .toOptional()
      }
      .nullableValue
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

  @OptIn(DelicateKotlinPoetApi::class)
  private fun VariableElement.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(annotationMirrors.map { AnnotationSpec.get(it) })
  }

  private fun ExecutableElement.jvmModifiers(): Set<JvmMethodModifier> {
    return modifiers.mapNotNullTo(mutableSetOf()) {
      when (it) {
        ElementsModifier.SYNCHRONIZED -> SYNCHRONIZED
        ElementsModifier.STATIC -> STATIC
        ElementsModifier.DEFAULT -> DEFAULT
        else -> null
      }
    }
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun ExecutableElement.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(annotationMirrors.map { AnnotationSpec.get(it) })
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun ExecutableElement.exceptionTypeNames(): List<TypeName> {
    return thrownTypes.map { it.asTypeName() }
  }

  override fun enumEntry(enumClassName: ClassName, memberName: String): EnumEntryData {
    val enumType =
      lookupTypeElement(enumClassName) ?: error("No type element found for: $enumClassName.")
    val enumTypeAsType = enumType.asType()
    val member =
      typeElementCache
        .getOrPut(enumClassName.nestedClass(memberName)) {
          ElementFilter.typesIn(enumType.enclosedElements)
            .asSequence()
            .filter { types.isSubtype(enumTypeAsType, it.superclass) }
            .find { it.simpleName.contentEquals(memberName) }
            .toOptional()
        }
        .nullableValue
    val declarationContainer = member?.getAnnotation(Metadata::class.java)?.toKmClass(lenient)

    val entry =
      ElementFilter.fieldsIn(enumType.enclosedElements).find {
        it.simpleName.contentEquals(memberName)
      } ?: error("Could not find the enum entry for: $enumClassName")

    return EnumEntryData(
      declarationContainer = declarationContainer,
      annotations = entry.annotationSpecs(),
    )
  }

  private fun VariableElement.constantValue(): CodeBlock? {
    return constantValue?.let(ClassInspectorUtil::codeLiteralOf)
  }

  override fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean {
    return lookupMethod(className, methodSignature, ElementFilter::methodsIn) != null
  }

  /**
   * Detects whether [this] given method is overridden in [type].
   *
   * Adapted and simplified from AutoCommon's private [MoreElements.getLocalAndInheritedMethods]
   * methods implementations for detecting overrides.
   */
  private fun ExecutableElement.isOverriddenIn(type: TypeElement): Boolean {
    val methodMap = mutableMapOf<String, MutableList<ExecutableElement>>()
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
    val methodList = methodMap[simpleName.toString()] ?: return false
    val signature = jvmMethodSignature(types)
    return methodList
      .asSequence()
      .filter { it.jvmMethodSignature(types) == signature }
      .take(1)
      .any { elements.overrides(this, it, type) }
  }

  /**
   * Add to [methodsAccumulator] the instance methods from [this] that are visible to code in the
   * package [pkg]. This means all the instance methods from [this] itself and all instance methods
   * it inherits from its ancestors, except private methods and package-private methods in other
   * packages. This method does not take overriding into account, so it will add both an ancestor
   * method and a descendant method that overrides it. [methodsAccumulator] is a multimap from a
   * method name to all of the methods with that name, including methods that override or overload
   * one another. Within those methods, those in ancestor types always precede those in descendant
   * types.
   *
   * Adapted from AutoCommon's private [MoreElements.getLocalAndInheritedMethods] methods'
   * implementations, before overridden methods are stripped.
   */
  private fun TypeElement.getAllMethods(
    pkg: PackageElement,
    methodsAccumulator: MutableMap<String, MutableList<ExecutableElement>>,
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
      if (
        ElementsModifier.STATIC !in method.modifiers &&
          ElementsModifier.FINAL !in method.modifiers &&
          ElementsModifier.PRIVATE !in method.modifiers &&
          method.isVisibleFrom(pkg)
      ) {
        methodsAccumulator.getOrPut(method.simpleName.toString(), ::ArrayList).add(method)
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

  @OptIn(DelicateKotlinPoetApi::class)
  override fun containerData(
    declarationContainer: KmDeclarationContainer,
    className: ClassName,
    parentClassName: ClassName?,
  ): ContainerData {
    val typeElement: TypeElement =
      lookupTypeElement(className) ?: error("No class found for: $className.")
    val isCompanionObject =
      when (declarationContainer) {
        is KmClass -> {
          declarationContainer.kind == ClassKind.COMPANION_OBJECT
        }
        is KmPackage -> {
          false
        }
        else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
      }

    // Should only be called if parentName has been null-checked
    val classIfCompanion by
      lazy(NONE) {
        if (isCompanionObject && parentClassName != null) {
          lookupTypeElement(parentClassName) ?: error("No class found for: $parentClassName.")
        } else {
          typeElement
        }
      }

    val propertyData =
      declarationContainer.properties
        .asSequence()
        .filter { it.kind.isDeclaration }
        .associateWithTo(TreeMap(KM_PROPERTY_COMPARATOR)) { property ->
          val isJvmField =
            ClassInspectorUtil.computeIsJvmField(
              property = property,
              classInspector = this,
              isCompanionObject = isCompanionObject,
              hasGetter = property.getterSignature != null,
              hasSetter = property.setterSignature != null,
              hasField = property.fieldSignature != null,
            )

          val fieldData =
            property.fieldSignature?.let fieldDataLet@{ fieldSignature ->
              // Check the field in the parent first. For const/static/jvmField elements, these only
              // exist in the parent and we want to check that if necessary to avoid looking up a
              // non-existent field in the companion.
              val parentModifiers =
                if (isCompanionObject && parentClassName != null) {
                  classIfCompanion.lookupField(fieldSignature)?.jvmModifiers(isJvmField).orEmpty()
                } else {
                  emptySet()
                }

              val isStatic = JvmFieldModifier.STATIC in parentModifiers

              // TODO we looked up field once, let's reuse it
              val classForOriginalField =
                typeElement.takeUnless {
                  isCompanionObject && (property.isConst || isJvmField || isStatic)
                } ?: classIfCompanion

              val field =
                classForOriginalField.lookupField(fieldSignature)
                  ?: return@fieldDataLet FieldData.SYNTHETIC
              val constant =
                if (property.hasConstant) {
                  val fieldWithConstant =
                    classIfCompanion
                      .takeIf { it != typeElement }
                      ?.let {
                        if (it.kind.isInterface) {
                          field
                        } else {
                          // const properties are relocated to the enclosing class
                          it.lookupField(fieldSignature) ?: return@fieldDataLet FieldData.SYNTHETIC
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
                jvmModifiers =
                  jvmModifiers.filterNotTo(mutableSetOf()) {
                    // JvmField companion objects don't need JvmStatic, it's implicit
                    isCompanionObject && isJvmField && it == JvmFieldModifier.STATIC
                  },
                constant = constant,
              )
            }

          val getterData =
            property.getterSignature?.let { getterSignature ->
              val method = classIfCompanion.lookupMethod(getterSignature, ElementFilter::methodsIn)
              method?.methodData(
                typeElement = typeElement,
                hasAnnotations = property.getter.hasAnnotationsInBytecode,
                jvmInformationMethod =
                  classIfCompanion
                    .takeIf { it != typeElement }
                    ?.lookupMethod(getterSignature, ElementFilter::methodsIn) ?: method,
              ) ?: return@let MethodData.SYNTHETIC
            }

          val setterData =
            property.setterSignature?.let { setterSignature ->
              val method = classIfCompanion.lookupMethod(setterSignature, ElementFilter::methodsIn)
              method?.methodData(
                typeElement = typeElement,
                hasAnnotations = property.setter?.hasAnnotationsInBytecode ?: false,
                jvmInformationMethod =
                  classIfCompanion
                    .takeIf { it != typeElement }
                    ?.lookupMethod(setterSignature, ElementFilter::methodsIn) ?: method,
                knownIsOverride = getterData?.isOverride,
              ) ?: return@let MethodData.SYNTHETIC
            }

          val annotations = mutableListOf<AnnotationSpec>()
          if (property.hasAnnotationsInBytecode) {
            property.syntheticMethodForAnnotations?.let { annotationsHolderSignature ->
              val method =
                typeElement.lookupMethod(annotationsHolderSignature, ElementFilter::methodsIn)
                  ?: return@let MethodData.SYNTHETIC
              annotations +=
                method
                  .annotationSpecs()
                  // Cover for https://github.com/square/kotlinpoet/issues/1046
                  .filterNot { it.typeName == JAVA_DEPRECATED }
            }
          }

          // If a field is static in a companion object, remove the modifier and add the annotation
          // directly on the top level. Otherwise this will generate `@field:JvmStatic`, which is
          // not legal
          var finalFieldData = fieldData
          fieldData?.jvmModifiers?.let { modifiers ->
            if (isCompanionObject && JvmFieldModifier.STATIC in modifiers) {
              finalFieldData =
                fieldData.copy(
                  jvmModifiers =
                    fieldData.jvmModifiers.filterNotTo(LinkedHashSet()) {
                      it == JvmFieldModifier.STATIC
                    }
                )
              annotations += AnnotationSpec.builder(JVM_STATIC).build()
            }
          }

          PropertyData(
            annotations = annotations,
            fieldData = finalFieldData,
            getterData = getterData,
            setterData = setterData,
            isJvmField = isJvmField,
          )
        }

    val methodData =
      declarationContainer.functions.associateWithTo(TreeMap(KM_FUNCTION_COMPARATOR)) { kmFunction
        ->
        val signature = kmFunction.signature
        if (signature != null) {
          val method = typeElement.lookupMethod(signature, ElementFilter::methodsIn)
          method?.methodData(
            typeElement = typeElement,
            hasAnnotations = kmFunction.hasAnnotationsInBytecode,
            jvmInformationMethod =
              classIfCompanion
                .takeIf { it != typeElement }
                ?.lookupMethod(signature, ElementFilter::methodsIn) ?: method,
          ) ?: return@associateWithTo MethodData.SYNTHETIC
        } else {
          MethodData.EMPTY
        }
      }

    when (declarationContainer) {
      is KmClass -> {
        val constructorData =
          declarationContainer.constructors.associateWithTo(TreeMap(KM_CONSTRUCTOR_COMPARATOR)) {
            kmConstructor ->
            if (
              declarationContainer.kind == ClassKind.ANNOTATION_CLASS ||
                declarationContainer.isValue
            ) {
              //
              // Annotations are interfaces in bytecode, but kotlin metadata will still report a
              // constructor signature
              //
              // Inline classes have no constructors at runtime
              //
              return@associateWithTo ConstructorData.EMPTY
            }
            val signature = kmConstructor.signature
            if (signature != null) {
              val constructor =
                typeElement.lookupMethod(signature, ElementFilter::constructorsIn)
                  ?: return@associateWithTo ConstructorData.EMPTY
              ConstructorData(
                annotations =
                  if (kmConstructor.hasAnnotationsInBytecode) {
                    constructor.annotationSpecs()
                  } else {
                    emptyList()
                  },
                parameterAnnotations = constructor.parameters.indexedAnnotationSpecs(),
                isSynthetic = false,
                jvmModifiers = constructor.jvmModifiers(),
                exceptions = constructor.exceptionTypeNames(),
              )
            } else {
              ConstructorData.EMPTY
            }
          }
        return ClassData(
          declarationContainer = declarationContainer,
          className = className,
          annotations =
            if (declarationContainer.hasAnnotationsInBytecode) {
              ClassInspectorUtil.createAnnotations {
                addAll(typeElement.annotationMirrors.map { AnnotationSpec.get(it) })
              }
            } else {
              emptyList()
            },
          properties = propertyData,
          constructors = constructorData,
          methods = methodData,
        )
      }
      is KmPackage -> {
        // There's no flag for checking if there are annotations, so we just eagerly check in this
        // case. All annotations on this class are file: site targets in source. This includes
        // @JvmName.
        var jvmName: String? = null
        val fileAnnotations =
          ClassInspectorUtil.createAnnotations(FILE) {
            addAll(
              typeElement.annotationMirrors.map {
                if (it.annotationType == jvmNameType) {
                  val nameValue =
                    requireNotNull(it.elementValues[jvmNameName]) {
                      "No name property found on $it"
                    }
                  jvmName = nameValue.value as String
                }
                AnnotationSpec.get(it)
              }
            )
          }
        return FileData(
          declarationContainer = declarationContainer,
          annotations = fileAnnotations,
          properties = propertyData,
          methods = methodData,
          className = className,
          jvmName = jvmName,
        )
      }
      else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
    }
  }

  private fun List<VariableElement>.indexedAnnotationSpecs(): Map<Int, Collection<AnnotationSpec>> {
    return withIndex().associate { (index, parameter) ->
      index to ClassInspectorUtil.createAnnotations { addAll(parameter.annotationSpecs()) }
    }
  }

  private fun ExecutableElement.methodData(
    typeElement: TypeElement,
    hasAnnotations: Boolean,
    jvmInformationMethod: ExecutableElement = this,
    knownIsOverride: Boolean? = null,
  ): MethodData {
    return MethodData(
      annotations = if (hasAnnotations) annotationSpecs() else emptyList(),
      parameterAnnotations = parameters.indexedAnnotationSpecs(),
      isSynthetic = false,
      jvmModifiers = jvmInformationMethod.jvmModifiers(),
      isOverride = knownIsOverride ?: isOverriddenIn(typeElement),
      exceptions = exceptionTypeNames(),
    )
  }

  public companion object {
    /**
     * @param lenient see docs on [KotlinClassMetadata.readStrict] and
     *   [KotlinClassMetadata.readLenient] for more details.
     * @return an [Elements]-based implementation of [ClassInspector].
     */
    @JvmStatic
    public fun create(lenient: Boolean, elements: Elements, types: Types): ClassInspector {
      return ElementsClassInspector(lenient, elements, types)
    }

    private val JVM_STATIC = JvmStatic::class.asClassName()
  }
}

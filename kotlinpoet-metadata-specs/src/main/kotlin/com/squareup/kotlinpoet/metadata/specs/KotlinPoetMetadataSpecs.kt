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
@file:JvmName("KotlinPoetMetadataSpecs")

package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.FunSpec.Builder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.CONST
import com.squareup.kotlinpoet.KModifier.CROSSINLINE
import com.squareup.kotlinpoet.KModifier.DATA
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.EXTERNAL
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.INFIX
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.INNER
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.LATEINIT
import com.squareup.kotlinpoet.KModifier.NOINLINE
import com.squareup.kotlinpoet.KModifier.OPEN
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PROTECTED
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.KModifier.SEALED
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.KModifier.TAILREC
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeParameter
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.ImmutableKmWithFlags
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_EXTERNAL
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_INLINE
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_NOT_DEFAULT
import com.squareup.kotlinpoet.metadata.declaresDefaultValue
import com.squareup.kotlinpoet.metadata.hasGetter
import com.squareup.kotlinpoet.metadata.hasSetter
import com.squareup.kotlinpoet.metadata.isAbstract
import com.squareup.kotlinpoet.metadata.isAnnotation
import com.squareup.kotlinpoet.metadata.isClass
import com.squareup.kotlinpoet.metadata.isCompanionObject
import com.squareup.kotlinpoet.metadata.isConst
import com.squareup.kotlinpoet.metadata.isCrossInline
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.isDeclaration
import com.squareup.kotlinpoet.metadata.isDelegated
import com.squareup.kotlinpoet.metadata.isDelegation
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.isEnumEntry
import com.squareup.kotlinpoet.metadata.isExpect
import com.squareup.kotlinpoet.metadata.isExternal
import com.squareup.kotlinpoet.metadata.isFinal
import com.squareup.kotlinpoet.metadata.isInfix
import com.squareup.kotlinpoet.metadata.isInline
import com.squareup.kotlinpoet.metadata.isInner
import com.squareup.kotlinpoet.metadata.isInterface
import com.squareup.kotlinpoet.metadata.isInternal
import com.squareup.kotlinpoet.metadata.isLateinit
import com.squareup.kotlinpoet.metadata.isNoInline
import com.squareup.kotlinpoet.metadata.isObject
import com.squareup.kotlinpoet.metadata.isOpen
import com.squareup.kotlinpoet.metadata.isOperator
import com.squareup.kotlinpoet.metadata.isPrimary
import com.squareup.kotlinpoet.metadata.isPrivate
import com.squareup.kotlinpoet.metadata.isProtected
import com.squareup.kotlinpoet.metadata.isPublic
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.kotlinpoet.metadata.isSuspend
import com.squareup.kotlinpoet.metadata.isSynthesized
import com.squareup.kotlinpoet.metadata.isTailRec
import com.squareup.kotlinpoet.metadata.isVal
import com.squareup.kotlinpoet.metadata.isVar
import com.squareup.kotlinpoet.metadata.propertyAccessorFlags
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.bestGuessClassName
import com.squareup.kotlinpoet.metadata.specs.internal.primaryConstructor
import com.squareup.kotlinpoet.metadata.specs.internal.toTypeName
import com.squareup.kotlinpoet.metadata.specs.internal.toTypeVariableName
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.jvmInternalName
import java.util.Locale
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun KClass<*>.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = java.toTypeSpec(classInspector)

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun Class<*>.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(classInspector, asClassName())

/** @return a [TypeSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
fun TypeElement.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(classInspector, asClassName())

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun KClass<*>.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = java.toFileSpec(classInspector)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun Class<*>.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = FileSpec.get(`package`.name, toTypeSpec(classInspector))

/** @return a [FileSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
fun TypeElement.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = FileSpec.get(
    packageName = packageName,
    typeSpec = toTypeSpec(classInspector)
)

/** @return a [TypeSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
fun ImmutableKmClass.toTypeSpec(
  classInspector: ClassInspector?,
  className: ClassName = bestGuessClassName(name)
): TypeSpec {
  return toTypeSpec(classInspector, className, null)
}

/** @return a [FileSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
fun ImmutableKmClass.toFileSpec(
  classInspector: ClassInspector?,
  className: ClassName = bestGuessClassName(name)
): FileSpec {
  return FileSpec.get(
      packageName = className.packageName,
      typeSpec = toTypeSpec(classInspector, className)
  )
}

private const val NOT_IMPLEMENTED = "throw·NotImplementedError(\"Stub!\")"

@KotlinPoetMetadataPreview
private fun List<ImmutableKmTypeParameter>.toTypeParamsResolver(
  fallback: ((Int) -> TypeVariableName)? = null
): (Int) -> TypeVariableName {
  val parametersMap = mutableMapOf<Int, TypeVariableName>()
  val typeParamResolver = { id: Int ->
    parametersMap[id]
        ?: fallback?.invoke(id)
        ?: throw IllegalStateException("No type argument found for $id!")
  }
  // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined params
  forEach {
    // Put the simple typevar in first, then it can be referenced in the full toTypeVariable()
    // replacement later that may add bounds referencing this.
    parametersMap[it.id] = TypeVariableName(it.name)
    // Now replace it with the full version.
    parametersMap[it.id] = it.toTypeVariableName(typeParamResolver)
  }
  return typeParamResolver
}

@KotlinPoetMetadataPreview
private fun ImmutableKmClass.toTypeSpec(
  classInspector: ClassInspector?,
  className: ClassName,
  parentClassName: ClassName?
): TypeSpec {
  val classTypeParamsResolver = typeParameters.toTypeParamsResolver()
  val jvmInternalName = name.jvmInternalName
  val simpleName = className.simpleName
  val classData = classInspector?.classData(className, parentClassName)

  val builder = when {
    isAnnotation -> TypeSpec.annotationBuilder(simpleName)
    isCompanionObject -> TypeSpec.companionObjectBuilder(companionObjectName(simpleName))
    isEnum -> TypeSpec.enumBuilder(simpleName)
    isExpect -> TypeSpec.expectClassBuilder(simpleName)
    isObject -> TypeSpec.objectBuilder(simpleName)
    isInterface -> TypeSpec.interfaceBuilder(simpleName)
    isEnumEntry -> TypeSpec.anonymousClassBuilder()
    else -> TypeSpec.classBuilder(simpleName)
  }

  classData?.annotations
      ?.filterNot {
        it.className == METADATA || it.className in JAVA_ANNOTATION_ANNOTATIONS
      }
      ?.let(builder::addAnnotations)

  if (isEnum) {
    enumEntries.forEach { entryName ->
      val typeSpec = if (classInspector != null) {
        classInspector.enumEntry(className, entryName)?.let { entry ->
          val entryClassName = className.nestedClass(entryName)
          entry.toTypeSpec(classInspector, entryClassName, parentClassName = className)
        }
      } else {
        TypeSpec.anonymousClassBuilder()
            .addKdoc(
                "No ClassInspector was available during metadata parsing, so this entry may not be reflected accurately if it has a class body.")
            .build()
      }
      if (typeSpec != null) {
        builder.addEnumConstant(entryName, typeSpec)
      } else {
        builder.addEnumConstant(entryName)
      }
    }
  }

  if (!isEnumEntry) {
    addVisibility { builder.addModifiers(it) }
    builder.addModifiers(*flags.modalities
        .filterNot { it == FINAL } // Default
        .filterNot { isInterface && it == ABSTRACT } // Abstract is a default on interfaces
        .toTypedArray()
    )
    if (isData) {
      builder.addModifiers(DATA)
    }
    if (isExternal) {
      builder.addModifiers(EXTERNAL)
    }
    if (isInline) {
      builder.addModifiers(INLINE)
    }
    if (isInner) {
      builder.addModifiers(INNER)
    }
    builder.addTypeVariables(typeParameters.map { it.toTypeVariableName(classTypeParamsResolver) })
    // If we have an inspector, we can check exactly which "supertype" is an interface vs
    // class. Without a handler though, we have to best-effort guess. Usually, the flow is:
    // - First element of a non-interface type is the superclass (can be `Any`)
    // - First element of an interface type is the first superinterface
    val superClassFilter = classInspector?.let { handler ->
      { type: ImmutableKmType ->
        !handler.isInterface(bestGuessClassName((type.classifier as KmClassifier.Class).name))
      }
    } ?: { true }
    val superClass = supertypes.asSequence()
        .filter { it.classifier is KmClassifier.Class }
        .find(superClassFilter)
    if (superClass != null && !isEnum && !isInterface && !isAnnotation) {
      superClass.toTypeName(classTypeParamsResolver).takeIf { it != ANY }
          ?.let(builder::superclass)
    }
    builder.addSuperinterfaces(
        supertypes.asSequence()
            .filterNot { it == superClass }
            .map { it.toTypeName(classTypeParamsResolver) }
            .filterNot { it == ANY }
            .asIterable()
    )
    val primaryConstructorParams = mutableMapOf<String, ParameterSpec>()
    if (isClass || isAnnotation || isEnum) {
      primaryConstructor?.let {
        it.toFunSpec(classTypeParamsResolver, classData?.constructors?.get(it) ?: return@let)
            .also { spec ->
              val finalSpec = if (isEnum && spec.annotations.isEmpty()) {
                // Metadata specifies the constructor as private, but that's implicit so we can omit it
                spec.toBuilder().apply { modifiers.remove(PRIVATE) }.build()
              } else spec
              builder.primaryConstructor(finalSpec)
              primaryConstructorParams.putAll(spec.parameters.associateBy { it.name })
            }
      }
      constructors.filter { !it.isPrimary }.takeIf { it.isNotEmpty() }?.let { secondaryConstructors ->
        builder.addFunctions(secondaryConstructors
            .mapNotNull { kmConstructor ->
              classData?.constructors?.get(kmConstructor)?.let { kmConstructor to it }
            }
            .map { (kmConstructor, constructorData) ->
              kmConstructor.toFunSpec(classTypeParamsResolver, constructorData)
        })
      }
    }
    builder.addProperties(
        properties
            .asSequence()
            .filter { it.isDeclaration }
            .filterNot { it.isSynthesized }
            .map { it to classData?.properties?.get(it) }
            .map { (property, propertyData) ->
              val annotations = mutableListOf<AnnotationSpec>()
              if (propertyData != null) {
                if (property.hasGetter && !isAbstract) {
                  property.getterSignature?.let { getterSignature ->
                    if (!isInterface &&
                        classInspector?.supportsNonRuntimeRetainedAnnotations == false) {
                      // Infer if JvmName was used
                      // We skip interface types for this because they can't have @JvmName.
                      // For annotation properties, kotlinc puts JvmName annotations by default in
                      // bytecode but they're implicit in source, so we expect the simple name for
                      // annotation types.
                      val expectedMetadataName = if (isAnnotation) {
                        property.name
                      } else {
                        "get${property.name.safeCapitalize(Locale.US)}"
                      }
                      getterSignature.jvmNameAnnotation(
                          metadataName = expectedMetadataName,
                          useSiteTarget = UseSiteTarget.GET
                      )?.let { jvmNameAnnotation ->
                        annotations += jvmNameAnnotation
                      }
                    }
                  }
                }
                if (property.hasSetter && !isAbstract) {
                  property.setterSignature?.let { setterSignature ->
                    if (!isAnnotation &&
                        !isInterface &&
                        classInspector?.supportsNonRuntimeRetainedAnnotations == false) {
                      // Infer if JvmName was used
                      // We skip annotation types for this because they can't have vars.
                      // We skip interface types for this because they can't have @JvmName.
                      setterSignature.jvmNameAnnotation(
                          metadataName = "set${property.name.safeCapitalize(Locale.US)}",
                          useSiteTarget = UseSiteTarget.SET
                      )?.let { jvmNameAnnotation ->
                        annotations += jvmNameAnnotation
                      }
                    }
                  }
                }
              }
              property.toPropertySpec(
                  typeParamResolver = classTypeParamsResolver,
                  isConstructorParam = property.name in primaryConstructorParams,
                  annotations = ClassInspectorUtil.createAnnotations {
                    addAll(annotations)
                    addAll(propertyData?.allAnnotations.orEmpty())
                  },
                  propertyData = propertyData
              )
            }
            .asIterable()
    )
    companionObject?.let { objectName ->
      val companionType = if (classInspector != null) {
        val companionClassName = className.nestedClass(objectName)
        classInspector.classFor(companionClassName)
            .toTypeSpec(classInspector, companionClassName, parentClassName = className)
      } else {
        TypeSpec.companionObjectBuilder(companionObjectName(objectName))
            .addKdoc(
                "No ClassInspector was available during metadata parsing, so this companion object's API/contents may not be reflected accurately.")
            .build()
      }
      builder.addType(companionType)
    }
  }
  builder.addFunctions(
      functions
          .asSequence()
          .filter { it.isDeclaration }
          .filterNot { it.isDelegation }
          .filterNot { it.isSynthesized }
          .map { it to classData?.methods?.get(it) }
          .map { (func, methodData) ->
            val functionTypeParamsResolver = func.typeParameters.toTypeParamsResolver(
                fallback = classTypeParamsResolver)
            val annotations = mutableListOf<AnnotationSpec>()
            if (classInspector != null) {
              func.signature?.let { signature ->
                if (!isInterface && !classInspector.supportsNonRuntimeRetainedAnnotations) {
                  // Infer if JvmName was used
                  // We skip interface types for this because they can't have @JvmName.
                  signature.jvmNameAnnotation(func.name)?.let { jvmNameAnnotation ->
                    annotations += jvmNameAnnotation
                  }
                }
              }
            }
            val finalAnnotations = ClassInspectorUtil.createAnnotations {
              addAll(annotations)
              addAll(methodData?.allAnnotations().orEmpty())
            }
            func.toFunSpec(functionTypeParamsResolver, finalAnnotations, methodData)
                .toBuilder()
                .apply {
                  // For interface methods, remove any body and mark the methods as abstract
                  fun isKotlinDefaultInterfaceMethod(): Boolean {
                    classInspector?.let { handler ->
                      func.signature?.let { signature ->
                        val suffix = signature.desc.removePrefix("(")
                        return handler.methodExists(
                            className.nestedClass("DefaultImpls"),
                            signature.copy(
                                desc = "(L$jvmInternalName;$suffix"
                            ))
                      }
                    }
                    return false
                  }
                  // For interface methods, remove any body and mark the methods as abstract
                  // IFF it doesn't have a default interface body.
                  if (isInterface &&
                      finalAnnotations.none { it.className == JVM_DEFAULT } &&
                      !isKotlinDefaultInterfaceMethod()
                  ) {
                    addModifiers(ABSTRACT)
                    clearBody()
                  } else if (ABSTRACT in modifiers) {
                    // Remove bodies for abstract functions
                    clearBody()
                  }
                  if (methodData?.isSynthetic == true) {
                    addKdoc("Note: Since this is a synthetic function, some JVM information " +
                        "(annotations, modifiers) may be missing.")
                  }
                }
                .build()
          }
          .asIterable()
  )

  for (it in nestedClasses) {
    val nestedClassName = className.nestedClass(it)
    val nestedClass = classInspector?.classFor(nestedClassName)
    val nestedType = if (nestedClass != null) {
      if (nestedClass.isCompanionObject) {
        // We handle these separately
        continue
      } else {
        nestedClass.toTypeSpec(classInspector, nestedClassName, parentClassName = className)
      }
    } else {
      TypeSpec.classBuilder(it)
          .addKdoc(
              "No ClassInspector was available during metadata parsing, so this nested class's API/contents may not be reflected accurately.")
          .build()
    }
    builder.addType(nestedType)
  }

  return builder
      .tag(this)
      .build()
}

private fun companionObjectName(name: String): String? {
  return if (name == "Companion") null else name
}

@KotlinPoetMetadataPreview
private fun ImmutableKmConstructor.toFunSpec(
  typeParamResolver: ((index: Int) -> TypeName),
  constructorData: ConstructorData?
): FunSpec {
  return FunSpec.constructorBuilder()
      .apply {
        addAnnotations(constructorData?.allAnnotations.orEmpty())
        addVisibility { addModifiers(it) }
        addParameters(this@toFunSpec.valueParameters.mapIndexed { index, param ->
          param.toParameterSpec(
              typeParamResolver,
              constructorData?.takeIf { it != ConstructorData.EMPTY }
                  ?.parameterAnnotations
                  ?.get(index)
                  .orEmpty()
          )
        })
        if (!isPrimary) {
          // TODO How do we know when to add callSuperConstructor()?
        }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmFunction.toFunSpec(
  typeParamResolver: ((index: Int) -> TypeName),
  annotations: Iterable<AnnotationSpec>,
  methodData: MethodData?
): FunSpec {
  return FunSpec.builder(name)
      .apply {
        addAnnotations(annotations)
        addVisibility { addModifiers(it) }
        val isOverride = methodData?.isOverride == true
        addModifiers(flags.modalities
            .filterNot { it == FINAL && !isOverride } // Final is the default
            .filterNot { it == OPEN && isOverride } // Overrides are implicitly open
        )
        if (valueParameters.isNotEmpty()) {
          addParameters(valueParameters.mapIndexed { index, param ->
            param.toParameterSpec(
                typeParamResolver,
                methodData?.parameterAnnotations?.getValue(index).orEmpty()
            )
          })
        }
        if (typeParameters.isNotEmpty()) {
          addTypeVariables(typeParameters.map { it.toTypeVariableName(typeParamResolver) })
        }
        if (isOverride) {
          addModifiers(KModifier.OVERRIDE)
        }
        if (isOperator) {
          addModifiers(OPERATOR)
        }
        if (isInfix) {
          addModifiers(INFIX)
        }
        if (isInline) {
          addModifiers(INLINE)
        }
        if (isTailRec) {
          addModifiers(TAILREC)
        }
        if (isExternal) {
          addModifiers(EXTERNAL)
        }
        if (isExpect) {
          addModifiers(EXPECT)
        }
        if (isSuspend) {
          addModifiers(SUSPEND)
        }
        val returnTypeName = this@toFunSpec.returnType.toTypeName(typeParamResolver)
        if (returnTypeName != UNIT) {
          returns(returnTypeName)
          addStatement(NOT_IMPLEMENTED)
        }
        receiverParameterType?.toTypeName(typeParamResolver)?.let { receiver(it) }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmValueParameter.toParameterSpec(
  typeParamResolver: ((index: Int) -> TypeName),
  annotations: Collection<AnnotationSpec>
): ParameterSpec {
  val paramType = varargElementType ?: type ?: throw IllegalStateException("No argument type!")
  return ParameterSpec.builder(name, paramType.toTypeName(typeParamResolver))
      .apply {
        addAnnotations(annotations)
        if (varargElementType != null) {
          addModifiers(VARARG)
        }
        if (isCrossInline) {
          addModifiers(CROSSINLINE)
        }
        if (isNoInline) {
          addModifiers(NOINLINE)
        }
        if (declaresDefaultValue) {
          defaultValue(NOT_IMPLEMENTED)
        }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmProperty.toPropertySpec(
  typeParamResolver: ((index: Int) -> TypeName),
  isConstructorParam: Boolean,
  annotations: Iterable<AnnotationSpec>,
  propertyData: PropertyData?
): PropertySpec {
  val isOverride = propertyData?.isOverride ?: false
  val returnTypeName = returnType.toTypeName(typeParamResolver)
  return PropertySpec.builder(name, returnTypeName)
      .apply {
        // If a property annotation doesn't have a custom site target and is used in a constructor
        // we have to add the property: site target to it.
        val finalAnnotations = annotations
            .filterNot { isConst && it.className == JVM_STATIC }
            .map {
              if (isConstructorParam && it.useSiteTarget == null) {
                // TODO Ideally don't do this if the annotation use site is only field?
                //  e.g. JvmField. It's technically fine, but redundant on parameters as it's
                //  automatically applied to the property for these annotation types.
                //  This is another thing ClassInspector *could* tell us
                it.toBuilder().useSiteTarget(UseSiteTarget.PROPERTY).build()
              } else {
                it
              }
            }
        addAnnotations(finalAnnotations)
        addVisibility { addModifiers(it) }
        addModifiers(flags.modalities
            .filterNot { it == FINAL && !isOverride } // Final is the default
            .filterNot { it == OPEN && isOverride } // Overrides are implicitly open
        )
        if (isOverride) {
          addModifiers(KModifier.OVERRIDE)
        }
        if (isConst) {
          addModifiers(CONST)
        }
        if (isVar) {
          mutable(true)
        } else if (isVal) {
          mutable(false)
        }
        if (isDelegated) {
          // Placeholders for these are tricky
          addKdoc("Note: delegation is ABI stub only and not guaranteed to match source code.")
          if (isVal) {
            delegate("%M { %L }", MemberName("kotlin", "lazy"), NOT_IMPLEMENTED) // Placeholder
          } else {
            if (returnTypeName.isNullable) {
              delegate("%T.observable(null) { _, _, _ -> }",
                  ClassName("kotlin.properties", "Delegates"))
            } else {
              delegate("%T.notNull()", ClassName("kotlin.properties", "Delegates")) // Placeholder
            }
          }
        }
        if (isExpect) {
          addModifiers(EXPECT)
        }
        if (isExternal) {
          addModifiers(EXTERNAL)
        }
        if (isLateinit) {
          addModifiers(LATEINIT)
        }
        if (isConstructorParam || (!isDelegated && !isLateinit)) {
          val constant = propertyData?.fieldData?.constant
          when {
            constant != null -> initializer(constant)
            isConstructorParam -> initializer(name)
            returnTypeName.isNullable -> initializer("null")
            isAbstract -> {
              // No-op, don't emit an initializer for abstract properties
            }
            else -> initializer(NOT_IMPLEMENTED)
          }
        }
        // Delegated properties have setters/getters defined for some reason, ignore here
        // since the delegate handles it
        // vals with initialized constants have a getter in bytecode but not a body in kotlin source
        val modifierSet = modifiers.toSet()
        if (hasGetter && !isDelegated && !isAbstract) {
          propertyAccessor(modifierSet, getterFlags,
              FunSpec.getterBuilder().addStatement(NOT_IMPLEMENTED), isOverride)?.let(::getter)
        }
        if (hasSetter && !isDelegated && !isAbstract) {
          propertyAccessor(modifierSet, setterFlags, FunSpec.setterBuilder(), isOverride)?.let(::setter)
        }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun propertyAccessor(
  propertyModifiers: Set<KModifier>,
  flags: Flags,
  functionBuilder: Builder,
  isOverride: Boolean
): FunSpec? {
  val visibility = flags.visibility
  if (visibility == PUBLIC || visibility !in propertyModifiers) {
    // This is redundant and just a stub
    // For annotations on this accessor, we declare them on the property with site target instead
    return null
  }
  val modalities = flags.modalities
      .filterNot { it == FINAL && !isOverride }
      .filterNot { it == OPEN && isOverride }
  val propertyAccessorFlags = flags.propertyAccessorFlags
  return if (visibility != PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
    functionBuilder
        .apply {
          addModifiers(visibility)
          addModifiers(modalities)
          addModifiers(*propertyAccessorFlags.toKModifiersArray())
        }
        .build()
  } else {
    null
  }
}

private fun Set<PropertyAccessorFlag>.toKModifiersArray(): Array<KModifier> {
  return mapNotNull {
    when (it) {
      IS_EXTERNAL -> EXTERNAL
      IS_INLINE -> INLINE
      IS_NOT_DEFAULT -> null // Gracefully skip over these
    }
  }.toTypedArray()
}

private fun JvmMethodSignature.jvmNameAnnotation(
  metadataName: String,
  useSiteTarget: UseSiteTarget? = null
): AnnotationSpec? {
  return if (name == metadataName) {
    null
  } else {
    return AnnotationSpec.builder(JvmName::class)
        .addMember("name = %S", name)
        .useSiteTarget(useSiteTarget)
        .build()
  }
}

private val JAVA_ANNOTATION_ANNOTATIONS = setOf(
  java.lang.annotation.Retention::class.asClassName(),
  java.lang.annotation.Target::class.asClassName()
)

@KotlinPoetMetadataPreview
private val Flags.visibility: KModifier
  get() = when {
    isInternal -> INTERNAL
    isPrivate -> PRIVATE
    isProtected -> PROTECTED
    isPublic -> PUBLIC
    else -> {
      // IS_PRIVATE_TO_THIS or IS_LOCAL, so just default to public
      PUBLIC
    }
  }

@KotlinPoetMetadataPreview
private fun ImmutableKmWithFlags.addVisibility(body: (KModifier) -> Unit) {
  val modifierVisibility = flags.visibility
  if (modifierVisibility != PUBLIC) {
    body(modifierVisibility)
  }
}

// TODO This is a copy of the stdlib version. Use it directly once it's out of experimental
private fun String.safeCapitalize(locale: Locale): String {
  if (isNotEmpty()) {
    val firstChar = this[0]
    if (firstChar.isLowerCase()) {
      return buildString {
        val titleChar = firstChar.toTitleCase()
        if (titleChar != firstChar.toUpperCase()) {
          append(titleChar)
        } else {
          append(this@safeCapitalize.substring(0, 1).toUpperCase(locale))
        }
        append(this@safeCapitalize.substring(1))
      }
    }
  }
  return this
}

@KotlinPoetMetadataPreview
private val Flags.modalities: Set<KModifier>
  get() = setOf {
    if (isFinal) {
      add(FINAL)
    }
    if (isOpen) {
      add(OPEN)
    }
    if (isAbstract) {
      add(ABSTRACT)
    }
    if (isSealed) {
      add(SEALED)
    }
  }

private inline fun <E> setOf(body: MutableSet<E>.() -> Unit): Set<E> {
  return mutableSetOf<E>().apply(body).toSet()
}

private val METADATA = Metadata::class.asClassName()
private val JVM_DEFAULT = JvmDefault::class.asClassName()
private val JVM_STATIC = JvmStatic::class.asClassName()

@PublishedApi
internal val Element.packageName: String
  get() {
    var element = this
    while (element.kind != ElementKind.PACKAGE) {
      element = element.enclosingElement
    }
    return (element as PackageElement).toString()
  }

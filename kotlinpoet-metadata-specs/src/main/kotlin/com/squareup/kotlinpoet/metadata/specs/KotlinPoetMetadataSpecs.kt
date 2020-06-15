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
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
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
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.ImmutableKmConstructor
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmPackage
import com.squareup.kotlinpoet.metadata.ImmutableKmProperty
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmTypeAlias
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.ImmutableKmWithFlags
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_EXTERNAL
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_INLINE
import com.squareup.kotlinpoet.metadata.PropertyAccessorFlag.IS_NOT_DEFAULT
import com.squareup.kotlinpoet.metadata.declaresDefaultValue
import com.squareup.kotlinpoet.metadata.hasAnnotations
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
import com.squareup.kotlinpoet.metadata.isReified
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.kotlinpoet.metadata.isSuspend
import com.squareup.kotlinpoet.metadata.isSynthesized
import com.squareup.kotlinpoet.metadata.isTailRec
import com.squareup.kotlinpoet.metadata.isVal
import com.squareup.kotlinpoet.metadata.isVar
import com.squareup.kotlinpoet.metadata.propertyAccessorFlags
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.createAnnotations
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.createClassName
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil.toTreeSet
import com.squareup.kotlinpoet.metadata.specs.internal.TypeParameterResolver
import com.squareup.kotlinpoet.metadata.specs.internal.primaryConstructor
import com.squareup.kotlinpoet.metadata.specs.internal.toAnnotationSpec
import com.squareup.kotlinpoet.metadata.specs.internal.toTypeName
import com.squareup.kotlinpoet.metadata.specs.internal.toTypeParameterResolver
import com.squareup.kotlinpoet.metadata.specs.internal.toTypeVariableName
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import java.util.Locale
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.jvmInternalName

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
public fun KClass<*>.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = java.toTypeSpec(classInspector)

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
public fun Class<*>.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(classInspector, asClassName())

/** @return a [TypeSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
public fun TypeElement.toTypeSpec(
  classInspector: ClassInspector? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(classInspector, asClassName())

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
public fun KClass<*>.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = java.toFileSpec(classInspector)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
public fun Class<*>.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = FileSpec.get(`package`.name, toTypeSpec(classInspector))

/** @return a [FileSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
public fun TypeElement.toFileSpec(
  classInspector: ClassInspector? = null
): FileSpec = FileSpec.get(
    packageName = packageName,
    typeSpec = toTypeSpec(classInspector)
)

/** @return a [TypeSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
public fun ImmutableKmClass.toTypeSpec(
  classInspector: ClassInspector?,
  className: ClassName = createClassName(name)
): TypeSpec {
  return toTypeSpec(classInspector, className, null)
}

/** @return a [FileSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
public fun ImmutableKmClass.toFileSpec(
  classInspector: ClassInspector?,
  className: ClassName = createClassName(name)
): FileSpec {
  return FileSpec.get(
      packageName = className.packageName,
      typeSpec = toTypeSpec(classInspector, className)
  )
}

/** @return a [FileSpec] ABI representation of this [ImmutableKmPackage]. */
@KotlinPoetMetadataPreview
public fun ImmutableKmPackage.toFileSpec(
  classInspector: ClassInspector?,
  className: ClassName
): FileSpec {
  val fileData = classInspector?.containerData(className, null)
  check(fileData is FileData?) {
    "Unexpected container data type: ${fileData?.javaClass}"
  }
  val fileName = fileData?.let { (it as FileData).fileName } ?: className.simpleName
  return FileSpec.builder(className.packageName, fileName)
      .apply {
        fileData?.let { data ->
          (data as FileData).jvmName?.let { name ->
            addAnnotation(AnnotationSpec.builder(ClassInspectorUtil.JVM_NAME)
                .addMember("name = %S", name)
                .build())
          }
          val fileAnnotations = createAnnotations(FILE) {
            addAll(data.annotations.filterNot { it.typeName == METADATA })
          }
          for (fileAnnotation in fileAnnotations) {
            addAnnotation(fileAnnotation)
          }
        }
        for (function in functions) {
          val methodData = fileData?.methods?.get(function)
          addFunction(function.toFunSpec(
              classInspector = classInspector,
              containerData = fileData,
              methodData = methodData,
              isInInterface = false
          ))
        }
        for (property in properties) {
          val propertyData = fileData?.properties?.get(property)
          addProperty(property.toPropertySpec(
              classInspector = classInspector,
              containerData = fileData,
              propertyData = propertyData,
              isInInterface = false
          ))
        }
        for (alias in typeAliases) {
          addTypeAlias(alias.toTypeAliasSpec())
        }
      }
      .build()
}

private const val NOT_IMPLEMENTED = "throw·NotImplementedError(\"Stub!\")"

@KotlinPoetMetadataPreview
private fun ImmutableKmClass.toTypeSpec(
  classInspector: ClassInspector?,
  className: ClassName,
  parentClassName: ClassName?
): TypeSpec {
  val classTypeParamsResolver = typeParameters.toTypeParameterResolver()
  val jvmInternalName = name.jvmInternalName
  val simpleName = className.simpleName
  val classData = classInspector?.containerData(className, parentClassName)
  check(classData is ClassData?) {
    "Unexpected container data type: ${classData?.javaClass}"
  }

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
        it.typeName == METADATA || it.typeName in JAVA_ANNOTATION_ANNOTATIONS
      }
      ?.let(builder::addAnnotations)

  if (isEnum) {
    enumEntries.forEach { entryName ->
      val typeSpec = if (classInspector != null) {
        val entry = classInspector.enumEntry(className, entryName)
        entry.declarationContainer
            ?.let { enumEntryClass ->
              val entryClassName = className.nestedClass(entryName)
              enumEntryClass.toTypeSpec(classInspector, entryClassName, parentClassName = className)
            }
            ?: TypeSpec.anonymousClassBuilder()
              .addAnnotations(entry.annotations)
              .build()
      } else {
        TypeSpec.anonymousClassBuilder()
            .addKdoc(
                "No ClassInspector was available during metadata parsing, so this entry may not be reflected accurately if it has a class body.")
            .build()
      }
      builder.addEnumConstant(entryName, typeSpec)
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
        !handler.isInterface(createClassName((type.classifier as KmClassifier.Class).name))
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
              property.toPropertySpec(
                  typeParamResolver = classTypeParamsResolver,
                  isConstructorParam = property.name in primaryConstructorParams,
                  classInspector = classInspector,
                  containerData = classData,
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
            func.toFunSpec(classTypeParamsResolver, classInspector, classData, methodData)
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
                      annotations.none { it.typeName == JVM_DEFAULT } &&
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
  typeParamResolver: TypeParameterResolver,
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
private val ContainerData.isInterface: Boolean get() {
  return declarationContainer.let { container ->
    container is ImmutableKmClass && container.isInterface
  }
}

@KotlinPoetMetadataPreview
private fun ImmutableKmFunction.toFunSpec(
  classTypeParamsResolver: TypeParameterResolver = TypeParameterResolver.EMPTY,
  classInspector: ClassInspector? = null,
  containerData: ContainerData? = null,
  methodData: MethodData? = null,
  isInInterface: Boolean = containerData?.isInterface ?: false
): FunSpec {
  val typeParamsResolver = typeParameters.toTypeParameterResolver(
      fallback = classTypeParamsResolver
  )
  val mutableAnnotations = mutableListOf<AnnotationSpec>()
  if (classInspector != null && containerData != null) {
    signature?.let { signature ->
      if (!containerData.isInterface) {
        // Infer if JvmName was used
        // We skip interface types for this because they can't have @JvmName.
        signature.jvmNameAnnotation(name)?.let { jvmNameAnnotation ->
          mutableAnnotations += jvmNameAnnotation
        }
      }
    }
  }
  val anyReified = typeParameters.any { it.isReified }
  val isInFacade = containerData is FileData
  val annotations = mutableAnnotations
      .plus(methodData?.allAnnotations(containsReifiedTypeParameter = anyReified).orEmpty())
      .filterNot { isInFacade && it.typeName == JVM_STATIC }
      .toTreeSet()
  return FunSpec.builder(name)
      .apply {
        addAnnotations(annotations)
        addVisibility { addModifiers(it) }
        val isOverride = methodData?.isOverride == true
        addModifiers(flags.modalities
            .filterNot { it == FINAL && !isOverride } // Final is the default
            .filterNot { it == OPEN && isOverride } // Overrides are implicitly open
            .filterNot { it == OPEN && isInInterface } // interface methods are implicitly open
        )
        if (valueParameters.isNotEmpty()) {
          addParameters(valueParameters.mapIndexed { index, param ->
            param.toParameterSpec(
                typeParamsResolver,
                // This can be empty if the element is synthetic
                methodData?.parameterAnnotations?.get(index).orEmpty()
            )
          })
        }
        if (typeParameters.isNotEmpty()) {
          addTypeVariables(typeParameters.map { it.toTypeVariableName(typeParamsResolver) })
        }
        if (methodData?.isOverride == true) {
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
        val returnTypeName = this@toFunSpec.returnType.toTypeName(typeParamsResolver)
        if (returnTypeName != UNIT) {
          returns(returnTypeName)
          if (!isAbstract) {
            addStatement(NOT_IMPLEMENTED)
          }
        }
        receiverParameterType?.toTypeName(typeParamsResolver)?.let { receiver(it) }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmValueParameter.toParameterSpec(
  typeParamResolver: TypeParameterResolver,
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
  typeParamResolver: TypeParameterResolver = TypeParameterResolver.EMPTY,
  isConstructorParam: Boolean = false,
  classInspector: ClassInspector? = null,
  containerData: ContainerData? = null,
  propertyData: PropertyData? = null,
  isInInterface: Boolean = containerData?.isInterface ?: false
): PropertySpec {
  val isOverride = propertyData?.isOverride ?: false
  val returnTypeName = returnType.toTypeName(typeParamResolver)
  val mutableAnnotations = mutableListOf<AnnotationSpec>()
  if (containerData != null && propertyData != null) {
    if (hasGetter) {
      getterSignature?.let { getterSignature ->
        if (!containerData.isInterface &&
            !isOpen &&
            !isAbstract) {
          // Infer if JvmName was used
          // We skip interface types or open/abstract properties because they can't have @JvmName.
          // For annotation properties, kotlinc puts JvmName annotations by default in
          // bytecode but they're implicit in source, so we expect the simple name for
          // annotation types.
          val expectedMetadataName = if (containerData is ClassData &&
              containerData.declarationContainer.isAnnotation) {
            name
          } else {
            "get${name.safeCapitalize(Locale.US)}"
          }
          getterSignature.jvmNameAnnotation(
              metadataName = expectedMetadataName,
              useSiteTarget = UseSiteTarget.GET
          )?.let { jvmNameAnnotation ->
            mutableAnnotations += jvmNameAnnotation
          }
        }
      }
    }
    if (hasSetter) {
      setterSignature?.let { setterSignature ->
        if (containerData is ClassData &&
            !containerData.declarationContainer.isAnnotation &&
            !containerData.declarationContainer.isInterface &&
            classInspector?.supportsNonRuntimeRetainedAnnotations == false &&
            !isOpen &&
            !isAbstract) {
          // Infer if JvmName was used
          // We skip annotation types for this because they can't have vars.
          // We skip interface types or open/abstract properties because they can't have @JvmName.
          setterSignature.jvmNameAnnotation(
              metadataName = "set${name.safeCapitalize(Locale.US)}",
              useSiteTarget = UseSiteTarget.SET
          )?.let { jvmNameAnnotation ->
            mutableAnnotations += jvmNameAnnotation
          }
        }
      }
    }
  }
  return PropertySpec.builder(name, returnTypeName)
      .apply {
        // If a property annotation doesn't have a custom site target and is used in a constructor
        // we have to add the property: site target to it.

        val isInFacade = containerData is FileData
        val finalAnnotations = mutableAnnotations
            .plus(propertyData?.allAnnotations.orEmpty())
            .filterNot { (isConst || isInFacade) && it.typeName == JVM_STATIC }
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
            .toTreeSet()
        addAnnotations(finalAnnotations)
        addVisibility { addModifiers(it) }
        addModifiers(flags.modalities
            .filterNot { it == FINAL && !isOverride } // Final is the default
            .filterNot { it == OPEN && isOverride } // Overrides are implicitly open
            .filterNot { it == OPEN && isInInterface } // Interface properties implicitly open
            .filterNot { it == ABSTRACT && isInInterface } // Interface properties implicitly abstract
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
            isAbstract || isInInterface -> {
              // No-op, don't emit an initializer for abstract or interface properties
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

@KotlinPoetMetadataPreview
private fun ImmutableKmTypeAlias.toTypeAliasSpec(): TypeAliasSpec {
  val typeParamResolver = typeParameters.toTypeParameterResolver()
  return TypeAliasSpec.builder(name, underlyingType.toTypeName(typeParamResolver))
      .apply {
        addVisibility {
          addModifiers(it)
        }
        if (hasAnnotations) {
          val annotationSpecs = this@toTypeAliasSpec.annotations
              .map { it.toAnnotationSpec() }
          addAnnotations(annotationSpecs)
        }
      }
      .addTypeVariables(typeParamResolver.parametersMap.values)
      .build()
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

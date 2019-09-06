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
import com.squareup.kotlinpoet.CodeBlock
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
import com.squareup.kotlinpoet.joinToCode
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
import com.squareup.kotlinpoet.metadata.hasAnnotations
import com.squareup.kotlinpoet.metadata.hasConstant
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
  elementHandler: ElementHandler? = null
): TypeSpec = java.toTypeSpec(elementHandler)

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun Class<*>.toTypeSpec(
  elementHandler: ElementHandler? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(elementHandler)

/** @return a [TypeSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
fun TypeElement.toTypeSpec(
  elementHandler: ElementHandler? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(elementHandler)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun KClass<*>.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = java.toFileSpec(elementHandler)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetMetadataPreview
fun Class<*>.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = FileSpec.get(`package`.name, toTypeSpec(elementHandler))

/** @return a [FileSpec] ABI representation of this [TypeElement]. */
@KotlinPoetMetadataPreview
fun TypeElement.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = FileSpec.get(
    packageName = packageName,
    typeSpec = toTypeSpec(elementHandler)
)

/** @return a [TypeSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
fun ImmutableKmClass.toTypeSpec(
  elementHandler: ElementHandler?
): TypeSpec {
  return toTypeSpec(elementHandler, null)
}

/** @return a [FileSpec] ABI representation of this [ImmutableKmClass]. */
@KotlinPoetMetadataPreview
fun ImmutableKmClass.toFileSpec(
  elementHandler: ElementHandler?
): FileSpec {
  return FileSpec.get(
      packageName = name.jvmInternalName.substringBeforeLast("/"),
      typeSpec = toTypeSpec(elementHandler)
  )
}

private const val TODO_BLOCK = "TODO(\"Stub!\")"

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
  forEach { parametersMap[it.id] = it.toTypeVariableName(typeParamResolver) }
  return typeParamResolver
}

@KotlinPoetMetadataPreview
private fun ImmutableKmClass.toTypeSpec(
  elementHandler: ElementHandler?,
  parentName: String?
): TypeSpec {
  val classTypeParamsResolver = typeParameters.toTypeParamsResolver()

  // Top-level: package/of/class/MyClass
  // Nested A:  package/of/class/MyClass.InnerClass
  // Nested B:  package/of/class/MyClass$InnerClass
  val simpleName = name.substringAfterLast(
      '/', // Drop the package name, e.g. "package/of/class/"
      '.', // Drop any enclosing classes, e.g. "MyClass."
      '$' // Drop any enclosing classes, e.g. "MyClass$"
  )
  val jvmInternalName = name.jvmInternalName
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

  if (isEnum) {
    enumEntries.forEach { entryName ->
      val typeSpec = if (elementHandler != null) {
        elementHandler.enumEntry(jvmInternalName, entryName)?.toTypeSpec(elementHandler)
      } else {
        TypeSpec.anonymousClassBuilder()
            .addKdoc(
                "No ElementHandler was available during metadata parsing, so this entry may not be reflected accurately if it has a class body.")
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
    // If we have an element handler, we can check exactly which "supertype" is an interface vs
    // class. Without a handler though, we have to best-effort guess. Usually, the flow is:
    // - First element of a non-interface type is the superclass (can be `Any`)
    // - First element of an interface type is the first superinterface
    val superClassFilter = elementHandler?.let { handler ->
      { type: ImmutableKmType ->
        !handler.isInterface((type.classifier as KmClassifier.Class).name.jvmInternalName)
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
        it.toFunSpec(classTypeParamsResolver, it.annotations(jvmInternalName, elementHandler))
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
        builder.addFunctions(secondaryConstructors.map {
          it.toFunSpec(classTypeParamsResolver, it.annotations(jvmInternalName, elementHandler))
        })
      }
    }
    builder.addProperties(
        properties
            .asSequence()
            .filter { it.isDeclaration }
            .filterNot { it.isSynthesized }
            .map { property ->
              val annotations = LinkedHashSet<AnnotationSpec>()
              var constant: CodeBlock? = null
              var isOverride = false
              if (elementHandler != null) {
                if (property.hasAnnotations) {
                  annotations += property.syntheticMethodForAnnotations?.let {
                    elementHandler.methodAnnotations(jvmInternalName, it)
                  }.orEmpty()
                }
                val isJvmField: Boolean
                if (property.getterSignature == null &&
                    property.setterSignature == null &&
                    property.fieldSignature != null &&
                    !property.isConst) {
                  if (elementHandler.supportsNonRuntimeRetainedAnnotations && !isCompanionObject) {
                    // Throwaway value as this kind of element handler should be automatically be
                    // picking up the jvm field annotation.
                    //
                    // We don't do this for companion object fields though as they appear to not
                    // have the JvmField annotation copied over though
                    //
                    isJvmField = false
                  } else {
                    isJvmField = true
                    annotations += AnnotationSpec.builder(JVM_FIELD).build()
                  }
                } else {
                  isJvmField = false
                }
                property.fieldSignature?.let { fieldSignature ->
                  annotations += elementHandler.fieldAnnotations(jvmInternalName, fieldSignature)
                      .map { it.toBuilder().useSiteTarget(UseSiteTarget.FIELD).build() }
                  annotations += elementHandler.fieldJvmModifiers(jvmInternalName, fieldSignature, isJvmField)
                      .map { it.annotationSpec() }
                  if (isCompanionObject && parentName != null) {
                    // These are copied into the parent
                    annotations += elementHandler.fieldJvmModifiers(parentName, fieldSignature, isJvmField)
                        .map { it.annotationSpec() }
                  }
                  if (!(isCompanionObject && (property.isConst ||
                          annotations.any { it.className == JVM_STATIC || it.className == JVM_FIELD })) &&
                      elementHandler.isFieldSynthetic(jvmInternalName, fieldSignature)) {
                    // For static, const, or JvmField fields in a companion object, the companion
                    // object's field is marked as synthetic to hide it from Java, but in this case
                    // it's a false positive for this check in kotlin.
                    annotations += AnnotationSpec.builder(JVM_SYNTHETIC)
                        .useSiteTarget(UseSiteTarget.FIELD)
                        .build()
                  }
                  if (property.hasConstant) {
                    constant = if (isCompanionObject && parentName != null) {
                      if (elementHandler.classFor(parentName).isInterface) {
                        elementHandler.fieldConstant(jvmInternalName, fieldSignature)
                      } else {
                        // const properties are relocated to the enclosing class
                        elementHandler.fieldConstant(parentName, fieldSignature)
                      }
                    } else {
                      elementHandler.fieldConstant(jvmInternalName, fieldSignature)
                    }
                  }
                }
                if (property.hasGetter && property.canHaveGetterBody) {
                  property.getterSignature?.let { getterSignature ->
                    if (!isOverride) {
                      isOverride = elementHandler.isMethodOverride(jvmInternalName, getterSignature)
                    }
                    if (property.getterFlags.hasAnnotations) {
                      annotations += elementHandler.methodAnnotations(jvmInternalName,
                          getterSignature)
                          .map { it.toBuilder().useSiteTarget(UseSiteTarget.GET).build() }
                    }
                    annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                        getterSignature)
                        .map {
                          it.annotationSpec().toBuilder().useSiteTarget(UseSiteTarget.GET).build()
                        }
                    if (elementHandler.isMethodSynthetic(jvmInternalName, getterSignature)) {
                      annotations += AnnotationSpec.builder(JvmSynthetic::class)
                          .useSiteTarget(UseSiteTarget.GET)
                          .build()
                    }
                    if (isCompanionObject && parentName != null) {
                      // These are copied into the parent
                      annotations += elementHandler.methodJvmModifiers(jvmInternalName, getterSignature)
                          .map { it.annotationSpec() }
                    }
                    if (!isInterface && !elementHandler.supportsNonRuntimeRetainedAnnotations) {
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
                    elementHandler.methodExceptions(jvmInternalName, getterSignature, false)
                        .takeIf { it.isNotEmpty() }
                        ?.let { exceptions ->
                          annotations += createThrowsSpec(exceptions, UseSiteTarget.GET)
                        }
                  }
                }
                if (property.hasSetter) {
                  property.setterSignature?.let { setterSignature ->
                    if (!isOverride) {
                      isOverride = elementHandler.isMethodOverride(jvmInternalName, setterSignature)
                    }
                    if (property.setterFlags.hasAnnotations) {
                      annotations += elementHandler.methodAnnotations(jvmInternalName,
                          setterSignature)
                          .map { it.toBuilder().useSiteTarget(UseSiteTarget.SET).build() }
                    }
                    annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                        setterSignature)
                        .map {
                          it.annotationSpec().toBuilder().useSiteTarget(UseSiteTarget.SET).build()
                        }
                    if (elementHandler.isMethodSynthetic(jvmInternalName, setterSignature)) {
                      annotations += AnnotationSpec.builder(JvmSynthetic::class)
                          .useSiteTarget(UseSiteTarget.SET)
                          .build()
                    }
                    if (isCompanionObject && parentName != null) {
                      // These are copied into the parent
                      annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                          setterSignature)
                          .map { it.annotationSpec() }
                    }
                    if (!isAnnotation &&
                        !isInterface &&
                        !elementHandler.supportsNonRuntimeRetainedAnnotations) {
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
                    elementHandler.methodExceptions(jvmInternalName, setterSignature, false)
                        .takeIf { it.isNotEmpty() }
                        ?.let { exceptions ->
                          annotations += createThrowsSpec(exceptions, UseSiteTarget.SET)
                        }
                  }
                }
              }
              property.toPropertySpec(
                  typeParamResolver = classTypeParamsResolver,
                  isConstructorParam = property.name in primaryConstructorParams,
                  annotations = annotations,
                  constant = constant,
                  isOverride = isOverride
              )
            }
            .asIterable()
    )
    companionObject?.let { objectName ->
      val companionType = if (elementHandler != null) {
        elementHandler.classFor("$jvmInternalName$$objectName")
            .toTypeSpec(elementHandler, jvmInternalName)
      } else {
        TypeSpec.companionObjectBuilder(companionObjectName(objectName))
            .addKdoc(
                "No ElementHandler was available during metadata parsing, so this companion object's API/contents may not be reflected accurately.")
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
          .map { func ->
            val functionTypeParamsResolver = func.typeParameters.toTypeParamsResolver(
                fallback = classTypeParamsResolver)
            val annotations = LinkedHashSet<AnnotationSpec>()
            var isOverride = false
            var isSynthetic = false
            if (elementHandler != null) {
              func.signature?.let { signature ->
                isSynthetic = elementHandler.isMethodSynthetic(jvmInternalName, signature)
                if (isSynthetic) {
                  annotations += AnnotationSpec.builder(JvmSynthetic::class).build()
                }
                if (func.hasAnnotations) {
                  annotations += elementHandler.methodAnnotations(jvmInternalName, signature)
                }
                annotations += elementHandler.methodJvmModifiers(jvmInternalName, signature)
                    .map { it.annotationSpec() }
                isOverride = elementHandler.isMethodOverride(jvmInternalName, signature)
                if (!isInterface && !elementHandler.supportsNonRuntimeRetainedAnnotations) {
                  // Infer if JvmName was used
                  // We skip interface types for this because they can't have @JvmName.
                  signature.jvmNameAnnotation(func.name)?.let { jvmNameAnnotation ->
                    annotations += jvmNameAnnotation
                  }
                }
                elementHandler.methodExceptions(jvmInternalName, signature, false)
                    .takeIf { it.isNotEmpty() }
                    ?.let { exceptions ->
                      annotations += createThrowsSpec(exceptions)
                    }
              }
            }
            func.toFunSpec(functionTypeParamsResolver, annotations, isOverride)
                .toBuilder()
                .apply {
                  // For interface methods, remove any body and mark the methods as abstract
                  fun isKotlinDefaultInterfaceMethod(): Boolean {
                    elementHandler?.let { handler ->
                      func.signature?.let { signature ->
                        val suffix = signature.desc.removePrefix("(")
                        return handler.methodExists(
                            "${jvmInternalName}\$DefaultImpls",
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
                      annotations.none { it.className == JVM_DEFAULT } &&
                      !isKotlinDefaultInterfaceMethod()
                  ) {
                    addModifiers(ABSTRACT)
                    clearBody()
                  }
                  if (isSynthetic) {
                    addKdoc("Note: Since this is a synthetic function, some JVM information " +
                        "(annotations, modifiers) may be missing.")
                  }
                }
                .build()
          }
          .asIterable()
  )

  for (it in nestedClasses) {
    val nestedClass = elementHandler?.classFor("$jvmInternalName$$it")
    val nestedType = if (nestedClass != null) {
      if (nestedClass.isCompanionObject) {
        // We handle these separately
        continue
      } else {
        nestedClass.toTypeSpec(elementHandler, jvmInternalName)
      }
    } else {
      TypeSpec.classBuilder(it)
          .addKdoc(
              "No ElementHandler was available during metadata parsing, so this nested class's API/contents may not be reflected accurately.")
          .build()
    }
    builder.addType(nestedType)
  }

  return builder
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmConstructor.annotations(
  classJvmName: String,
  elementHandler: ElementHandler?
): List<AnnotationSpec> {
  if (elementHandler != null) {
    signature?.let { signature ->
      val annotations = mutableListOf<AnnotationSpec>()
      if (hasAnnotations) {
        annotations += elementHandler.constructorAnnotations(classJvmName, signature)
      }
      elementHandler.methodExceptions(classJvmName, signature, true)
          .takeIf { it.isNotEmpty() }
          ?.let {
            annotations += createThrowsSpec(it)
          }
      return annotations
    }
  }
  return emptyList()
}

private fun companionObjectName(name: String): String? {
  return if (name == "Companion") null else name
}

@KotlinPoetMetadataPreview
private fun ImmutableKmConstructor.toFunSpec(
  typeParamResolver: ((index: Int) -> TypeName),
  annotations: List<AnnotationSpec>
): FunSpec {
  return FunSpec.constructorBuilder()
      .apply {
        addAnnotations(annotations)
        addVisibility { addModifiers(it) }
        addParameters(this@toFunSpec.valueParameters.map { it.toParameterSpec(typeParamResolver) })
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
  isOverride: Boolean
): FunSpec {
  return FunSpec.builder(name)
      .apply {
        addAnnotations(annotations)
        addVisibility { addModifiers(it) }
        if (valueParameters.isNotEmpty()) {
          addParameters(valueParameters.map { it.toParameterSpec(typeParamResolver) })
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
          addStatement(TODO_BLOCK)
        }
        receiverParameterType?.toTypeName(typeParamResolver)?.let { receiver(it) }
      }
      .tag(this)
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmValueParameter.toParameterSpec(
  typeParamResolver: ((index: Int) -> TypeName)
): ParameterSpec {
  val paramType = varargElementType ?: type ?: throw IllegalStateException("No argument type!")
  return ParameterSpec.builder(name, paramType.toTypeName(typeParamResolver))
      .apply {
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
          defaultValue(TODO_BLOCK)
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
  constant: CodeBlock?,
  isOverride: Boolean
): PropertySpec {
  val returnTypeName = returnType.toTypeName(typeParamResolver)
  return PropertySpec.builder(name, returnTypeName)
      .apply {
        val finalAnnotations = if (isConst) {
          annotations.filterNot { it.className == JVM_STATIC }
        } else {
          annotations
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
            delegate("%M { %L }", MemberName("kotlin", "lazy"), TODO_BLOCK) // Placeholder
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
          when {
            constant != null -> initializer(constant)
            isConstructorParam -> initializer(name)
            returnTypeName.isNullable -> initializer("null")
            else -> initializer(TODO_BLOCK)
          }
        }
        // Delegated properties have setters/getters defined for some reason, ignore here
        // since the delegate handles it
        // vals with initialized constants have a getter in bytecode but not a body in kotlin source
        val modifierSet = modifiers.toSet()
        if (hasGetter && !isDelegated && canHaveGetterBody) {
          propertyAccessor(modifierSet, getterFlags,
              FunSpec.getterBuilder().addStatement(TODO_BLOCK), isOverride)?.let(::getter)
        }
        if (hasSetter && !isDelegated) {
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

@KotlinPoetMetadataPreview
private inline val ImmutableKmProperty.canHaveGetterBody: Boolean get() = !(isVal && hasConstant)

private inline fun <E> setOf(body: MutableSet<E>.() -> Unit): Set<E> {
  return mutableSetOf<E>().apply(body).toSet()
}

private val JVM_DEFAULT = JvmDefault::class.asClassName()
private val JVM_STATIC = JvmStatic::class.asClassName()
private val JVM_FIELD = JvmField::class.asClassName()
private val JVM_SYNTHETIC = JvmSynthetic::class.asClassName()

private fun createThrowsSpec(
  exceptions: Set<TypeName>,
  useSiteTarget: UseSiteTarget? = null
): AnnotationSpec {
  return AnnotationSpec.builder(Throws::class)
      .addMember(
          "exceptionClasses = %L",
          exceptions.map { CodeBlock.of("%T::class", it) }
              .joinToCode(prefix = "[", suffix = "]")
      )
      .useSiteTarget(useSiteTarget)
      .build()
}

private fun String.substringAfterLast(vararg delimiters: Char): String {
  val index = lastIndexOfAny(delimiters)
  return if (index == -1) this else substring(index + 1, length)
}

@PublishedApi
internal val Element.packageName: String
  get() {
    var element = this
    while (element.kind != ElementKind.PACKAGE) {
      element = element.enclosingElement
    }
    return (element as PackageElement).toString()
  }

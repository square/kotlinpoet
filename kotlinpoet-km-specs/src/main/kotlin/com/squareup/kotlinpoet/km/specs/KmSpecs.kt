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
package com.squareup.kotlinpoet.km.specs

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
import com.squareup.kotlinpoet.km.ImmutableKmClass
import com.squareup.kotlinpoet.km.ImmutableKmConstructor
import com.squareup.kotlinpoet.km.ImmutableKmFunction
import com.squareup.kotlinpoet.km.ImmutableKmProperty
import com.squareup.kotlinpoet.km.ImmutableKmType
import com.squareup.kotlinpoet.km.ImmutableKmTypeParameter
import com.squareup.kotlinpoet.km.ImmutableKmValueParameter
import com.squareup.kotlinpoet.km.ImmutableKmWithFlags
import com.squareup.kotlinpoet.km.KotlinPoetKm
import com.squareup.kotlinpoet.km.PropertyAccessorFlag
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_EXTERNAL
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_INLINE
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_NOT_DEFAULT
import com.squareup.kotlinpoet.km.declaresDefaultValue
import com.squareup.kotlinpoet.km.hasAnnotations
import com.squareup.kotlinpoet.km.hasConstant
import com.squareup.kotlinpoet.km.hasGetter
import com.squareup.kotlinpoet.km.hasSetter
import com.squareup.kotlinpoet.km.isAbstract
import com.squareup.kotlinpoet.km.isAnnotation
import com.squareup.kotlinpoet.km.isCompanionObject
import com.squareup.kotlinpoet.km.isConst
import com.squareup.kotlinpoet.km.isCrossInline
import com.squareup.kotlinpoet.km.isData
import com.squareup.kotlinpoet.km.isDeclaration
import com.squareup.kotlinpoet.km.isDelegated
import com.squareup.kotlinpoet.km.isDelegation
import com.squareup.kotlinpoet.km.isEnum
import com.squareup.kotlinpoet.km.isEnumEntry
import com.squareup.kotlinpoet.km.isExpect
import com.squareup.kotlinpoet.km.isExternal
import com.squareup.kotlinpoet.km.isFinal
import com.squareup.kotlinpoet.km.isInfix
import com.squareup.kotlinpoet.km.isInline
import com.squareup.kotlinpoet.km.isInner
import com.squareup.kotlinpoet.km.isInterface
import com.squareup.kotlinpoet.km.isInternal
import com.squareup.kotlinpoet.km.isLateinit
import com.squareup.kotlinpoet.km.isNoInline
import com.squareup.kotlinpoet.km.isObject
import com.squareup.kotlinpoet.km.isOpen
import com.squareup.kotlinpoet.km.isOperator
import com.squareup.kotlinpoet.km.isPrimary
import com.squareup.kotlinpoet.km.isPrivate
import com.squareup.kotlinpoet.km.isProtected
import com.squareup.kotlinpoet.km.isPublic
import com.squareup.kotlinpoet.km.isSealed
import com.squareup.kotlinpoet.km.isSuspend
import com.squareup.kotlinpoet.km.isSynthesized
import com.squareup.kotlinpoet.km.isTailRec
import com.squareup.kotlinpoet.km.isVal
import com.squareup.kotlinpoet.km.isVar
import com.squareup.kotlinpoet.km.propertyAccessorFlags
import com.squareup.kotlinpoet.km.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.jvm.jvmInternalName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun KClass<*>.toTypeSpec(
  elementHandler: ElementHandler? = null
): TypeSpec = java.toTypeSpec(elementHandler)

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun Class<*>.toTypeSpec(
  elementHandler: ElementHandler? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(elementHandler)

/** @return a [TypeSpec] ABI representation of this [TypeElement]. */
@KotlinPoetKm
fun TypeElement.toTypeSpec(
  elementHandler: ElementHandler? = null
): TypeSpec = toImmutableKmClass().toTypeSpec(elementHandler)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun KClass<*>.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = java.toFileSpec(elementHandler)

/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun Class<*>.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = FileSpec.get(`package`.name, toTypeSpec(elementHandler))

/** @return a [FileSpec] ABI representation of this [TypeElement]. */
@KotlinPoetKm
fun TypeElement.toFileSpec(
  elementHandler: ElementHandler? = null
): FileSpec = FileSpec.get(
    packageName = packageName,
    typeSpec = toTypeSpec(elementHandler)
)

private const val TODO_BLOCK = "TODO(\"Stub!\")"

@KotlinPoetKm
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

@KotlinPoetKm
private fun ImmutableKmClass.toTypeSpec(
  elementHandler: ElementHandler?,
  parentName: String? = null
): TypeSpec {
  val classTypeParamsResolver = typeParameters.toTypeParamsResolver()

  val simpleName = name.substringAfterLast(if (isInline) "/" else ".")
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
      // TODO these are special.
      //  - Name is the fqcn
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
    val primaryConstructorSpec = primaryConstructor?.takeIf {
      it.valueParameters.isNotEmpty() || flags.visibility != PUBLIC || it.hasAnnotations
    }?.let {
      it.toFunSpec(classTypeParamsResolver, it.annotations(jvmInternalName, elementHandler))
          .also { spec ->
            val finalSpec = if (isEnum) {
              // Metadata specifies the constructor as private, but that's implicit so we can omit it
              spec.toBuilder().apply { modifiers.remove(PRIVATE) }.build()
            } else spec
            builder.primaryConstructor(finalSpec)
          }
    }
    constructors.filter { !it.isPrimary }.takeIf { it.isNotEmpty() }?.let { secondaryConstructors ->
      builder.addFunctions(secondaryConstructors.map {
        it.toFunSpec(classTypeParamsResolver, it.annotations(jvmInternalName, elementHandler))
      })
    }
    val primaryConstructorParams = primaryConstructorSpec?.parameters.orEmpty().associateBy { it.name }
    builder.addProperties(
        properties
            .asSequence()
            .filter { it.isDeclaration }
            .filterNot { it.isSynthesized }
            .map {
              val annotations = LinkedHashSet<AnnotationSpec>()
              var constant: CodeBlock? = null
              var isOverride = false
              if (elementHandler != null) {
                if (it.hasAnnotations) {
                  annotations += it.syntheticMethodForAnnotations?.let {
                    elementHandler.methodAnnotations(jvmInternalName, it)
                  }.orEmpty()
                }
                it.fieldSignature?.let { fieldSignature ->
                  annotations += elementHandler.fieldAnnotations(jvmInternalName, fieldSignature)
                      .map { it.toBuilder().useSiteTarget(UseSiteTarget.FIELD).build() }
                  annotations += elementHandler.fieldJvmModifiers(jvmInternalName, fieldSignature)
                      .map { it.annotationSpec() }
                  if (isCompanionObject && parentName != null) {
                    // These are copied into the parent
                    annotations += elementHandler.fieldJvmModifiers(parentName, fieldSignature)
                        .map { it.annotationSpec() }
                  }
                  if (it.hasConstant) {
                    constant = if (isCompanionObject && parentName != null) {
                      // Constants are relocated to the enclosing class!
                      elementHandler.fieldConstant(parentName, fieldSignature)
                    } else {
                      elementHandler.fieldConstant(jvmInternalName, fieldSignature)
                    }
                  }
                }
                if (it.hasGetter) {
                  it.getterSignature?.let { getterSignature ->
                    if (!isOverride) {
                      isOverride = elementHandler.isMethodOverride(jvmInternalName, getterSignature)
                    }
                    if (it.getterFlags.hasAnnotations) {
                      annotations += elementHandler.methodAnnotations(jvmInternalName,
                          getterSignature)
                          .map { it.toBuilder().useSiteTarget(UseSiteTarget.GET).build() }
                    }
                    annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                        getterSignature)
                        .map {
                          it.annotationSpec().toBuilder().useSiteTarget(UseSiteTarget.GET).build()
                        }

                    if (isCompanionObject && parentName != null) {
                      // These are copied into the parent
                      annotations += elementHandler.methodJvmModifiers(jvmInternalName, getterSignature)
                          .map { it.annotationSpec() }
                    }
                  }
                }
                if (it.hasSetter) {
                  it.setterSignature?.let { setterSignature ->
                    if (!isOverride) {
                      isOverride = elementHandler.isMethodOverride(jvmInternalName, setterSignature)
                    }
                    if (it.setterFlags.hasAnnotations) {
                      annotations += elementHandler.methodAnnotations(jvmInternalName,
                          setterSignature)
                          .map { it.toBuilder().useSiteTarget(UseSiteTarget.SET).build() }
                    }
                    annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                        setterSignature)
                        .map {
                          it.annotationSpec().toBuilder().useSiteTarget(UseSiteTarget.SET).build()
                        }
                    if (isCompanionObject && parentName != null) {
                      // These are copied into the parent
                      annotations += elementHandler.methodJvmModifiers(jvmInternalName,
                          setterSignature)
                          .map { it.annotationSpec() }
                    }
                  }
                }
              }
              it.toPropertySpec(
                  typeParamResolver = classTypeParamsResolver,
                  isConstructorParam = it.name in primaryConstructorParams,
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
            if (elementHandler != null) {
              func.signature?.let { signature ->
                if (func.hasAnnotations) {
                  annotations += elementHandler.methodAnnotations(jvmInternalName, signature)
                }
                annotations += elementHandler.methodJvmModifiers(jvmInternalName, signature)
                    .map { it.annotationSpec() }
                isOverride = elementHandler.isMethodOverride(jvmInternalName, signature)
              }
            }
            func.toFunSpec(functionTypeParamsResolver, annotations, isOverride).let {
              // For interface methods, remove any body and mark the methods as abstracte
              // TODO kotlin interface methods _can_ be implemented. How do we detect that?
              if (isInterface && annotations.none { it.className == JVM_DEFAULT }) {
                it.toBuilder()
                    .addModifiers(ABSTRACT)
                    .clearBody()
                    .build()
              } else {
                it
              }
            }
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

@KotlinPoetKm
private fun ImmutableKmConstructor.annotations(
  classJvmName: String,
  elementHandler: ElementHandler?
): List<AnnotationSpec> {
  return if (elementHandler != null && hasAnnotations && signature != null) {
    elementHandler.constructorAnnotations(classJvmName, signature!!)
  } else {
    emptyList()
  }
}

private fun companionObjectName(name: String): String? {
  return if (name == "Companion") null else name
}

@KotlinPoetKm
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

@KotlinPoetKm
private fun ImmutableKmFunction.toFunSpec(
  typeParamResolver: ((index: Int) -> TypeName),
  annotations: Iterable<AnnotationSpec>,
  isOverride: Boolean
): FunSpec {
  // Only visisble from Elements API as Override is not available at runtime for reflection
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
        if (isSynthesized) {
          addAnnotation(JvmSynthetic::class)
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

@KotlinPoetKm
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

@KotlinPoetKm
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
        if (hasGetter && !isDelegated) {
          propertyAccessor(getterFlags, FunSpec.getterBuilder().addStatement(
              TODO_BLOCK), isOverride)?.let(
              ::getter)
        }
        if (hasSetter && !isDelegated) {
          propertyAccessor(setterFlags, FunSpec.setterBuilder(), isOverride)?.let(::setter)
        }
      }
      .tag(this)
      .build()
}

@KotlinPoetKm
private fun propertyAccessor(flags: Flags, functionBuilder: Builder, isOverride: Boolean): FunSpec? {
  val visibility = flags.visibility
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

@KotlinPoetKm
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

@KotlinPoetKm
private fun ImmutableKmWithFlags.addVisibility(body: (KModifier) -> Unit) {
  val modifierVisibility = flags.visibility
  if (modifierVisibility != PUBLIC) {
    body(modifierVisibility)
  }
}

@KotlinPoetKm
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

private val OVERRIDE = Override::class.asClassName()
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

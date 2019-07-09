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

import com.squareup.kotlinpoet.km.ImmutableKmClass
import com.squareup.kotlinpoet.km.ImmutableKmConstructor
import com.squareup.kotlinpoet.km.ImmutableKmFunction
import com.squareup.kotlinpoet.km.ImmutableKmProperty
import com.squareup.kotlinpoet.km.ImmutableKmValueParameter
import com.squareup.kotlinpoet.km.KotlinPoetKm
import com.squareup.kotlinpoet.km.PropertyAccessorFlag
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_EXTERNAL
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_INLINE
import com.squareup.kotlinpoet.km.PropertyAccessorFlag.IS_NOT_DEFAULT
import com.squareup.kotlinpoet.km.declaresDefaultValue
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
import com.squareup.kotlinpoet.km.isFakeOverride
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
import com.squareup.kotlinpoet.km.isOverride
import com.squareup.kotlinpoet.km.isOverrideProperty
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
import kotlinx.metadata.Flags
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun KClass<*>.toTypeSpec(): TypeSpec = java.toTypeSpec()
/** @return a [TypeSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun Class<*>.toTypeSpec(): TypeSpec = toImmutableKmClass().toTypeSpec()
/** @return a [TypeSpec] ABI representation of this [TypeElement]. */
@KotlinPoetKm
fun TypeElement.toTypeSpec(): TypeSpec = toImmutableKmClass().toTypeSpec()
/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun KClass<*>.toFileSpec(): FileSpec = java.toFileSpec()
/** @return a [FileSpec] ABI representation of this [KClass]. */
@KotlinPoetKm
fun Class<*>.toFileSpec(): FileSpec = FileSpec.get(`package`.name, toTypeSpec())
/** @return a [FileSpec] ABI representation of this [TypeElement]. */
@KotlinPoetKm
fun TypeElement.toFileSpec(): FileSpec = FileSpec.get(
    packageName = packageName.toString(),
    typeSpec = toTypeSpec()
)

private const val TODO_BLOCK = "TODO(\"Stub!\")"

@KotlinPoetKm
private fun ImmutableKmClass.toTypeSpec(): TypeSpec {
  // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined params
  val parametersMap = mutableMapOf<Int, TypeName>()
  val typeParamResolver = { id: Int -> parametersMap.getValue(id) }
  typeParameters.forEach { parametersMap[it.id] = it.toTypeVariableName(typeParamResolver) }

  val simpleName = name.substringAfterLast(".")
  val builder = when {
    isAnnotation -> TypeSpec.annotationBuilder(simpleName)
    isCompanionObject -> TypeSpec.companionObjectBuilder(simpleName)
    isEnum -> TypeSpec.enumBuilder(simpleName)
    isExpect -> TypeSpec.expectClassBuilder(simpleName)
    isObject -> TypeSpec.objectBuilder(simpleName)
    isInterface -> TypeSpec.interfaceBuilder(simpleName)
    else -> TypeSpec.classBuilder(simpleName)
  }
  builder.addModifiers(flags.visibility)
  builder.addModifiers(*flags.modalities
      .filterNot { it == KModifier.FINAL } // Default
      .toTypedArray()
  )
  if (isData) {
    builder.addModifiers(KModifier.DATA)
  }
  if (isExternal) {
    builder.addModifiers(KModifier.EXTERNAL)
  }
  if (isInline) {
    builder.addModifiers(KModifier.INLINE)
  }
  if (isInner) {
    builder.addModifiers(KModifier.INNER)
  }
  if (isEnumEntry) {
    // TODO handle typespec arg for complex enums
    enumEntries.forEach {
      builder.addEnumConstant(it)
    }
  }

  builder.addTypeVariables(typeParameters.map { it.toTypeVariableName(typeParamResolver) })
  supertypes.first().toTypeName(typeParamResolver).takeIf { it != ANY }?.let(builder::superclass)
  builder.addSuperinterfaces(supertypes.drop(1).map { it.toTypeName(typeParamResolver) })
  builder.addProperties(
      properties
          .asSequence()
          .filter { it.isDeclaration }
          .filterNot { it.isDelegated }
          .map { it.toPropertySpec(typeParamResolver) }
          .asIterable()
  )
  primaryConstructor?.takeIf { it.valueParameters.isNotEmpty() || flags.visibility != KModifier.PUBLIC }?.let {
    builder.primaryConstructor(it.toFunSpec(typeParamResolver))
  }
  constructors.filter { !it.isPrimary }.takeIf { it.isNotEmpty() }?.let { secondaryConstructors ->
    builder.addFunctions(secondaryConstructors.map { it.toFunSpec(typeParamResolver) })
  }
  companionObject?.let {
    builder.addType(TypeSpec.companionObjectBuilder(it).build())
  }
  builder.addFunctions(
      functions
          .asSequence()
          .filter { it.isDeclaration }
          .filterNot { it.isDelegation }
          .map { it.toFunSpec(typeParamResolver) }
          .asIterable()
  )

  return builder
      .tag(this)
      .build()
}

@KotlinPoetKm
private fun ImmutableKmConstructor.toFunSpec(
    typeParamResolver: ((index: Int) -> TypeName)
): FunSpec {
  return FunSpec.constructorBuilder()
      .apply {
        addModifiers(flags.visibility)
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
    typeParamResolver: ((index: Int) -> TypeName)
): FunSpec {
  return FunSpec.builder(name)
      .apply {
        addModifiers(flags.visibility)
        addParameters(this@toFunSpec.valueParameters.map { it.toParameterSpec(typeParamResolver) })
        if (isFakeOverride) {
          addModifiers(KModifier.OVERRIDE)
        }
        if (isSynthesized) {
          addAnnotation(JvmSynthetic::class)
        }
        if (isOperator) {
          addModifiers(KModifier.OPERATOR)
        }
        if (isInfix) {
          addModifiers(KModifier.INFIX)
        }
        if (isInline) {
          addModifiers(KModifier.INLINE)
        }
        if (isTailRec) {
          addModifiers(KModifier.TAILREC)
        }
        if (isExternal) {
          addModifiers(KModifier.EXTERNAL)
        }
        if (isExpect) {
          addModifiers(KModifier.EXPECT)
        }
        if (isSuspend) {
          addModifiers(KModifier.SUSPEND)
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
          addModifiers(KModifier.VARARG)
        }
        if (isCrossInline) {
          addModifiers(KModifier.CROSSINLINE)
        }
        if (isNoInline) {
          addModifiers(KModifier.NOINLINE)
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
    typeParamResolver: ((index: Int) -> TypeName)
) = PropertySpec.builder(name, returnType.toTypeName(typeParamResolver))
    .apply {
      addModifiers(flags.visibility)
      addModifiers(*flags.modalities
          .filterNot { it == KModifier.FINAL && !isOverride }
          .toTypedArray())
      if (isOverride) {
        addModifiers(KModifier.OVERRIDE)
      }
      if (isConst) {
        addModifiers(KModifier.CONST)
      }
      if (isVar) {
        mutable(true)
      } else if (isVal) {
        mutable(false)
      }
      if (isDelegated) {
        delegate("") // Placeholder
      }
      if (isExpect) {
        addModifiers(KModifier.EXPECT)
      }
      if (isExternal) {
        addModifiers(KModifier.EXTERNAL)
      }
      if (isLateinit) {
        addModifiers(KModifier.LATEINIT)
      }
      if (isSynthesized) {
        addAnnotation(JvmSynthetic::class)
      }
      if (!isDelegated && !isLateinit) {
        // TODO if hasConstant + elements, we could look up the constant initializer
        if (type.isNullable) {
          initializer("null")
        } else {
          initializer(TODO_BLOCK)
        }
      }
      if (hasGetter) {
        val visibility = setterFlags.visibility
        val modalities = setterFlags.modalities
            .filterNot { it == KModifier.FINAL && !flags.isOverrideProperty }
        val propertyAccessorFlags = setterFlags.propertyAccessorFlags
        if (visibility != KModifier.PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
          getter(FunSpec.setterBuilder()
              .apply {
                addModifiers(visibility)
                addModifiers(*modalities.toTypedArray())
                addModifiers(*propertyAccessorFlags.toKModifiersArray())
              }
              .build())
        }
      }
      if (hasSetter) {
        val visibility = setterFlags.visibility
        val modalities = setterFlags.modalities
            .filterNot { it == KModifier.FINAL && !flags.isOverrideProperty }
        val propertyAccessorFlags = setterFlags.propertyAccessorFlags
        if (visibility != KModifier.PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
          setter(FunSpec.setterBuilder()
              .apply {
                addModifiers(visibility)
                addModifiers(*modalities.toTypedArray())
                addModifiers(*propertyAccessorFlags.toKModifiersArray())
              }
              .build())
        }
      }
    }
    .tag(this)
    .build()

private fun Set<PropertyAccessorFlag>.toKModifiersArray(): Array<KModifier> {
  return map {
    when (it) {
      IS_EXTERNAL -> KModifier.EXTERNAL
      IS_INLINE -> KModifier.INLINE
      IS_NOT_DEFAULT -> TODO("Wat")
    }
  }.toTypedArray()
}

@KotlinPoetKm
private val Flags.visibility: KModifier
  get() = when {
    isInternal -> KModifier.INTERNAL
    isPrivate -> KModifier.PRIVATE
    isProtected -> KModifier.PROTECTED
    isPublic -> KModifier.PUBLIC
    else -> {
      // IS_PRIVATE_TO_THIS or IS_LOCAL, so just default to public
      KModifier.PUBLIC
    }
  }

@KotlinPoetKm
private val Flags.modalities: Set<KModifier>
  get() = setOf {
    if (isFinal) {
      add(KModifier.FINAL)
    }
    if (isOpen) {
      add(KModifier.OPEN)
    }
    if (isAbstract) {
      add(KModifier.ABSTRACT)
    }
    if (isSealed) {
      add(KModifier.SEALED)
    }
  }

private inline fun <E> setOf(body: MutableSet<E>.() -> Unit): Set<E> {
  return mutableSetOf<E>().apply(body).toSet()
}

@PublishedApi
internal val Element.packageName: PackageElement
  get() {
      var element = this
      while (element.kind != ElementKind.PACKAGE) {
        element = element.enclosingElement
      }
      return element as PackageElement
  }

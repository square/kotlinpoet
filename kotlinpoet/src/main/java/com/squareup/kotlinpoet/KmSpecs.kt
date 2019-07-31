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
import com.squareup.kotlinpoet.km.ImmutableKmWithFlags
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

  val simpleName = name.substringAfterLast(if (isInline) "/" else ".")
  val builder = when {
    isAnnotation -> TypeSpec.annotationBuilder(simpleName)
    isCompanionObject -> TypeSpec.companionObjectBuilder(companionObjectName(simpleName))
    isEnum -> TypeSpec.enumBuilder(simpleName)
    isExpect -> TypeSpec.expectClassBuilder(simpleName)
    isObject -> TypeSpec.objectBuilder(simpleName)
    isInterface -> TypeSpec.interfaceBuilder(simpleName)
    else -> TypeSpec.classBuilder(simpleName)
  }
  addVisibility { builder.addModifiers(it) }
  builder.addModifiers(flags.modalities
      .filterNot { it == KModifier.FINAL } // Default
      .filterNot { isInterface && it == KModifier.ABSTRACT } // Abstract is a default on interfaces
  )
  if (isData) {
    builder.addModifiers(KModifier.DATA)
  }
  if (isExternal) {
    builder.addModifiers(KModifier.EXTERNAL)
  }
  if (isInline) {
    builder.addModifiers(KModifier.INLINE)
    // TODO these are special.
    //  - Name is the fqcn
  }
  if (isInner) {
    builder.addModifiers(KModifier.INNER)
  }
  if (isEnumEntry) {
    // TODO
  }
  if (isEnum) {
    // TODO handle typespec arg for complex enums
    enumEntries.forEach {
      builder.addEnumConstant(it)
    }
  }

  builder.addTypeVariables(typeParameters.map { it.toTypeVariableName(typeParamResolver) })
  if (!isEnum && !isInterface) {
    supertypes.first().toTypeName(typeParamResolver).takeIf { it != ANY }?.let(builder::superclass)
  }
  builder.addSuperinterfaces(supertypes.drop(if (isInterface) 0 else 1).map { it.toTypeName(typeParamResolver) })
  val primaryConstructorSpec = primaryConstructor?.takeIf { it.valueParameters.isNotEmpty() || flags.visibility != KModifier.PUBLIC }?.let {
    it.toFunSpec(typeParamResolver).also {
      builder.primaryConstructor(it)
    }
  }
  constructors.filter { !it.isPrimary }.takeIf { it.isNotEmpty() }?.let { secondaryConstructors ->
    builder.addFunctions(secondaryConstructors.map { it.toFunSpec(typeParamResolver) })
  }
  val primaryConstructorParams = primaryConstructorSpec?.parameters.orEmpty().associateBy { it.name }
  builder.addProperties(
      properties
          .asSequence()
          .filter { it.isDeclaration }
          .filterNot { it.isSynthesized }
          .map { it.toPropertySpec(typeParamResolver, it.name in primaryConstructorParams) }
          .asIterable()
  )
  companionObject?.let {
    builder.addType(TypeSpec.companionObjectBuilder(companionObjectName(it)).build())
  }
  builder.addFunctions(
      functions
          .asSequence()
          .filter { it.isDeclaration }
          .filterNot { it.isDelegation }
          .filterNot { it.isSynthesized }
          .map { it.toFunSpec(typeParamResolver) }
          .asIterable()
  )

  return builder
      .tag(this)
      .build()
}

private fun companionObjectName(name: String): String? {
  return if (name == "Companion") null else name
}

@KotlinPoetKm
private fun ImmutableKmConstructor.toFunSpec(
  typeParamResolver: ((index: Int) -> TypeName)
): FunSpec {
  return FunSpec.constructorBuilder()
      .apply {
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
  typeParamResolver: ((index: Int) -> TypeName)
): FunSpec {
  return FunSpec.builder(name)
      .apply {
        addVisibility { addModifiers(it) }
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
  typeParamResolver: ((index: Int) -> TypeName),
  isConstructorParam: Boolean
) = PropertySpec.builder(name, returnType.toTypeName(typeParamResolver))
    .apply {
      addVisibility { addModifiers(it) }
      addModifiers(flags.modalities
          .filterNot { it == KModifier.FINAL && !isOverride } // Final is the default
          .filterNot { it == KModifier.OPEN && isOverride } // Overrides are implicitly open
      )
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
        // Placeholders for these are tricky
        addKdoc("Note: delegation is ABI stub only and not guaranteed to match source code.")
        if (isVal) {
          delegate("%M { %L }", MemberName("kotlin", "lazy"), TODO_BLOCK) // Placeholder
        } else {
          if (type.isNullable) {
            delegate("%T.observable(null) { _, _, _ -> }", ClassName("kotlin.properties", "Delegates"))
          } else {
            delegate("%T.notNull()", ClassName("kotlin.properties", "Delegates")) // Placeholder
          }
        }
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
      if (isConstructorParam || (!isDelegated && !isLateinit)) {
        // TODO if hasConstant + elements, we could look up the constant initializer
        when {
          isConstructorParam -> initializer(name)
          type.isNullable -> initializer("null")
          else -> initializer(TODO_BLOCK)
        }
      }
      // Delegated properties have setters/getters defined for some reason, ignore here
      // since the delegate handles it
      if (hasGetter && !isDelegated) {
        propertyAccessor(getterFlags, FunSpec.getterBuilder())?.let(::getter)
      }
      if (hasSetter && !isDelegated) {
        propertyAccessor(setterFlags, FunSpec.setterBuilder())?.let(::setter)
      }
    }
    .tag(this)
    .build()

@KotlinPoetKm
private fun propertyAccessor(flags: Flags, functionBuilder: FunSpec.Builder): FunSpec? {
  val visibility = flags.visibility
  val modalities = flags.modalities
      .filterNot { it == KModifier.FINAL && !flags.isOverrideProperty }
  val propertyAccessorFlags = flags.propertyAccessorFlags
  return if (visibility != KModifier.PUBLIC || modalities.isNotEmpty() || propertyAccessorFlags.isNotEmpty()) {
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
      IS_EXTERNAL -> KModifier.EXTERNAL
      IS_INLINE -> KModifier.INLINE
      IS_NOT_DEFAULT -> null // Gracefully skip over these
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
private fun ImmutableKmWithFlags.addVisibility(body: (KModifier) -> Unit) {
  val modifierVisibility = flags.visibility
  if (modifierVisibility != KModifier.PUBLIC) {
    body(modifierVisibility)
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

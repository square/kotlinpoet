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
@file:Suppress("unused")

package com.squareup.kotlinpoet.metadata

import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmConstructor
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.KmTypeParameter
import kotlinx.metadata.KmValueParameter

// Common flags for any element with flags.
@KotlinPoetMetadata
val Flags.hasAnnotations: Boolean get() = Flag.HAS_ANNOTATIONS(this)
@KotlinPoetMetadata
val Flags.isAbstract: Boolean get() = Flag.IS_ABSTRACT(this)
@KotlinPoetMetadata
val Flags.isFinal: Boolean get() = Flag.IS_FINAL(this)
@KotlinPoetMetadata
val Flags.isInternal: Boolean get() = Flag.IS_INTERNAL(this)
@KotlinPoetMetadata
val Flags.isLocal: Boolean get() = Flag.IS_LOCAL(this)
@KotlinPoetMetadata
val Flags.isOpen: Boolean get() = Flag.IS_OPEN(this)
@KotlinPoetMetadata
val Flags.isPrivate: Boolean get() = Flag.IS_PRIVATE(this)
@KotlinPoetMetadata
val Flags.isPrivate_to_this: Boolean get() = Flag.IS_PRIVATE_TO_THIS(this)
@KotlinPoetMetadata
val Flags.isProtected: Boolean get() = Flag.IS_PROTECTED(this)
@KotlinPoetMetadata
val Flags.isPublic: Boolean get() = Flag.IS_PUBLIC(this)
@KotlinPoetMetadata
val Flags.isSealed: Boolean get() = Flag.IS_SEALED(this)
@KotlinPoetMetadata
val ImmutableKmWithFlags.hasAnnotations: Boolean get() = flags.hasAnnotations
@KotlinPoetMetadata
val ImmutableKmWithFlags.isAbstract: Boolean get() = flags.isAbstract
@KotlinPoetMetadata
val ImmutableKmWithFlags.isFinal: Boolean get() = flags.isFinal
@KotlinPoetMetadata
val ImmutableKmWithFlags.isInternal: Boolean get() = flags.isInternal
@KotlinPoetMetadata
val ImmutableKmWithFlags.isLocal: Boolean get() = flags.isLocal
@KotlinPoetMetadata
val ImmutableKmWithFlags.isOpen: Boolean get() = flags.isOpen
@KotlinPoetMetadata
val ImmutableKmWithFlags.isPrivate: Boolean get() = flags.isPrivate
@KotlinPoetMetadata
val ImmutableKmWithFlags.isPrivate_to_this: Boolean get() = flags.isPrivate_to_this
@KotlinPoetMetadata
val ImmutableKmWithFlags.isProtected: Boolean get() = flags.isProtected
@KotlinPoetMetadata
val ImmutableKmWithFlags.isPublic: Boolean get() = flags.isPublic
@KotlinPoetMetadata
val ImmutableKmWithFlags.isSealed: Boolean get() = flags.isSealed

// Type flags.
@KotlinPoetMetadata
val Flags.isNullableType: Boolean get() = Flag.Type.IS_NULLABLE(this)
@KotlinPoetMetadata
val Flags.isSuspendType: Boolean get() = Flag.Type.IS_SUSPEND(this)

// Class flags.
@KotlinPoetMetadata
val Flags.isAnnotationClass: Boolean get() = Flag.Class.IS_ANNOTATION_CLASS(this)
@KotlinPoetMetadata
val Flags.isClass: Boolean get() = Flag.Class.IS_CLASS(this)
@KotlinPoetMetadata
val Flags.isCompanionObjectClass: Boolean get() = Flag.Class.IS_COMPANION_OBJECT(this)
@KotlinPoetMetadata
val Flags.isDataClass: Boolean get() = Flag.Class.IS_DATA(this)
@KotlinPoetMetadata
val Flags.isEnumClass: Boolean get() = Flag.Class.IS_ENUM_CLASS(this)
@KotlinPoetMetadata
val Flags.isEnumEntryClass: Boolean get() = Flag.Class.IS_ENUM_ENTRY(this)
@KotlinPoetMetadata
val Flags.isExpectClass: Boolean get() = Flag.Class.IS_EXPECT(this)
@KotlinPoetMetadata
val Flags.isExternalClass: Boolean get() = Flag.Class.IS_EXTERNAL(this)
@KotlinPoetMetadata
val Flags.isInlineClass: Boolean get() = Flag.Class.IS_INLINE(this)
@KotlinPoetMetadata
val Flags.isInnerClass: Boolean get() = Flag.Class.IS_INNER(this)
@KotlinPoetMetadata
val Flags.isObjectClass: Boolean get() = Flag.Class.IS_OBJECT(this)
@KotlinPoetMetadata
val Flags.isInterface: Boolean get() = Flag.Class.IS_INTERFACE(this)
@KotlinPoetMetadata
val KmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadata
val KmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadata
val KmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadata
val KmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadata
val KmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadata
val KmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadata
val KmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadata
val KmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadata
val KmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadata
val KmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadata
val KmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadata
val KmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadata
val KmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadata
val KmType.isNullable: Boolean get() = flags.isNullableType
@KotlinPoetMetadata
val ImmutableKmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadata
val ImmutableKmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadata
val ImmutableKmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadata
val ImmutableKmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadata
val ImmutableKmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadata
val ImmutableKmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadata
val ImmutableKmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadata
val ImmutableKmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadata
val ImmutableKmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadata
val ImmutableKmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadata
val ImmutableKmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadata
val ImmutableKmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadata
val ImmutableKmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadata
val ImmutableKmType.isNullable: Boolean get() = flags.isNullableType

// Constructor flags.
@KotlinPoetMetadata
val Flags.isPrimaryConstructor: Boolean get() = Flag.Constructor.IS_PRIMARY(this)
@KotlinPoetMetadata
val KmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadata
val KmConstructor.isSecondary: Boolean get() = !isPrimary
@KotlinPoetMetadata
val ImmutableKmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadata
val ImmutableKmConstructor.isSecondary: Boolean get() = !isPrimary

// Function flags.
@KotlinPoetMetadata
val Flags.isDeclarationFunction: Boolean get() = Flag.Function.IS_DECLARATION(this)
@KotlinPoetMetadata
val Flags.isFakeOverrideFunction: Boolean get() = Flag.Function.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadata
val Flags.isDelegationFunction: Boolean get() = Flag.Function.IS_DELEGATION(this)
@KotlinPoetMetadata
val Flags.isSynthesizedFunction: Boolean get() = Flag.Function.IS_SYNTHESIZED(this)
@KotlinPoetMetadata
val Flags.isOperatorFunction: Boolean get() = Flag.Function.IS_OPERATOR(this)
@KotlinPoetMetadata
val Flags.isInfixFunction: Boolean get() = Flag.Function.IS_INFIX(this)
@KotlinPoetMetadata
val Flags.isInlineFunction: Boolean get() = Flag.Function.IS_INLINE(this)
@KotlinPoetMetadata
val Flags.isTailRecFunction: Boolean get() = Flag.Function.IS_TAILREC(this)
@KotlinPoetMetadata
val Flags.isExternalFunction: Boolean get() = Flag.Function.IS_EXTERNAL(this)
@KotlinPoetMetadata
val Flags.isSuspendFunction: Boolean get() = Flag.Function.IS_SUSPEND(this)
@KotlinPoetMetadata
val Flags.isExpectFunction: Boolean get() = Flag.Function.IS_EXPECT(this)
@KotlinPoetMetadata
val KmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadata
val KmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadata
val KmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadata
val KmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadata
val KmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadata
val KmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadata
val KmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadata
val KmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadata
val KmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadata
val KmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadata
val KmFunction.isExpect: Boolean get() = flags.isExpectFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadata
val ImmutableKmFunction.isExpect: Boolean get() = flags.isExpectFunction

// Parameter flags.
@KotlinPoetMetadata
val KmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadata
val KmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadata
val KmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)
@KotlinPoetMetadata
val ImmutableKmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadata
val ImmutableKmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadata
val ImmutableKmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)

// Property flags.
@KotlinPoetMetadata
val Flags.isFakeOverrideProperty: Boolean get() = Flag.Property.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadata
val KmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadata
val KmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadata
val KmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadata
val KmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadata
val KmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadata
val KmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadata
val KmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadata
val KmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadata
val KmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadata
val KmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadata
val KmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadata
val KmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadata
val KmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadata
val KmProperty.isVal: Boolean get() = !isVar
@KotlinPoetMetadata
val ImmutableKmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadata
val ImmutableKmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadata
val ImmutableKmProperty.isVal: Boolean get() = !isVar

// Property Accessor Flags
@KotlinPoetMetadata
val Flags.isPropertyAccessorExternal: Boolean
  get() = Flag.PropertyAccessor.IS_EXTERNAL(this)
@KotlinPoetMetadata
val Flags.isPropertyAccessorInline: Boolean get() = Flag.PropertyAccessor.IS_INLINE(this)
@KotlinPoetMetadata
val Flags.isPropertyAccessorNotDefault: Boolean
  get() = Flag.PropertyAccessor.IS_NOT_DEFAULT(this)

// TypeParameter flags.
@KotlinPoetMetadata
val KmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)
@KotlinPoetMetadata
val ImmutableKmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)

// Property Accessor Flags
enum class PropertyAccessorFlag {
  IS_EXTERNAL, IS_INLINE, IS_NOT_DEFAULT
}

@KotlinPoetMetadata
val ImmutableKmProperty.setterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = setterFlags.propertyAccessorFlags

@KotlinPoetMetadata
val ImmutableKmProperty.getterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = getterFlags.propertyAccessorFlags

@KotlinPoetMetadata
val Flags.propertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = setOf {
    if (Flag.PropertyAccessor.IS_EXTERNAL(this@propertyAccessorFlags)) {
      add(PropertyAccessorFlag.IS_EXTERNAL)
    }
    if (Flag.PropertyAccessor.IS_INLINE(this@propertyAccessorFlags)) {
      add(PropertyAccessorFlag.IS_INLINE)
    }
    if (Flag.PropertyAccessor.IS_NOT_DEFAULT(this@propertyAccessorFlags)) {
      add(PropertyAccessorFlag.IS_NOT_DEFAULT)
    }
  }

internal inline fun <E> setOf(body: MutableSet<E>.() -> Unit): Set<E> {
  return mutableSetOf<E>().apply(body).toSet()
}

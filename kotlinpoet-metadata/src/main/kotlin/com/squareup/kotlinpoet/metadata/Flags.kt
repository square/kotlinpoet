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
@KotlinPoetKm
val Flags.hasAnnotations: Boolean get() = Flag.HAS_ANNOTATIONS(this)
@KotlinPoetKm
val Flags.isAbstract: Boolean get() = Flag.IS_ABSTRACT(this)
@KotlinPoetKm
val Flags.isFinal: Boolean get() = Flag.IS_FINAL(this)
@KotlinPoetKm
val Flags.isInternal: Boolean get() = Flag.IS_INTERNAL(this)
@KotlinPoetKm
val Flags.isLocal: Boolean get() = Flag.IS_LOCAL(this)
@KotlinPoetKm
val Flags.isOpen: Boolean get() = Flag.IS_OPEN(this)
@KotlinPoetKm
val Flags.isPrivate: Boolean get() = Flag.IS_PRIVATE(this)
@KotlinPoetKm
val Flags.isPrivate_to_this: Boolean get() = Flag.IS_PRIVATE_TO_THIS(this)
@KotlinPoetKm
val Flags.isProtected: Boolean get() = Flag.IS_PROTECTED(this)
@KotlinPoetKm
val Flags.isPublic: Boolean get() = Flag.IS_PUBLIC(this)
@KotlinPoetKm
val Flags.isSealed: Boolean get() = Flag.IS_SEALED(this)
@KotlinPoetKm
val ImmutableKmWithFlags.hasAnnotations: Boolean get() = flags.hasAnnotations
@KotlinPoetKm
val ImmutableKmWithFlags.isAbstract: Boolean get() = flags.isAbstract
@KotlinPoetKm
val ImmutableKmWithFlags.isFinal: Boolean get() = flags.isFinal
@KotlinPoetKm
val ImmutableKmWithFlags.isInternal: Boolean get() = flags.isInternal
@KotlinPoetKm
val ImmutableKmWithFlags.isLocal: Boolean get() = flags.isLocal
@KotlinPoetKm
val ImmutableKmWithFlags.isOpen: Boolean get() = flags.isOpen
@KotlinPoetKm
val ImmutableKmWithFlags.isPrivate: Boolean get() = flags.isPrivate
@KotlinPoetKm
val ImmutableKmWithFlags.isPrivate_to_this: Boolean get() = flags.isPrivate_to_this
@KotlinPoetKm
val ImmutableKmWithFlags.isProtected: Boolean get() = flags.isProtected
@KotlinPoetKm
val ImmutableKmWithFlags.isPublic: Boolean get() = flags.isPublic
@KotlinPoetKm
val ImmutableKmWithFlags.isSealed: Boolean get() = flags.isSealed

// Type flags.
@KotlinPoetKm
val Flags.isNullableType: Boolean get() = Flag.Type.IS_NULLABLE(this)
@KotlinPoetKm
val Flags.isSuspendType: Boolean get() = Flag.Type.IS_SUSPEND(this)

// Class flags.
@KotlinPoetKm
val Flags.isAnnotationClass: Boolean get() = Flag.Class.IS_ANNOTATION_CLASS(this)
@KotlinPoetKm
val Flags.isClass: Boolean get() = Flag.Class.IS_CLASS(this)
@KotlinPoetKm
val Flags.isCompanionObjectClass: Boolean get() = Flag.Class.IS_COMPANION_OBJECT(this)
@KotlinPoetKm
val Flags.isDataClass: Boolean get() = Flag.Class.IS_DATA(this)
@KotlinPoetKm
val Flags.isEnumClass: Boolean get() = Flag.Class.IS_ENUM_CLASS(this)
@KotlinPoetKm
val Flags.isEnumEntryClass: Boolean get() = Flag.Class.IS_ENUM_ENTRY(this)
@KotlinPoetKm
val Flags.isExpectClass: Boolean get() = Flag.Class.IS_EXPECT(this)
@KotlinPoetKm
val Flags.isExternalClass: Boolean get() = Flag.Class.IS_EXTERNAL(this)
@KotlinPoetKm
val Flags.isInlineClass: Boolean get() = Flag.Class.IS_INLINE(this)
@KotlinPoetKm
val Flags.isInnerClass: Boolean get() = Flag.Class.IS_INNER(this)
@KotlinPoetKm
val Flags.isObjectClass: Boolean get() = Flag.Class.IS_OBJECT(this)
@KotlinPoetKm
val Flags.isInterface: Boolean get() = Flag.Class.IS_INTERFACE(this)
@KotlinPoetKm
val KmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetKm
val KmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetKm
val KmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetKm
val KmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetKm
val KmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetKm
val KmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetKm
val KmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetKm
val KmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetKm
val KmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetKm
val KmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetKm
val KmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetKm
val KmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetKm
val KmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetKm
val KmType.isNullable: Boolean get() = flags.isNullableType
@KotlinPoetKm
val ImmutableKmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetKm
val ImmutableKmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetKm
val ImmutableKmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetKm
val ImmutableKmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetKm
val ImmutableKmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetKm
val ImmutableKmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetKm
val ImmutableKmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetKm
val ImmutableKmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetKm
val ImmutableKmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetKm
val ImmutableKmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetKm
val ImmutableKmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetKm
val ImmutableKmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetKm
val ImmutableKmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetKm
val ImmutableKmType.isNullable: Boolean get() = flags.isNullableType

// Constructor flags.
@KotlinPoetKm
val Flags.isPrimaryConstructor: Boolean get() = Flag.Constructor.IS_PRIMARY(this)
@KotlinPoetKm
val KmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetKm
val KmConstructor.isSecondary: Boolean get() = !isPrimary
@KotlinPoetKm
val ImmutableKmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetKm
val ImmutableKmConstructor.isSecondary: Boolean get() = !isPrimary

// Function flags.
@KotlinPoetKm
val Flags.isDeclarationFunction: Boolean get() = Flag.Function.IS_DECLARATION(this)
@KotlinPoetKm
val Flags.isFakeOverrideFunction: Boolean get() = Flag.Function.IS_FAKE_OVERRIDE(this)
@KotlinPoetKm
val Flags.isDelegationFunction: Boolean get() = Flag.Function.IS_DELEGATION(this)
@KotlinPoetKm
val Flags.isSynthesizedFunction: Boolean get() = Flag.Function.IS_SYNTHESIZED(this)
@KotlinPoetKm
val Flags.isOperatorFunction: Boolean get() = Flag.Function.IS_OPERATOR(this)
@KotlinPoetKm
val Flags.isInfixFunction: Boolean get() = Flag.Function.IS_INFIX(this)
@KotlinPoetKm
val Flags.isInlineFunction: Boolean get() = Flag.Function.IS_INLINE(this)
@KotlinPoetKm
val Flags.isTailRecFunction: Boolean get() = Flag.Function.IS_TAILREC(this)
@KotlinPoetKm
val Flags.isExternalFunction: Boolean get() = Flag.Function.IS_EXTERNAL(this)
@KotlinPoetKm
val Flags.isSuspendFunction: Boolean get() = Flag.Function.IS_SUSPEND(this)
@KotlinPoetKm
val Flags.isExpectFunction: Boolean get() = Flag.Function.IS_EXPECT(this)
@KotlinPoetKm
val KmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetKm
val KmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetKm
val KmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetKm
val KmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetKm
val KmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetKm
val KmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetKm
val KmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetKm
val KmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetKm
val KmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetKm
val KmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetKm
val KmFunction.isExpect: Boolean get() = flags.isExpectFunction
@KotlinPoetKm
val ImmutableKmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetKm
val ImmutableKmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetKm
val ImmutableKmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetKm
val ImmutableKmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetKm
val ImmutableKmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetKm
val ImmutableKmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetKm
val ImmutableKmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetKm
val ImmutableKmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetKm
val ImmutableKmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetKm
val ImmutableKmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetKm
val ImmutableKmFunction.isExpect: Boolean get() = flags.isExpectFunction

// Parameter flags.
@KotlinPoetKm
val KmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetKm
val KmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetKm
val KmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)
@KotlinPoetKm
val ImmutableKmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetKm
val ImmutableKmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetKm
val ImmutableKmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)

// Property flags.
@KotlinPoetKm
val Flags.isFakeOverrideProperty: Boolean get() = Flag.Property.IS_FAKE_OVERRIDE(this)
@KotlinPoetKm
val KmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetKm
val KmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetKm
val KmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetKm
val KmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetKm
val KmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetKm
val KmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetKm
val KmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetKm
val KmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetKm
val KmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetKm
val KmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetKm
val KmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetKm
val KmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetKm
val KmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetKm
val KmProperty.isVal: Boolean get() = !isVar
@KotlinPoetKm
val ImmutableKmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetKm
val ImmutableKmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetKm
val ImmutableKmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetKm
val ImmutableKmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetKm
val ImmutableKmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetKm
val ImmutableKmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetKm
val ImmutableKmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetKm
val ImmutableKmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetKm
val ImmutableKmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetKm
val ImmutableKmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetKm
val ImmutableKmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetKm
val ImmutableKmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetKm
val ImmutableKmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetKm
val ImmutableKmProperty.isVal: Boolean get() = !isVar

// Property Accessor Flags
@KotlinPoetKm
val Flags.isPropertyAccessorExternal: Boolean
  get() = Flag.PropertyAccessor.IS_EXTERNAL(this)
@KotlinPoetKm
val Flags.isPropertyAccessorInline: Boolean get() = Flag.PropertyAccessor.IS_INLINE(this)
@KotlinPoetKm
val Flags.isPropertyAccessorNotDefault: Boolean
  get() = Flag.PropertyAccessor.IS_NOT_DEFAULT(this)

// TypeParameter flags.
@KotlinPoetKm
val KmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)
@KotlinPoetKm
val ImmutableKmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)

// Property Accessor Flags
enum class PropertyAccessorFlag {
  IS_EXTERNAL, IS_INLINE, IS_NOT_DEFAULT
}

@KotlinPoetKm
val ImmutableKmProperty.setterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = setterFlags.propertyAccessorFlags

@KotlinPoetKm
val ImmutableKmProperty.getterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = getterFlags.propertyAccessorFlags

@KotlinPoetKm
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

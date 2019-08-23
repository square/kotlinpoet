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
@KotlinPoetMetadataPreview
val Flags.hasAnnotations: Boolean get() = Flag.HAS_ANNOTATIONS(this)
@KotlinPoetMetadataPreview
val Flags.isAbstract: Boolean get() = Flag.IS_ABSTRACT(this)
@KotlinPoetMetadataPreview
val Flags.isFinal: Boolean get() = Flag.IS_FINAL(this)
@KotlinPoetMetadataPreview
val Flags.isInternal: Boolean get() = Flag.IS_INTERNAL(this)
@KotlinPoetMetadataPreview
val Flags.isLocal: Boolean get() = Flag.IS_LOCAL(this)
@KotlinPoetMetadataPreview
val Flags.isOpen: Boolean get() = Flag.IS_OPEN(this)
@KotlinPoetMetadataPreview
val Flags.isPrivate: Boolean get() = Flag.IS_PRIVATE(this)
@KotlinPoetMetadataPreview
val Flags.isPrivate_to_this: Boolean get() = Flag.IS_PRIVATE_TO_THIS(this)
@KotlinPoetMetadataPreview
val Flags.isProtected: Boolean get() = Flag.IS_PROTECTED(this)
@KotlinPoetMetadataPreview
val Flags.isPublic: Boolean get() = Flag.IS_PUBLIC(this)
@KotlinPoetMetadataPreview
val Flags.isSealed: Boolean get() = Flag.IS_SEALED(this)
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.hasAnnotations: Boolean get() = flags.hasAnnotations
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isAbstract: Boolean get() = flags.isAbstract
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isFinal: Boolean get() = flags.isFinal
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isInternal: Boolean get() = flags.isInternal
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isLocal: Boolean get() = flags.isLocal
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isOpen: Boolean get() = flags.isOpen
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isPrivate: Boolean get() = flags.isPrivate
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isPrivate_to_this: Boolean get() = flags.isPrivate_to_this
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isProtected: Boolean get() = flags.isProtected
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isPublic: Boolean get() = flags.isPublic
@KotlinPoetMetadataPreview
val ImmutableKmWithFlags.isSealed: Boolean get() = flags.isSealed

// Type flags.
@KotlinPoetMetadataPreview
val Flags.isNullableType: Boolean get() = Flag.Type.IS_NULLABLE(this)
@KotlinPoetMetadataPreview
val Flags.isSuspendType: Boolean get() = Flag.Type.IS_SUSPEND(this)

// Class flags.
@KotlinPoetMetadataPreview
val Flags.isAnnotationClass: Boolean get() = Flag.Class.IS_ANNOTATION_CLASS(this)
@KotlinPoetMetadataPreview
val Flags.isClass: Boolean get() = Flag.Class.IS_CLASS(this)
@KotlinPoetMetadataPreview
val Flags.isCompanionObjectClass: Boolean get() = Flag.Class.IS_COMPANION_OBJECT(this)
@KotlinPoetMetadataPreview
val Flags.isDataClass: Boolean get() = Flag.Class.IS_DATA(this)
@KotlinPoetMetadataPreview
val Flags.isEnumClass: Boolean get() = Flag.Class.IS_ENUM_CLASS(this)
@KotlinPoetMetadataPreview
val Flags.isEnumEntryClass: Boolean get() = Flag.Class.IS_ENUM_ENTRY(this)
@KotlinPoetMetadataPreview
val Flags.isExpectClass: Boolean get() = Flag.Class.IS_EXPECT(this)
@KotlinPoetMetadataPreview
val Flags.isExternalClass: Boolean get() = Flag.Class.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
val Flags.isInlineClass: Boolean get() = Flag.Class.IS_INLINE(this)
@KotlinPoetMetadataPreview
val Flags.isInnerClass: Boolean get() = Flag.Class.IS_INNER(this)
@KotlinPoetMetadataPreview
val Flags.isObjectClass: Boolean get() = Flag.Class.IS_OBJECT(this)
@KotlinPoetMetadataPreview
val Flags.isInterface: Boolean get() = Flag.Class.IS_INTERFACE(this)
@KotlinPoetMetadataPreview
val KmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadataPreview
val KmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadataPreview
val KmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadataPreview
val KmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadataPreview
val KmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadataPreview
val KmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadataPreview
val KmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadataPreview
val KmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadataPreview
val KmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadataPreview
val KmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadataPreview
val KmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadataPreview
val KmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadataPreview
val KmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadataPreview
val KmType.isNullable: Boolean get() = flags.isNullableType
@KotlinPoetMetadataPreview
val ImmutableKmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadataPreview
val ImmutableKmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadataPreview
val ImmutableKmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadataPreview
val ImmutableKmType.isNullable: Boolean get() = flags.isNullableType

// Constructor flags.
@KotlinPoetMetadataPreview
val Flags.isPrimaryConstructor: Boolean get() = Flag.Constructor.IS_PRIMARY(this)
@KotlinPoetMetadataPreview
val KmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadataPreview
val KmConstructor.isSecondary: Boolean get() = !isPrimary
@KotlinPoetMetadataPreview
val ImmutableKmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadataPreview
val ImmutableKmConstructor.isSecondary: Boolean get() = !isPrimary

// Function flags.
@KotlinPoetMetadataPreview
val Flags.isDeclarationFunction: Boolean get() = Flag.Function.IS_DECLARATION(this)
@KotlinPoetMetadataPreview
val Flags.isFakeOverrideFunction: Boolean get() = Flag.Function.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadataPreview
val Flags.isDelegationFunction: Boolean get() = Flag.Function.IS_DELEGATION(this)
@KotlinPoetMetadataPreview
val Flags.isSynthesizedFunction: Boolean get() = Flag.Function.IS_SYNTHESIZED(this)
@KotlinPoetMetadataPreview
val Flags.isOperatorFunction: Boolean get() = Flag.Function.IS_OPERATOR(this)
@KotlinPoetMetadataPreview
val Flags.isInfixFunction: Boolean get() = Flag.Function.IS_INFIX(this)
@KotlinPoetMetadataPreview
val Flags.isInlineFunction: Boolean get() = Flag.Function.IS_INLINE(this)
@KotlinPoetMetadataPreview
val Flags.isTailRecFunction: Boolean get() = Flag.Function.IS_TAILREC(this)
@KotlinPoetMetadataPreview
val Flags.isExternalFunction: Boolean get() = Flag.Function.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
val Flags.isSuspendFunction: Boolean get() = Flag.Function.IS_SUSPEND(this)
@KotlinPoetMetadataPreview
val Flags.isExpectFunction: Boolean get() = Flag.Function.IS_EXPECT(this)
@KotlinPoetMetadataPreview
val KmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadataPreview
val KmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadataPreview
val KmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadataPreview
val KmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadataPreview
val KmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadataPreview
val KmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadataPreview
val KmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadataPreview
val KmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadataPreview
val KmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadataPreview
val KmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadataPreview
val KmFunction.isExpect: Boolean get() = flags.isExpectFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadataPreview
val ImmutableKmFunction.isExpect: Boolean get() = flags.isExpectFunction

// Parameter flags.
@KotlinPoetMetadataPreview
val KmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadataPreview
val KmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadataPreview
val KmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)
@KotlinPoetMetadataPreview
val ImmutableKmValueParameter.declaresDefaultValue: Boolean get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadataPreview
val ImmutableKmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadataPreview
val ImmutableKmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)

// Property flags.
@KotlinPoetMetadataPreview
val Flags.isFakeOverrideProperty: Boolean get() = Flag.Property.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadataPreview
val KmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadataPreview
val KmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadataPreview
val KmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadataPreview
val KmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadataPreview
val KmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadataPreview
val KmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadataPreview
val KmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadataPreview
val KmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadataPreview
val KmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadataPreview
val KmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadataPreview
val KmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadataPreview
val KmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadataPreview
val KmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadataPreview
val KmProperty.isVal: Boolean get() = !isVar
@KotlinPoetMetadataPreview
val ImmutableKmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadataPreview
val ImmutableKmProperty.isVal: Boolean get() = !isVar

// Property Accessor Flags
@KotlinPoetMetadataPreview
val Flags.isPropertyAccessorExternal: Boolean
  get() = Flag.PropertyAccessor.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
val Flags.isPropertyAccessorInline: Boolean get() = Flag.PropertyAccessor.IS_INLINE(this)
@KotlinPoetMetadataPreview
val Flags.isPropertyAccessorNotDefault: Boolean
  get() = Flag.PropertyAccessor.IS_NOT_DEFAULT(this)

// TypeParameter flags.
@KotlinPoetMetadataPreview
val KmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)
@KotlinPoetMetadataPreview
val ImmutableKmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)

// Property Accessor Flags
enum class PropertyAccessorFlag {
  IS_EXTERNAL, IS_INLINE, IS_NOT_DEFAULT
}

@KotlinPoetMetadataPreview
val ImmutableKmProperty.setterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = setterFlags.propertyAccessorFlags

@KotlinPoetMetadataPreview
val ImmutableKmProperty.getterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = getterFlags.propertyAccessorFlags

@KotlinPoetMetadataPreview
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

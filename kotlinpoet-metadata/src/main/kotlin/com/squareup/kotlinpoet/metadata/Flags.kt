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
public val Flags.hasAnnotations: Boolean get() = Flag.HAS_ANNOTATIONS(this)
@KotlinPoetMetadataPreview
public val Flags.isAbstract: Boolean get() = Flag.IS_ABSTRACT(this)
@KotlinPoetMetadataPreview
public val Flags.isFinal: Boolean get() = Flag.IS_FINAL(this)
@KotlinPoetMetadataPreview
public val Flags.isInternal: Boolean get() = Flag.IS_INTERNAL(this)
@KotlinPoetMetadataPreview
public val Flags.isLocal: Boolean get() = Flag.IS_LOCAL(this)
@KotlinPoetMetadataPreview
public val Flags.isOpen: Boolean get() = Flag.IS_OPEN(this)
@KotlinPoetMetadataPreview
public val Flags.isPrivate: Boolean get() = Flag.IS_PRIVATE(this)
@KotlinPoetMetadataPreview
public val Flags.isPrivate_to_this: Boolean get() = Flag.IS_PRIVATE_TO_THIS(this)
@KotlinPoetMetadataPreview
public val Flags.isProtected: Boolean get() = Flag.IS_PROTECTED(this)
@KotlinPoetMetadataPreview
public val Flags.isPublic: Boolean get() = Flag.IS_PUBLIC(this)
@KotlinPoetMetadataPreview
public val Flags.isSealed: Boolean get() = Flag.IS_SEALED(this)
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.hasAnnotations: Boolean get() = flags.hasAnnotations
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isAbstract: Boolean get() = flags.isAbstract
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isFinal: Boolean get() = flags.isFinal
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isInternal: Boolean get() = flags.isInternal
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isLocal: Boolean get() = flags.isLocal
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isOpen: Boolean get() = flags.isOpen
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isPrivate: Boolean get() = flags.isPrivate
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isPrivate_to_this: Boolean get() = flags.isPrivate_to_this
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isProtected: Boolean get() = flags.isProtected
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isPublic: Boolean get() = flags.isPublic
@KotlinPoetMetadataPreview
public val ImmutableKmWithFlags.isSealed: Boolean get() = flags.isSealed

// Type flags.
@KotlinPoetMetadataPreview
public val Flags.isNullableType: Boolean get() = Flag.Type.IS_NULLABLE(this)
@KotlinPoetMetadataPreview
public val Flags.isSuspendType: Boolean get() = Flag.Type.IS_SUSPEND(this)

// Class flags.
@KotlinPoetMetadataPreview
public val Flags.isAnnotationClass: Boolean get() = Flag.Class.IS_ANNOTATION_CLASS(this)
@KotlinPoetMetadataPreview
public val Flags.isClass: Boolean get() = Flag.Class.IS_CLASS(this)
@KotlinPoetMetadataPreview
public val Flags.isCompanionObjectClass: Boolean get() = Flag.Class.IS_COMPANION_OBJECT(this)
@KotlinPoetMetadataPreview
public val Flags.isDataClass: Boolean get() = Flag.Class.IS_DATA(this)
@KotlinPoetMetadataPreview
public val Flags.isEnumClass: Boolean get() = Flag.Class.IS_ENUM_CLASS(this)
@KotlinPoetMetadataPreview
public val Flags.isEnumEntryClass: Boolean get() = Flag.Class.IS_ENUM_ENTRY(this)
@KotlinPoetMetadataPreview
public val Flags.isExpectClass: Boolean get() = Flag.Class.IS_EXPECT(this)
@KotlinPoetMetadataPreview
public val Flags.isExternalClass: Boolean get() = Flag.Class.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
public val Flags.isInlineClass: Boolean get() = Flag.Class.IS_INLINE(this)
@KotlinPoetMetadataPreview
public val Flags.isInnerClass: Boolean get() = Flag.Class.IS_INNER(this)
@KotlinPoetMetadataPreview
public val Flags.isObjectClass: Boolean get() = Flag.Class.IS_OBJECT(this)
@KotlinPoetMetadataPreview
public val Flags.isInterface: Boolean get() = Flag.Class.IS_INTERFACE(this)
@KotlinPoetMetadataPreview
public val KmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadataPreview
public val KmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadataPreview
public val KmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadataPreview
public val KmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadataPreview
public val KmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadataPreview
public val KmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadataPreview
public val KmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadataPreview
public val KmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadataPreview
public val KmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadataPreview
public val KmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadataPreview
public val KmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadataPreview
public val KmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadataPreview
public val KmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadataPreview
public val KmType.isNullable: Boolean get() = flags.isNullableType
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isAnnotation: Boolean get() = flags.isAnnotationClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isClass: Boolean get() = flags.isClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isCompanionObject: Boolean get() = flags.isCompanionObjectClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isData: Boolean get() = flags.isDataClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isEnum: Boolean get() = flags.isEnumClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isEnumEntry: Boolean get() = flags.isEnumEntryClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isExpect: Boolean get() = flags.isExpectClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isExternal: Boolean get() = flags.isExternalClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isInline: Boolean get() = flags.isInlineClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isInner: Boolean get() = flags.isInnerClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isObject: Boolean get() = flags.isObjectClass
@KotlinPoetMetadataPreview
public val ImmutableKmClass.isInterface: Boolean get() = flags.isInterface
@KotlinPoetMetadataPreview
public val ImmutableKmType.isSuspend: Boolean get() = flags.isSuspendType
@KotlinPoetMetadataPreview
public val ImmutableKmType.isNullable: Boolean get() = flags.isNullableType

// Constructor flags.
@KotlinPoetMetadataPreview
public val Flags.isPrimaryConstructor: Boolean get() = Flag.Constructor.IS_PRIMARY(this)
@KotlinPoetMetadataPreview
public val KmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadataPreview
public val KmConstructor.isSecondary: Boolean get() = !isPrimary
@KotlinPoetMetadataPreview
public val ImmutableKmConstructor.isPrimary: Boolean get() = flags.isPrimaryConstructor
@KotlinPoetMetadataPreview
public val ImmutableKmConstructor.isSecondary: Boolean get() = !isPrimary

// Function flags.
@KotlinPoetMetadataPreview
public val Flags.isDeclarationFunction: Boolean get() = Flag.Function.IS_DECLARATION(this)
@KotlinPoetMetadataPreview
public val Flags.isFakeOverrideFunction: Boolean get() = Flag.Function.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadataPreview
public val Flags.isDelegationFunction: Boolean get() = Flag.Function.IS_DELEGATION(this)
@KotlinPoetMetadataPreview
public val Flags.isSynthesizedFunction: Boolean get() = Flag.Function.IS_SYNTHESIZED(this)
@KotlinPoetMetadataPreview
public val Flags.isOperatorFunction: Boolean get() = Flag.Function.IS_OPERATOR(this)
@KotlinPoetMetadataPreview
public val Flags.isInfixFunction: Boolean get() = Flag.Function.IS_INFIX(this)
@KotlinPoetMetadataPreview
public val Flags.isInlineFunction: Boolean get() = Flag.Function.IS_INLINE(this)
@KotlinPoetMetadataPreview
public val Flags.isTailRecFunction: Boolean get() = Flag.Function.IS_TAILREC(this)
@KotlinPoetMetadataPreview
public val Flags.isExternalFunction: Boolean get() = Flag.Function.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
public val Flags.isSuspendFunction: Boolean get() = Flag.Function.IS_SUSPEND(this)
@KotlinPoetMetadataPreview
public val Flags.isExpectFunction: Boolean get() = Flag.Function.IS_EXPECT(this)
@KotlinPoetMetadataPreview
public val KmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadataPreview
public val KmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadataPreview
public val KmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadataPreview
public val KmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadataPreview
public val KmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadataPreview
public val KmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadataPreview
public val KmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadataPreview
public val KmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadataPreview
public val KmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadataPreview
public val KmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadataPreview
public val KmFunction.isExpect: Boolean get() = flags.isExpectFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isDeclaration: Boolean get() = flags.isDeclarationFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isFakeOverride: Boolean get() = flags.isFakeOverrideFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isDelegation: Boolean get() = flags.isDelegationFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isSynthesized: Boolean get() = flags.isSynthesizedFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isOperator: Boolean get() = flags.isOperatorFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isInfix: Boolean get() = flags.isInfixFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isInline: Boolean get() = flags.isInlineFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isTailRec: Boolean get() = flags.isTailRecFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isExternal: Boolean get() = flags.isExternalFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isSuspend: Boolean get() = flags.isSuspendFunction
@KotlinPoetMetadataPreview
public val ImmutableKmFunction.isExpect: Boolean get() = flags.isExpectFunction

// Parameter flags.
@KotlinPoetMetadataPreview
public val KmValueParameter.declaresDefaultValue: Boolean get() =
  Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadataPreview
public val KmValueParameter.isCrossInline: Boolean get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadataPreview
public val KmValueParameter.isNoInline: Boolean get() = Flag.ValueParameter.IS_NOINLINE(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmValueParameter.declaresDefaultValue: Boolean
  get() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmValueParameter.isCrossInline: Boolean
  get() = Flag.ValueParameter.IS_CROSSINLINE(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmValueParameter.isNoInline: Boolean
  get() = Flag.ValueParameter.IS_NOINLINE(flags)

// Property flags.
@KotlinPoetMetadataPreview
public val Flags.isFakeOverrideProperty: Boolean get() = Flag.Property.IS_FAKE_OVERRIDE(this)
@KotlinPoetMetadataPreview
public val KmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadataPreview
public val KmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadataPreview
public val KmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadataPreview
public val KmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadataPreview
public val KmProperty.isVal: Boolean get() = !isVar
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.hasConstant: Boolean get() = Flag.Property.HAS_CONSTANT(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.hasGetter: Boolean get() = Flag.Property.HAS_GETTER(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.hasSetter: Boolean get() = Flag.Property.HAS_SETTER(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isConst: Boolean get() = Flag.Property.IS_CONST(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isDeclaration: Boolean get() = Flag.Property.IS_DECLARATION(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isDelegated: Boolean get() = Flag.Property.IS_DELEGATED(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isDelegation: Boolean get() = Flag.Property.IS_DELEGATION(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isExpect: Boolean get() = Flag.Property.IS_EXPECT(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isExternal: Boolean get() = Flag.Property.IS_EXTERNAL(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isFakeOverride: Boolean get() = flags.isFakeOverrideProperty
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isLateinit: Boolean get() = Flag.Property.IS_LATEINIT(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isSynthesized: Boolean get() = Flag.Property.IS_SYNTHESIZED(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isVar: Boolean get() = Flag.Property.IS_VAR(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmProperty.isVal: Boolean get() = !isVar

// Property Accessor Flags
@KotlinPoetMetadataPreview
public val Flags.isPropertyAccessorExternal: Boolean
  get() = Flag.PropertyAccessor.IS_EXTERNAL(this)
@KotlinPoetMetadataPreview
public val Flags.isPropertyAccessorInline: Boolean
  get() = Flag.PropertyAccessor.IS_INLINE(this)
@KotlinPoetMetadataPreview
public val Flags.isPropertyAccessorNotDefault: Boolean
  get() = Flag.PropertyAccessor.IS_NOT_DEFAULT(this)

// TypeParameter flags.
@KotlinPoetMetadataPreview
public val KmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)
@KotlinPoetMetadataPreview
public val ImmutableKmTypeParameter.isReified: Boolean get() = Flag.TypeParameter.IS_REIFIED(flags)

// Property Accessor Flags
public enum class PropertyAccessorFlag {
  IS_EXTERNAL,
  IS_INLINE,
  IS_NOT_DEFAULT,
}

@KotlinPoetMetadataPreview
public val ImmutableKmProperty.setterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = setterFlags.propertyAccessorFlags

@KotlinPoetMetadataPreview
public val ImmutableKmProperty.getterPropertyAccessorFlags: Set<PropertyAccessorFlag>
  get() = getterFlags.propertyAccessorFlags

@KotlinPoetMetadataPreview
public val Flags.propertyAccessorFlags: Set<PropertyAccessorFlag>
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

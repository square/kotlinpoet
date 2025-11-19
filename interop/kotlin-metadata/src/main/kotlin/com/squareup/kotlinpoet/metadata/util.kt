/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.metadata

import com.squareup.kotlinpoet.KModifier
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmProperty
import kotlin.metadata.MemberKind
import kotlin.metadata.Modality
import kotlin.metadata.Visibility
import kotlin.metadata.isSecondary
import kotlin.metadata.isVar
import kotlin.metadata.kind

internal val KmClass.isObject: Boolean
  get() = kind == ClassKind.OBJECT

internal val KmClass.isCompanionObject: Boolean
  get() = kind == ClassKind.COMPANION_OBJECT

internal val KmClass.isClass: Boolean
  get() = kind == ClassKind.CLASS

internal val KmClass.isAnnotation: Boolean
  get() = kind == ClassKind.ANNOTATION_CLASS

internal val KmClass.isEnum: Boolean
  get() = kind == ClassKind.ENUM_CLASS

internal val KmClass.isInterface: Boolean
  get() = kind == ClassKind.INTERFACE

internal val KmConstructor.isPrimary: Boolean
  get() = !isSecondary

internal val KmProperty.isVal: Boolean
  get() = !isVar

internal fun Modality.toKModifier(): KModifier =
  when (this) {
    Modality.FINAL -> KModifier.FINAL
    Modality.OPEN -> KModifier.OPEN
    Modality.ABSTRACT -> KModifier.ABSTRACT
    Modality.SEALED -> KModifier.SEALED
  }

internal fun Visibility.toKModifier(): KModifier =
  when (this) {
    Visibility.INTERNAL -> KModifier.INTERNAL
    Visibility.PRIVATE -> KModifier.PRIVATE
    Visibility.PROTECTED -> KModifier.PROTECTED
    Visibility.PUBLIC -> KModifier.PUBLIC
    Visibility.PRIVATE_TO_THIS,
    Visibility.LOCAL -> {
      // Default to public
      KModifier.PUBLIC
    }
  }

internal val MemberKind.isDeclaration: Boolean
  get() = this == MemberKind.DECLARATION

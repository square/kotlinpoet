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
package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/**
 * Represents the name of a member (such as a function or a property).
 *
 * @param packageName e.g. `kotlin.collections`
 * @param enclosingClassName e.g. `Map.Entry.Companion`, if the member is declared inside the
 *   companion object of the Map.Entry class
 * @param simpleName e.g. `isBlank`, `size`
 * @param isExtension whether the member is an extension property or an extension function. Default
 *   is false.
 *
 * If there is a member with the same name as this member in a local scope, the generated code will
 * include this member's fully-qualified name to avoid ambiguity, e.g.:
 * ```kotlin
 * package com.squareup.tacos
 *
 * import kotlin.Unit
 *
 * public class TacoTest {
 *   public fun test(): Unit {
 *     kotlin.error("errorText")
 *   }
 *
 *   public fun error(): Unit {
 *   }
 * }
 * ```
 *
 * However, since Kotlin compiler does not allow fully-qualified extension members, if [isExtension]
 * is set to true for this [MemberName], the generated code will include an import for this member
 * and its simple name at the call site, e.g.:
 * ```kotlin
 * package com.squareup.tacos
 *
 * import kotlin.Unit
 * import kotlin.hashCode
 *
 * public class TacoTest {
 *   public override fun hashCode(): Unit {
 *     var result = super.hashCode
 *     if (result == 0) {
 *       result = result * 37 + embedded_message.hashCode()
 *       super.hashCode = result
 *     }
 *     return result
 *   }
 * }
 * ```
 */
@ExposedCopyVisibility
public data class MemberName
internal constructor(
  public val packageName: String,
  public val enclosingClassName: ClassName?,
  public val simpleName: String,
  public val operator: KOperator? = null,
  public val isExtension: Boolean = false,
) {
  // TODO(egorand): Reduce the number of overloaded constructors in KotlinPoet 2.0.

  public constructor(
    packageName: String,
    simpleName: String,
  ) : this(packageName, enclosingClassName = null, simpleName)

  public constructor(
    packageName: String,
    simpleName: String,
    isExtension: Boolean,
  ) : this(packageName, enclosingClassName = null, simpleName, operator = null, isExtension)

  public constructor(
    enclosingClassName: ClassName,
    simpleName: String,
  ) : this(enclosingClassName.packageName, enclosingClassName, simpleName)

  public constructor(
    enclosingClassName: ClassName,
    simpleName: String,
    isExtension: Boolean,
  ) : this(
    enclosingClassName.packageName,
    enclosingClassName,
    simpleName,
    operator = null,
    isExtension,
  )

  public constructor(
    packageName: String,
    operator: KOperator,
  ) : this(packageName, enclosingClassName = null, operator.functionName, operator)

  public constructor(
    enclosingClassName: ClassName,
    operator: KOperator,
  ) : this(enclosingClassName.packageName, enclosingClassName, operator.functionName, operator)

  /** Fully qualified name using `.` as a separator, like `kotlin.String.isBlank`. */
  public val canonicalName: String = buildString {
    if (enclosingClassName != null) {
      append(enclosingClassName.canonicalName)
      append('.')
    } else if (packageName.isNotBlank()) {
      append(packageName)
      append('.')
    }
    append(simpleName)
  }

  /**
   * Callable reference to this member. Emits [enclosingClassName] if it exists, followed by the
   * reference operator `::`, followed by either [simpleName] or the fully-qualified name if this is
   * a top-level member.
   *
   * Note: As `::$packageName.$simpleName` is not valid syntax, an aliased import may be required
   * for a top-level member with a conflicting name.
   */
  public fun reference(): CodeBlock =
    when (enclosingClassName) {
      null -> CodeBlock.of("::%M", this)
      else -> CodeBlock.of("%T::%N", enclosingClassName, simpleName)
    }

  internal fun emit(out: CodeWriter) {
    if (operator == null) {
      out.emit(out.lookupName(this).escapeSegmentsIfNecessary())
    } else {
      out.lookupName(this)
      out.emit(operator.operator)
    }
  }

  override fun toString(): String = canonicalName

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    @JvmSynthetic
    @JvmStatic
    public inline fun ClassName.member(simpleName: String): MemberName =
      MemberName(this, simpleName)

    @JvmStatic
    @JvmName("get")
    public fun KClass<*>.member(simpleName: String): MemberName = asClassName().member(simpleName)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    @JvmStatic
    @JvmName("get")
    public fun Class<*>.member(simpleName: String): MemberName = asClassName().member(simpleName)
  }
}

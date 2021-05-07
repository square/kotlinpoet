package com.squareup.kotlinpoet

import kotlin.reflect.KClass

/**
 * Represents the name of a member (such as a function or a property).
 *
 * @param packageName e.g. `kotlin.collections`
 * @param enclosingClassName e.g. `Map.Entry.Companion`, if the member is declared inside the
 * companion object of the Map.Entry class
 * @param simpleName e.g. `isBlank`, `size`
 */
public data class MemberName internal constructor(
  public val packageName: String,
  public val enclosingClassName: ClassName?,
  public val simpleName: String,
  public val operator: KOperator? = null
) {
  public constructor(packageName: String, simpleName: String) : this(packageName, null, simpleName)
  public constructor(enclosingClassName: ClassName, simpleName: String) :
    this(enclosingClassName.packageName, enclosingClassName, simpleName)
  public constructor(packageName: String, operator: KOperator) :
    this(packageName, null, operator.functionName, operator)
  public constructor(enclosingClassName: ClassName, operator: KOperator) :
    this(enclosingClassName.packageName, enclosingClassName, operator.functionName, operator)

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
   * Callable reference to this member. Emits [enclosingClassName] if it exists, followed by
   * the reference operator `::`, followed by either [simpleName] or the fully-qualified
   * name if this is a top-level member.
   *
   * Note: As `::$packageName.$simpleName` is not valid syntax, an aliased import may be
   * required for a top-level member with a conflicting name.
   */
  public fun reference(): CodeBlock = when (enclosingClassName) {
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
    @JvmSynthetic @JvmStatic public inline fun ClassName.member(simpleName: String): MemberName =
      MemberName(this, simpleName)
    @JvmStatic @JvmName("get") public fun KClass<*>.member(simpleName: String): MemberName =
      asClassName().member(simpleName)
    @JvmStatic @JvmName("get") public fun Class<*>.member(simpleName: String): MemberName =
      asClassName().member(simpleName)
  }
}

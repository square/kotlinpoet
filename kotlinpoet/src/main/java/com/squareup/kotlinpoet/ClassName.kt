/*
 * Copyright (C) 2014 Google, Inc.
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
@file:JvmName("ClassNames")

package com.squareup.kotlinpoet

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.NestingKind.MEMBER
import javax.lang.model.element.NestingKind.TOP_LEVEL
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.DeprecationLevel.WARNING
import kotlin.reflect.KClass

/** A fully-qualified class name for top-level and member classes.  */
public class ClassName internal constructor(
  names: List<String>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap()
) : TypeName(nullable, annotations, TagMap(tags)), Comparable<ClassName> {
  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  @Deprecated("", level = DeprecationLevel.HIDDEN)
  public constructor(packageName: String, simpleName: String, vararg simpleNames: String) :
    this(listOf(packageName, simpleName, *simpleNames))

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  public constructor(packageName: String, vararg simpleNames: String) :
    this(listOf(packageName, *simpleNames)) {
      require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
      require(simpleNames.none { it.isEmpty() }) {
        "simpleNames must not contain empty items: ${simpleNames.contentToString()}"
      }
    }

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  public constructor(packageName: String, simpleNames: List<String>) :
    this(mutableListOf(packageName).apply { addAll(simpleNames) }) {
      require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
      require(simpleNames.none { it.isEmpty() }) {
        "simpleNames must not contain empty items: $simpleNames"
      }
    }

  /** From top to bottom. This will be `["java.util", "Map", "Entry"]` for `Map.Entry`. */
  private val names = names.toImmutableList()

  /** Fully qualified name using `.` as a separator, like `kotlin.collections.Map.Entry`. */
  public val canonicalName: String = if (names[0].isEmpty())
    names.subList(1, names.size).joinToString(".") else
    names.joinToString(".")

  /** Package name, like `"kotlin.collections"` for `Map.Entry`. */
  public val packageName: String get() = names[0]

  /** Simple name of this class, like `"Entry"` for `Map.Entry`. */
  public val simpleName: String get() = names[names.size - 1]

  /**
   * The enclosing classes, outermost first, followed by the simple name. This is `["Map", "Entry"]`
   * for `Map.Entry`.
   */
  public val simpleNames: List<String> get() = names.subList(1, names.size)

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): ClassName {
    return ClassName(names, nullable, annotations, tags)
  }

  /**
   * Returns the enclosing class, like `Map` for `Map.Entry`. Returns null if this class is not
   * nested in another class.
   */
  public fun enclosingClassName(): ClassName? {
    return if (names.size != 2)
      ClassName(names.subList(0, names.size - 1)) else
      null
  }

  /**
   * Returns the top class in this nesting group. Equivalent to chained calls to
   * [ClassName.enclosingClassName] until the result's enclosing class is null.
   */
  public fun topLevelClassName(): ClassName = ClassName(names.subList(0, 2))

  /**
   * Fully qualified name using `.` to separate package from the top level class name, and `$` to
   * separate nested classes, like `kotlin.collections.Map$Entry`.
   */
  public fun reflectionName(): String {
    // trivial case: no nested names
    if (names.size == 2) {
      return if (packageName.isEmpty())
        names[1] else
        packageName + "." + names[1]
    }
    // concat top level class name and nested names
    return buildString {
      append(topLevelClassName().canonicalName)
      for (name in simpleNames.subList(1, simpleNames.size)) {
        append('$').append(name)
      }
    }
  }

  /**
   * Callable reference to the constructor of this class. Emits the enclosing class if one exists,
   * followed by the reference operator `::`, followed by either [simpleName] or the
   * fully-qualified name if this is a top-level class.
   *
   * Note: As `::$packageName.$simpleName` is not valid syntax, an aliased import may be required
   * for a top-level class with a conflicting name.
   */
  public fun constructorReference(): CodeBlock {
    val enclosing = enclosingClassName()
    return if (enclosing != null) {
      CodeBlock.of("%T::%N", enclosing, simpleName)
    } else {
      CodeBlock.of("::%T", this)
    }
  }

  /** Returns a new [ClassName] instance for the specified `name` as nested inside this class. */
  public fun nestedClass(name: String): ClassName = ClassName(names + name)

  /**
   * Returns a class that shares the same enclosing package or class. If this class is enclosed by
   * another class, this is equivalent to `enclosingClassName().nestedClass(name)`. Otherwise
   * it is equivalent to `get(packageName(), name)`.
   */
  public fun peerClass(name: String): ClassName {
    val result = names.toMutableList()
    result[result.size - 1] = name
    return ClassName(result)
  }

  /**
   * Orders by the fully-qualified name. Nested types are ordered immediately after their
   * enclosing type. For example, the following types are ordered by this method:
   *
   * ```
   * com.example.Robot
   * com.example.Robot.Motor
   * com.example.RoboticVacuum
   * ```
   */
  override fun compareTo(other: ClassName): Int = canonicalName.compareTo(other.canonicalName)

  override fun emit(out: CodeWriter) =
    out.emit(out.lookupName(this).escapeSegmentsIfNecessary())

  public companion object {
    /**
     * Returns a new [ClassName] instance for the given fully-qualified class name string. This
     * method assumes that the input is ASCII and follows typical Java style (lowercase package
     * names, UpperCamelCase class names) and may produce incorrect results or throw
     * [IllegalArgumentException] otherwise. For that reason, the constructor should be preferred as
     * it can create [ClassName] instances without such restrictions.
     */
    @JvmStatic public fun bestGuess(classNameString: String): ClassName {
      val names = mutableListOf<String>()

      // Add the package name, like "java.util.concurrent", or "" for no package.
      var p = 0
      while (p < classNameString.length && Character.isLowerCase(classNameString.codePointAt(p))) {
        p = classNameString.indexOf('.', p) + 1
        require(p != 0) { "couldn't make a guess for $classNameString" }
      }
      names += if (p != 0) classNameString.substring(0, p - 1) else ""

      // Add the class names, like "Map" and "Entry".
      for (part in classNameString.substring(p).split('.')) {
        require(part.isNotEmpty() && Character.isUpperCase(part.codePointAt(0))) {
          "couldn't make a guess for $classNameString"
        }

        names += part
      }

      require(names.size >= 2) { "couldn't make a guess for $classNameString" }
      return ClassName(names)
    }
  }
}

@JvmName("get")
public fun Class<*>.asClassName(): ClassName {
  require(!isPrimitive) { "primitive types cannot be represented as a ClassName" }
  require(Void.TYPE != this) { "'void' type cannot be represented as a ClassName" }
  require(!isArray) { "array types cannot be represented as a ClassName" }
  val names = mutableListOf<String>()
  var c = this
  while (true) {
    names += c.simpleName
    val enclosing = c.enclosingClass ?: break
    c = enclosing
  }
  // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
  val lastDot = c.name.lastIndexOf('.')
  if (lastDot != -1) names += c.name.substring(0, lastDot)
  names.reverse()
  return ClassName(names)
}

@JvmName("get")
public fun KClass<*>.asClassName(): ClassName {
  qualifiedName?.let { return ClassName.bestGuess(it) }
  throw IllegalArgumentException("$this cannot be represented as a ClassName")
}

/** Returns the class name for `element`. */
@Deprecated(
  message = "Element APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
  level = WARNING
)
@JvmName("get")
public fun TypeElement.asClassName(): ClassName {
  fun isClassOrInterface(e: Element) = e.kind.isClass || e.kind.isInterface

  fun getPackage(type: Element): PackageElement {
    var t = type
    while (t.kind != ElementKind.PACKAGE) {
      t = t.enclosingElement
    }
    return t as PackageElement
  }

  val names = mutableListOf<String>()
  var e: Element = this
  while (isClassOrInterface(e)) {
    val eType = e as TypeElement
    require(eType.nestingKind.isOneOf(TOP_LEVEL, MEMBER)) {
      "unexpected type testing"
    }
    names += eType.simpleName.toString()
    e = eType.enclosingElement
  }
  names += getPackage(this).qualifiedName.toString()
  names.reverse()
  return ClassName(names)
}

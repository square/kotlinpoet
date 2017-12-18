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
import kotlin.reflect.KClass

/** A fully-qualified class name for top-level and member classes.  */
class ClassName internal constructor(
  names: List<String>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations), Comparable<ClassName> {
  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields [Map.Entry].
   */
  constructor(packageName: String, simpleName: String, vararg simpleNames: String)
      : this(listOf(packageName, simpleName, *simpleNames))

  /** From top to bottom. This will be `["java.util", "Map", "Entry"]` for [Map.Entry].  */
  private val names = names.toImmutableList()
  val canonicalName = if (names[0].isEmpty())
    names.subList(1, names.size).joinToString(".") else
    names.joinToString(".")

  init {
    for (i in 1 until names.size) {
      require(names[i].isName) { "part ${names[i]} is keyword" }
    }
  }

  override fun asNullable() = ClassName(names, true, annotations)

  override fun asNonNullable() = ClassName(names, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>)
      = ClassName(names, nullable, this.annotations + annotations)

  override fun withoutAnnotations() = ClassName(names, nullable)

  /** Returns the package name, like `"java.util"` for `Map.Entry`.  */
  fun packageName() = names[0]

  /**
   * Returns the enclosing class, like [Map] for `Map.Entry`. Returns null if this class
   * is not nested in another class.
   */
  fun enclosingClassName(): ClassName? {
    return if (names.size != 2)
      ClassName(names.subList(0, names.size - 1)) else
      null
  }

  /**
   * Returns the top class in this nesting group. Equivalent to chained calls to
   * [ClassName.enclosingClassName] until the result's enclosing class is null.
   */
  fun topLevelClassName() = ClassName(names.subList(0, 2))

  fun reflectionName(): String {
    // trivial case: no nested names
    if (names.size == 2) {
      val packageName = packageName()
      return if (packageName.isEmpty())
        names[1] else
        packageName + "." + names[1]
    }
    // concat top level class name and nested names
    return buildString {
      append(topLevelClassName())
      for (name in simpleNames().subList(1, simpleNames().size)) {
        append('$').append(name)
      }
    }
  }

  /**
   * Returns a new [ClassName] instance for the specified `name` as nested inside this
   * class.
   */
  fun nestedClass(name: String) = ClassName(names + name)

  fun simpleNames() = names.subList(1, names.size)

  /**
   * Returns a class that shares the same enclosing package or class. If this class is enclosed by
   * another class, this is equivalent to `enclosingClassName().nestedClass(name)`. Otherwise
   * it is equivalent to `get(packageName(), name)`.
   */
  fun peerClass(name: String): ClassName {
    val result = names.toMutableList()
    result[result.size - 1] = name
    return ClassName(result)
  }

  /** Returns the simple name of this class, like `"Entry"` for [Map.Entry].  */
  fun simpleName() = names[names.size - 1]

  override fun compareTo(other: ClassName) = canonicalName.compareTo(other.canonicalName)

  override fun emit(out: CodeWriter) = out.emit(out.lookupName(this))

  companion object {
    /**
     * Returns a new [ClassName] instance for the given fully-qualified class name string. This
     * method assumes that the input is ASCII and follows typical Java style (lowercase package
     * names, UpperCamelCase class names) and may produce incorrect results or throw
     * [IllegalArgumentException] otherwise. For that reason, [.get] and
     * [.get] should be preferred as they can correctly create [ClassName]
     * instances without such restrictions.
     */
    @JvmStatic fun bestGuess(classNameString: String): ClassName {
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
fun Class<*>.asClassName(): ClassName {
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
fun KClass<*>.asClassName(): ClassName {
  qualifiedName?.let { return ClassName.bestGuess(it) }
  throw IllegalArgumentException("$this cannot be represented as a ClassName")
}

/** Returns the class name for `element`.  */
@JvmName("get")
fun TypeElement.asClassName(): ClassName {
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

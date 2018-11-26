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

internal interface ClassNameInterface {
  val names: List<String>

  /** Fully qualified name using `.` as a separator, like `kotlin.collections.Map.Entry`. */
  val canonicalName
    get() = if (names[0].isEmpty())
      names.subList(1, names.size).joinToString(".") else
      names.joinToString(".")

  /** Package name, like `"java.util"` for `Map.Entry`.  */
  val packageName get() = names[0]

  /** Simple name of this class, like `"Entry"` for [Map.Entry].  */
  val simpleName get() = names[names.size - 1]
  val simpleNames get() = names.subList(1, names.size)

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
   * [enclosingClassName] until the result's enclosing class is null.
   */
  fun topLevelClassName() = ClassName(names.subList(0, 2))

  /**
   * Fully qualified name using `.` to separate package from the top level class name, and `$` to
   * separate nested classes, like `kotlin.collections.Map$Entry`.
   */
  fun reflectionName(): String {
    // trivial case: no nested names
    if (names.size == 2) {
      return if (packageName.isEmpty())
        names[1] else
        packageName + "." + names[1]
    }
    // concat top level class name and nested names
    return buildString {
      append(topLevelClassName())
      for (name in simpleNames.subList(1, simpleNames.size)) {
        append('$').append(name)
      }
    }
  }

  /**
   * Returns a new [ClassName] instance for the specified `name` as nested inside this
   * class.
   */
  fun nestedClass(name: String) = ClassName(names + name)

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
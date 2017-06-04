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
package com.squareup.kotlinpoet

import java.io.IOException
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.NestingKind.MEMBER
import javax.lang.model.element.NestingKind.TOP_LEVEL
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

/** A fully-qualified class name for top-level and member classes.  */
class ClassName private constructor(
    names: List<String>,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList())
  : TypeName(nullable, annotations), Comparable<ClassName> {

  /** From top to bottom. This will be `["java.util", "Map", "Entry"]` for [Map.Entry].  */
  internal val names = names.toImmutableList()
  val canonicalName = if (names[0].isEmpty())
    names.subList(1, names.size).joinToString(".") else
    names.joinToString(".")

  init {
    for (i in 1 until names.size) {
      require(SourceVersion.isName(names[i])) { "part ${names[i]} is keyword" }
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
    val builder = StringBuilder()
    builder.append(topLevelClassName())
    for (name in simpleNames().subList(1, simpleNames().size)) {
      builder.append('$').append(name)
    }
    return builder.toString()
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

  @Throws(IOException::class)
  override fun emit(out: CodeWriter): CodeWriter {
    return out.emit(out.lookupName(this))
  }

  companion object {
    @JvmStatic fun get(clazz: Class<*>): ClassName {
      require(!clazz.isPrimitive) { "primitive types cannot be represented as a ClassName" }
      require(Void.TYPE != clazz) { "'void' type cannot be represented as a ClassName" }
      require(!clazz.isArray) { "array types cannot be represented as a ClassName" }
      val names = mutableListOf<String>()
      var c = clazz
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

    @JvmStatic fun get(clazz: KClass<*>): ClassName {
      val qualifiedName = clazz.qualifiedName
      if (qualifiedName == null) {
        throw IllegalArgumentException("$clazz cannot be represented as a TypeName")
      }
      return ClassName.bestGuess(qualifiedName)
    }

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
        require(p != 0) { "couldn't make a guess for ${classNameString}" }
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

    /**
     * Returns a class name created from the given parts. For example, calling this with package name
     * `"java.util"` and simple names `"Map"`, `"Entry"` yields [Map.Entry].
     */
    @JvmStatic fun get(
        packageName: String,
        simpleName: String,
        vararg simpleNames: String): ClassName {
      return ClassName(listOf(packageName, simpleName, *simpleNames))
    }

    /** Returns the class name for `element`.  */
    @JvmStatic fun get(element: TypeElement): ClassName {
      val names = mutableListOf<String>()
      var e: Element = element
      while (isClassOrInterface(e)) {
        require(element.nestingKind == TOP_LEVEL || element.nestingKind == MEMBER) {
          "unexpected type testing"
        }
        names += e.simpleName.toString()
        e = e.enclosingElement
      }
      names += getPackage(element).qualifiedName.toString()
      names.reverse()
      return ClassName(names)
    }

    private fun isClassOrInterface(e: Element): Boolean = e.kind.isClass || e.kind.isInterface

    private fun getPackage(type: Element): PackageElement {
      var t = type
      while (t.kind != ElementKind.PACKAGE) {
        t = t.enclosingElement
      }
      return t as PackageElement
    }
  }
}

/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("TypeNames")

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.NoType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleTypeVisitor7
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

/**
 * Any type in Kotlin's type system. This class identifies simple types like `Int` and `String`,
 * nullable types like `Int?`, composite types like `Array<String>` and `Set<String>`, and
 * unassignable types like `Unit`.
 *
 * Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for `kotlin.List` doesn't know about the `size()` function, the fact that lists are
 * collections, or even that it accepts a single type parameter.
 *
 * Instances of this class are immutable value objects that implement `equals()` and `hashCode()`
 * properly.
 *
 * Referencing existing types
 * --------------------------
 *
 * In an annotation processor you can get a type name instance for a type mirror by calling
 * [asTypeName]. In reflection code, you can use [asTypeName].

 * Defining new types
 * ------------------
 *
 * Create new reference types like `com.example.HelloWorld` with [ClassName.bestGuess]. To build composite
 * types like `Set<Long>`, use the factory methods on [ParameterizedTypeName], [TypeVariableName],
 * and [WildcardTypeName].
 */
abstract class TypeName internal constructor(
  val nullable: Boolean, annotations: List<AnnotationSpec>
) {
  val annotations = annotations.toImmutableList()

  /** Lazily-initialized toString of this type name.  */
  private val cachedString: String by lazy {
    buildCodeString {
      emitAnnotations(this)
      emit(this)
      if (nullable) emit("?")
    }
  }

  fun annotated(vararg annotations: AnnotationSpec) = annotated(annotations.toList())

  abstract fun asNullable(): TypeName

  abstract fun asNonNull(): TypeName

  abstract fun annotated(annotations: List<AnnotationSpec>): TypeName

  abstract fun withoutAnnotations(): TypeName

  val isAnnotated get() = annotations.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = cachedString

  internal abstract fun emit(out: CodeWriter): CodeWriter

  internal fun emitAnnotations(out: CodeWriter) {
    for (annotation in annotations) {
      annotation.emit(out, true)
      out.emit(" ")
    }
  }

  internal fun emitNullable(out: CodeWriter) {
    if (nullable) {
      out.emit("?")
    }
  }

  companion object {
    internal fun get(
      mirror: TypeMirror,
      typeVariables: Map<TypeParameterElement, TypeVariableName>
    ): TypeName {
      return mirror.accept(object : SimpleTypeVisitor7<TypeName, Void?>() {
        override fun visitPrimitive(t: PrimitiveType, p: Void?): TypeName {
          return when (t.kind) {
            TypeKind.BOOLEAN -> BOOLEAN
            TypeKind.BYTE -> BYTE
            TypeKind.SHORT -> SHORT
            TypeKind.INT -> INT
            TypeKind.LONG -> LONG
            TypeKind.CHAR -> CHAR
            TypeKind.FLOAT -> FLOAT
            TypeKind.DOUBLE -> DOUBLE
            else -> throw AssertionError()
          }
        }

        override fun visitDeclared(t: DeclaredType, p: Void?): TypeName {
          val rawType: ClassName = (t.asElement() as TypeElement).asClassName()
          val enclosingType = t.enclosingType
          val enclosing = if (enclosingType.kind != TypeKind.NONE
              && Modifier.STATIC !in t.asElement().modifiers)
            enclosingType.accept(this, null) else
            null
          if (t.typeArguments.isEmpty() && enclosing !is ParameterizedTypeName) {
            return rawType
          }

          val typeArgumentNames = mutableListOf<TypeName>()
          for (typeArgument in t.typeArguments) {
            typeArgumentNames += get(typeArgument, typeVariables)
          }
          return if (enclosing is ParameterizedTypeName)
            enclosing.nestedClass(rawType.simpleName, typeArgumentNames) else
            ParameterizedTypeName(null, rawType, typeArgumentNames)
        }

        override fun visitError(t: ErrorType, p: Void?): TypeName {
          return visitDeclared(t, p)
        }

        override fun visitArray(t: ArrayType, p: Void?): ParameterizedTypeName {
          return ARRAY.parameterizedBy(TypeName.get(t.componentType, typeVariables))
        }

        override fun visitTypeVariable(t: javax.lang.model.type.TypeVariable, p: Void?): TypeName {
          return TypeVariableName.get(t, typeVariables.toMutableMap())
        }

        override fun visitWildcard(t: javax.lang.model.type.WildcardType, p: Void?): TypeName {
          return WildcardTypeName.get(t, typeVariables)
        }

        override fun visitNoType(t: NoType, p: Void?): TypeName {
          if (t.kind == TypeKind.VOID) return UNIT
          return super.visitUnknown(t, p)
        }

        override fun defaultAction(e: TypeMirror?, p: Void?): TypeName {
          throw IllegalArgumentException("Unexpected type mirror: " + e!!)
        }
      }, null)
    }

    internal fun get(type: Type, map: MutableMap<Type, TypeVariableName>): TypeName {
      return when (type) {
        is Class<*> -> when {
          type === Void.TYPE -> UNIT
          type === Boolean::class.javaPrimitiveType -> BOOLEAN
          type === Byte::class.javaPrimitiveType -> BYTE
          type === Short::class.javaPrimitiveType -> SHORT
          type === Int::class.javaPrimitiveType -> INT
          type === Long::class.javaPrimitiveType -> LONG
          type === Char::class.javaPrimitiveType -> CHAR
          type === Float::class.javaPrimitiveType -> FLOAT
          type === Double::class.javaPrimitiveType -> DOUBLE
          type.isArray -> ARRAY.parameterizedBy(get(type.componentType, map))
          else -> type.asClassName()
        }
        is ParameterizedType -> ParameterizedTypeName.get(type, map)
        is WildcardType -> WildcardTypeName.get(type, map)
        is TypeVariable<*> -> TypeVariableName.get(type, map)
        is GenericArrayType -> ARRAY.parameterizedBy(get(type.genericComponentType, map))
        else -> throw IllegalArgumentException("unexpected type: " + type)
      }
    }
  }
}

@JvmField val ANY = ClassName("kotlin", "Any")
@JvmField val ARRAY = ClassName("kotlin", "Array")
@JvmField val UNIT = Unit::class.asClassName()
@JvmField val BOOLEAN = ClassName("kotlin", "Boolean")
@JvmField val BYTE = ClassName("kotlin", "Byte")
@JvmField val SHORT = ClassName("kotlin", "Short")
@JvmField val INT = ClassName("kotlin", "Int")
@JvmField val LONG = ClassName("kotlin", "Long")
@JvmField val CHAR = ClassName("kotlin", "Char")
@JvmField val FLOAT = ClassName("kotlin", "Float")
@JvmField val DOUBLE = ClassName("kotlin", "Double")

/** The wildcard type `*` which is shorthand for `out Any?`. */
@JvmField val STAR = WildcardTypeName.subtypeOf(ANY.asNullable())

/** [Dynamic] is a singleton `object` type, so this is a shorthand for it in Java. */
@JvmField val DYNAMIC = Dynamic

/** Returns a [TypeName] equivalent to this [TypeMirror]. */
@JvmName("get")
fun TypeMirror.asTypeName() = TypeName.get(this, mutableMapOf())

/** Returns a [TypeName] equivalent to this [KClass].  */
@JvmName("get")
fun KClass<*>.asTypeName() = asClassName()

/** Returns a [TypeName] equivalent to this [Type].  */
@JvmName("get")
fun Type.asTypeName() = TypeName.get(this, mutableMapOf())

/** A fully-qualified class name for top-level and member classes.  */
class ClassName internal constructor(
    names: List<String>,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations), Comparable<ClassName> {
  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  constructor(packageName: String, simpleName: String, vararg simpleNames: String)
      : this(listOf(packageName, simpleName, *simpleNames))

  /** From top to bottom. This will be `["java.util", "Map", "Entry"]` for `Map.Entry`.  */
  private val names = names.toImmutableList()

  /** Fully qualified name using `.` as a separator, like `kotlin.collections.Map.Entry`. */
  val canonicalName = if (names[0].isEmpty())
    names.subList(1, names.size).joinToString(".") else
    names.joinToString(".")

  /** Package name, like `"kotlin.collections"` for `Map.Entry`.  */
  val packageName get() = names[0]

  /** Simple name of this class, like `"Entry"` for `Map.Entry`.  */
  val simpleName get() = names[names.size - 1]

  /**
   * The enclosing classes, outermost first, followed by the simple name. This is `["Map", "Entry"]`
   * for `Map.Entry`.
   */
  val simpleNames get() = names.subList(1, names.size)

  init {
    for (i in 1 until names.size) {
      require(names[i].isName) { "part ${names[i]} is keyword" }
    }
  }

  override fun asNullable() = ClassName(names, true, annotations)

  override fun asNonNull() = ClassName(names, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>)
      = ClassName(names, nullable, this.annotations + annotations)

  override fun withoutAnnotations() = ClassName(names, nullable)

  /**
   * Returns the enclosing class, like `Map` for `Map.Entry`. Returns null if this class is not
   * nested in another class.
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

  /** Returns a new [ClassName] instance for the specified `name` as nested inside this class. */
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
  override fun compareTo(other: ClassName) = canonicalName.compareTo(other.canonicalName)

  override fun emit(out: CodeWriter) = out.emit(out.lookupName(this).escapeKeywords())

  companion object {
    /**
     * Returns a new [ClassName] instance for the given fully-qualified class name string. This
     * method assumes that the input is ASCII and follows typical Java style (lowercase package
     * names, UpperCamelCase class names) and may produce incorrect results or throw
     * [IllegalArgumentException] otherwise. For that reason, the constructor should be preferred as
     * it can create [ClassName] instances without such restrictions.
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

object Dynamic : TypeName(false, emptyList()) {

  override fun asNullable() =
      throw UnsupportedOperationException("dynamic can't be nullable")

  override fun asNonNull() =
      throw UnsupportedOperationException("dynamic can't be non-nullable")

  override fun annotated(annotations: List<AnnotationSpec>) =
      throw UnsupportedOperationException("dynamic can't have annotations")

  override fun withoutAnnotations() =
      throw UnsupportedOperationException("dynamic can't have annotations")

  override fun emit(out: CodeWriter) = out.apply {
    emit("dynamic")
  }
}

class LambdaTypeName internal constructor(
    val receiver: TypeName? = null,
    parameters: List<ParameterSpec> = emptyList(),
    val returnType: TypeName = UNIT,
    nullable: Boolean = false,
    val suspending: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  val parameters = parameters.toImmutableList()

  init {
    for (param in parameters) {
      require(param.annotations.isEmpty()) { "Parameters with annotations are not allowed" }
      require(param.modifiers.isEmpty()) { "Parameters with modifiers are not allowed" }
      require(param.defaultValue == null) { "Parameters with default values are not allowed" }
    }
  }

  override fun asNullable() = LambdaTypeName(receiver, parameters, returnType, true, suspending,
      annotations)

  override fun asNonNull()
      = LambdaTypeName(receiver, parameters, returnType, false, suspending, annotations)

  fun asSuspending() = LambdaTypeName(receiver, parameters, returnType, nullable, true, annotations)

  fun asNonSuspending() =
      LambdaTypeName(receiver, parameters, returnType, nullable, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>)
      = LambdaTypeName(receiver, parameters, returnType, nullable, suspending, annotations)

  override fun withoutAnnotations()
      = LambdaTypeName(receiver, parameters, returnType, nullable, suspending)

  override fun emit(out: CodeWriter): CodeWriter {
    emitAnnotations(out)

    if (nullable) {
      out.emit("(")
    }

    if (suspending) {
      out.emit("suspend ")
    }

    receiver?.let {
      if (it.isAnnotated) {
        out.emitCode("(%T).", it)
      } else {
        out.emitCode("%T.", it)
      }
    }

    parameters.emit(out)
    out.emitCode(if (returnType is LambdaTypeName) " -> (%T)" else " -> %T", returnType)

    if (nullable) {
      out.emit(")")
    }
    return out
  }

  companion object {
    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
        receiver: TypeName? = null,
        parameters: List<ParameterSpec> = emptyList(),
        returnType: TypeName
    ) = LambdaTypeName(receiver, parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
        receiver: TypeName? = null,
        vararg parameters: TypeName = emptyArray(),
        returnType: TypeName
    ): LambdaTypeName {
      return LambdaTypeName(
          receiver,
          parameters.toList().map { ParameterSpec.unnamed(it) },
          returnType)
    }

    /** Returns a lambda type with `returnType` and parameters listed in `parameters`. */
    @JvmStatic fun get(
        receiver: TypeName? = null,
        vararg parameters: ParameterSpec = emptyArray(),
        returnType: TypeName
    ) = LambdaTypeName(receiver, parameters.toList(), returnType)
  }
}

class ParameterizedTypeName internal constructor(
    private val enclosingType: TypeName?,
    val rawType: ClassName,
    typeArguments: List<TypeName>,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  val typeArguments = typeArguments.toImmutableList()

  init {
    require(typeArguments.isNotEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override fun asNullable()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, true, annotations)

  override fun asNonNull()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>) = ParameterizedTypeName(
      enclosingType, rawType, typeArguments, nullable, this.annotations + annotations)

  override fun withoutAnnotations()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, nullable)

  fun plusParameter(typeArgument: TypeName) = ParameterizedTypeName(enclosingType, rawType,
      typeArguments + typeArgument, nullable, annotations)

  fun plusParameter(typeArgument: KClass<*>) = plusParameter(typeArgument.asClassName())

  fun plusParameter(typeArgument: Class<*>) = plusParameter(typeArgument.asClassName())

  override fun emit(out: CodeWriter): CodeWriter {
    if (enclosingType != null) {
      enclosingType.emitAnnotations(out)
      enclosingType.emit(out)
      out.emit("." + rawType.simpleName)
    } else {
      rawType.emitAnnotations(out)
      rawType.emit(out)
    }
    if (typeArguments.isNotEmpty()) {
      out.emit("<")
      typeArguments.forEachIndexed { index, parameter ->
        if (index > 0) out.emit(", ")
        parameter.emitAnnotations(out)
        parameter.emit(out)
        parameter.emitNullable(out)
      }
      out.emit(">")
    }
    return out
  }

  /**
   * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested inside this
   * class, with the specified `typeArguments`.
   */
  fun nestedClass(name: String, typeArguments: List<TypeName>)
      = ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments)

  companion object {
    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun ClassName.parameterizedBy(vararg typeArguments: TypeName)
        = ParameterizedTypeName(null, this, typeArguments.toList())

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun KClass<*>.parameterizedBy(vararg typeArguments: KClass<*>)
        = ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun Class<*>.parameterizedBy(vararg typeArguments: Type) =
        ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArgument` to `this`.  */
    @JvmStatic @JvmName("get") fun ClassName.plusParameter(typeArgument: TypeName) =
        parameterizedBy(typeArgument)

    /** Returns a parameterized type, applying `typeArgument` to `this`.  */
    @JvmStatic @JvmName("get") fun KClass<*>.plusParameter(typeArgument: KClass<*>) =
        parameterizedBy(typeArgument)

    /** Returns a parameterized type, applying `typeArgument` to `this`.  */
    @JvmStatic @JvmName("get") fun Class<*>.plusParameter(typeArgument: Class<*>) =
        parameterizedBy(typeArgument)

    /** Returns a parameterized type equivalent to `type`.  */
    internal fun get(
        type: ParameterizedType,
        map: MutableMap<Type, TypeVariableName>
    ): ParameterizedTypeName {
      val rawType = (type.rawType as Class<*>).asClassName()
      val ownerType = if (type.ownerType is ParameterizedType
          && !java.lang.reflect.Modifier.isStatic((type.rawType as Class<*>).modifiers))
        type.ownerType as ParameterizedType else
        null

      val typeArguments = type.actualTypeArguments.map { TypeName.get(it, map = map) }
      return if (ownerType != null)
        get(ownerType, map = map).nestedClass(rawType.simpleName, typeArguments) else
        ParameterizedTypeName(null, rawType, typeArguments)
    }

    /** Returns a type name equivalent to type with given list of type arguments.  */
    internal fun get(type: KClass<*>, nullable: Boolean, typeArguments: List<KTypeProjection>): TypeName {
      if (typeArguments.isEmpty()) {
        return type.asTypeName()
      }

      val effectiveType = if (type.java.isArray) Array<Unit>::class else type
      val enclosingClass = type.java.enclosingClass?.kotlin

      return ParameterizedTypeName(
          enclosingClass?.let {
            get(it, false, typeArguments.drop(effectiveType.typeParameters.size))
          },
          effectiveType.asTypeName(),
          typeArguments.take(effectiveType.typeParameters.size).map { (paramVariance, paramType) ->
            val typeName = paramType?.asTypeName() ?: return@map STAR
            when (paramVariance) {
              null -> STAR
              KVariance.INVARIANT -> typeName
              KVariance.IN -> WildcardTypeName.supertypeOf(typeName)
              KVariance.OUT -> WildcardTypeName.subtypeOf(typeName)
            }
          },
          nullable,
          effectiveType.annotations.map { AnnotationSpec.get(it) })
    }
  }
}

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
import java.lang.reflect.Modifier.isStatic
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Collections
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
import kotlin.reflect.typeOf

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
sealed class TypeName constructor(
  val isNullable: Boolean,
  annotations: List<AnnotationSpec>,
  internal val tagMap: TagMap
) : Taggable by tagMap {
  val annotations = annotations.toImmutableList()

  /** Lazily-initialized toString of this type name.  */
  private val cachedString: String by lazy {
    buildCodeString {
      emitAnnotations(this)
      emit(this)
      if (isNullable) emit("?")
    }
  }

  abstract fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
    tags: Map<KClass<*>, Any> = this.tagMap.tags.toMap()
  ): TypeName

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
    if (isNullable) {
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
          val enclosing = if (enclosingType.kind != TypeKind.NONE &&
              Modifier.STATIC !in t.asElement().modifiers)
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

        override fun visitTypeVariable(
          t: javax.lang.model.type.TypeVariable,
          p: Void?
        ): TypeName {
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
@JvmField val STRING = ClassName("kotlin", "String")
@JvmField val CHAR_SEQUENCE = ClassName("kotlin", "CharSequence")
@JvmField val COMPARABLE = ClassName("kotlin", "Comparable")
@JvmField val THROWABLE = ClassName("kotlin", "Throwable")
@JvmField val ANNOTATION = ClassName("kotlin", "Annotation")
@JvmField val NOTHING = ClassName("kotlin", "Nothing")
@JvmField val NUMBER = ClassName("kotlin", "Number")
@JvmField val ITERABLE = ClassName("kotlin.collections", "Iterable")
@JvmField val COLLECTION = ClassName("kotlin.collections", "Collection")
@JvmField val LIST = ClassName("kotlin.collections", "List")
@JvmField val SET = ClassName("kotlin.collections", "Set")
@JvmField val MAP = ClassName("kotlin.collections", "Map")
@JvmField val MAP_ENTRY = MAP.nestedClass("Entry")
@JvmField val MUTABLE_ITERABLE = ClassName("kotlin.collections", "MutableIterable")
@JvmField val MUTABLE_COLLECTION = ClassName("kotlin.collections", "MutableCollection")
@JvmField val MUTABLE_LIST = ClassName("kotlin.collections", "MutableList")
@JvmField val MUTABLE_SET = ClassName("kotlin.collections", "MutableSet")
@JvmField val MUTABLE_MAP = ClassName("kotlin.collections", "MutableMap")
@JvmField val MUTABLE_MAP_ENTRY = MUTABLE_MAP.nestedClass("Entry")
@JvmField val BOOLEAN_ARRAY = ClassName("kotlin", "BooleanArray")
@JvmField val BYTE_ARRAY = ClassName("kotlin", "ByteArray")
@JvmField val CHAR_ARRAY = ClassName("kotlin", "CharArray")
@JvmField val SHORT_ARRAY = ClassName("kotlin", "ShortArray")
@JvmField val INT_ARRAY = ClassName("kotlin", "IntArray")
@JvmField val LONG_ARRAY = ClassName("kotlin", "LongArray")
@JvmField val FLOAT_ARRAY = ClassName("kotlin", "FloatArray")
@JvmField val DOUBLE_ARRAY = ClassName("kotlin", "DoubleArray")
@JvmField val ENUM = ClassName("kotlin", "Enum")
@JvmField val U_BYTE = ClassName("kotlin", "UByte")
@JvmField val U_SHORT = ClassName("kotlin", "UShort")
@JvmField val U_INT = ClassName("kotlin", "UInt")
@JvmField val U_LONG = ClassName("kotlin", "ULong")
@JvmField val U_BYTE_ARRAY = ClassName("kotlin", "UByteArray")
@JvmField val U_SHORT_ARRAY = ClassName("kotlin", "UShortArray")
@JvmField val U_INT_ARRAY = ClassName("kotlin", "UIntArray")
@JvmField val U_LONG_ARRAY = ClassName("kotlin", "ULongArray")

/** The wildcard type `*` which is shorthand for `out Any?`. */
@JvmField val STAR = WildcardTypeName.producerOf(ANY.copy(nullable = true))

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

@ExperimentalStdlibApi
inline fun <reified T> typeNameOf(): TypeName = typeOf<T>().asTypeName()

/** A fully-qualified class name for top-level and member classes.  */
class ClassName internal constructor(
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
  constructor(packageName: String, simpleName: String, vararg simpleNames: String) :
      this(listOf(packageName, simpleName, *simpleNames))

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  constructor(packageName: String, vararg simpleNames: String) :
      this(listOf(packageName, *simpleNames)) {
    require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
  }

  /**
   * Returns a class name created from the given parts. For example, calling this with package name
   * `"java.util"` and simple names `"Map"`, `"Entry"` yields `Map.Entry`.
   */
  constructor(packageName: String, simpleNames: List<String>) :
      this(mutableListOf(packageName).apply { addAll(simpleNames) }) {
    require(simpleNames.isNotEmpty()) { "simpleNames must not be empty" }
  }

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

  /**
   * Callable reference to the constructor of this class. Emits the enclosing class if one exists,
   * followed by the reference operator `::`, followed by either [simpleName] or the
   * fully-qualified name if this is a top-level class.
   *
   * Note: As `::$packageName.$simpleName` is not valid syntax, an aliased import may be required
   * for a top-level class with a conflicting name.
   */
  fun constructorReference(): CodeBlock {
    val enclosing = enclosingClassName()
    return if (enclosing != null) {
      CodeBlock.of("%T::%N", enclosing, simpleName)
    } else {
      CodeBlock.of("::%T", this)
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

  override fun emit(out: CodeWriter) = out.emit(out.lookupName(this).escapeSegmentsIfNecessary())

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

object Dynamic : TypeName(false, emptyList(), TagMap(emptyMap())) {

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ) = throw UnsupportedOperationException("dynamic doesn't support copying")

  override fun emit(out: CodeWriter) = out.apply {
    emit("dynamic")
  }
}

class LambdaTypeName private constructor(
  val receiver: TypeName? = null,
  parameters: List<ParameterSpec> = emptyList(),
  val returnType: TypeName = UNIT,
  nullable: Boolean = false,
  val isSuspending: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap()
) : TypeName(nullable, annotations, TagMap(tags)) {
  val parameters = parameters.toImmutableList()

  init {
    for (param in parameters) {
      require(param.annotations.isEmpty()) { "Parameters with annotations are not allowed" }
      require(param.modifiers.isEmpty()) { "Parameters with modifiers are not allowed" }
      require(param.defaultValue == null) { "Parameters with default values are not allowed" }
    }
  }

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): LambdaTypeName {
    return copy(nullable, annotations, this.isSuspending, tags)
  }

  fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
    suspending: Boolean = this.isSuspending,
    tags: Map<KClass<*>, Any> = this.tagMap.tags.toMap()
  ): LambdaTypeName {
    return LambdaTypeName(receiver, parameters, returnType, nullable, suspending, annotations, tags)
  }

  override fun emit(out: CodeWriter): CodeWriter {
    emitAnnotations(out)

    if (isNullable) {
      out.emit("(")
    }

    if (isSuspending) {
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

    if (isNullable) {
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
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap()
) : TypeName(nullable, annotations, TagMap(tags)) {
  val typeArguments = typeArguments.toImmutableList()

  init {
    require(typeArguments.isNotEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): ParameterizedTypeName {
    return ParameterizedTypeName(enclosingType, rawType, typeArguments, nullable, annotations, tags)
  }

  fun plusParameter(typeArgument: TypeName) = ParameterizedTypeName(enclosingType, rawType,
      typeArguments + typeArgument, isNullable, annotations)

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
  fun nestedClass(name: String, typeArguments: List<TypeName>) =
      ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments)

  companion object {
    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun ClassName.parameterizedBy(vararg typeArguments: TypeName) =
        ParameterizedTypeName(null, this, typeArguments.toList())

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun KClass<*>.parameterizedBy(vararg typeArguments: KClass<*>) =
        ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun Class<*>.parameterizedBy(vararg typeArguments: Type) =
        ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun ClassName.parameterizedBy(typeArguments: List<TypeName>) =
        ParameterizedTypeName(null, this, typeArguments)

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun KClass<*>.parameterizedBy(typeArguments: Iterable<KClass<*>>) =
        ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`.  */
    @JvmStatic @JvmName("get") fun Class<*>.parameterizedBy(typeArguments: Iterable<Type>) =
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
      val ownerType = if (type.ownerType is ParameterizedType &&
          !isStatic((type.rawType as Class<*>).modifiers))
        type.ownerType as ParameterizedType else
        null

      val typeArguments = type.actualTypeArguments.map { TypeName.get(it, map = map) }
      return if (ownerType != null)
        get(ownerType, map = map).nestedClass(rawType.simpleName, typeArguments) else
        ParameterizedTypeName(null, rawType, typeArguments)
    }

    /** Returns a type name equivalent to type with given list of type arguments.  */
    internal fun get(
      type: KClass<*>,
      nullable: Boolean,
      typeArguments: List<KTypeProjection>
    ): TypeName {
      if (typeArguments.isEmpty()) {
        return type.asTypeName().run { if (nullable) copy(nullable = true) else this }
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
              KVariance.IN -> WildcardTypeName.consumerOf(typeName)
              KVariance.OUT -> WildcardTypeName.producerOf(typeName)
            }
          },
          nullable,
          effectiveType.annotations.map { AnnotationSpec.get(it) })
    }
  }
}

class TypeVariableName private constructor(
  val name: String,
  val bounds: List<TypeName>,

  /** Either [KModifier.IN], [KModifier.OUT], or null. */
  val variance: KModifier? = null,
  val isReified: Boolean = false,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap()
) : TypeName(nullable, annotations, TagMap(tags)) {

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): TypeVariableName {
    return copy(nullable, annotations, this.bounds, this.isReified, tags)
  }

  fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
    bounds: List<TypeName> = this.bounds.toList(),
    reified: Boolean = this.isReified,
    tags: Map<KClass<*>, Any> = this.tagMap.tags
  ): TypeVariableName {
    return TypeVariableName(name, bounds.withoutImplicitBound(), variance, reified, nullable,
        annotations, tags)
  }

  private fun List<TypeName>.withoutImplicitBound(): List<TypeName> {
    return if (size == 1) this else filterNot { it == NULLABLE_ANY }
  }

  override fun emit(out: CodeWriter) = out.emit(name)

  companion object {
    internal fun of(
      name: String,
      bounds: List<TypeName>,
      variance: KModifier?
    ): TypeVariableName {
      require(variance == null || variance.isOneOf(KModifier.IN, KModifier.OUT)) {
        "$variance is an invalid variance modifier, the only allowed values are in and out!"
      }
      require(bounds.isNotEmpty()) {
        "$name has no bounds"
      }
      // Strip Any? from bounds if it is present.
      return TypeVariableName(name, bounds, variance)
    }

    /** Returns type variable named `name` with `variance` and without bounds.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, variance: KModifier? = null) =
        TypeVariableName.of(
            name = name,
            bounds = NULLABLE_ANY_LIST,
            variance = variance
        )

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: TypeName, variance: KModifier? = null) =
        TypeVariableName.of(
            name = name,
            bounds = bounds.toList().ifEmpty(::NULLABLE_ANY_LIST),
            variance = variance
        )

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: KClass<*>, variance: KModifier? = null) =
        TypeVariableName.of(
            name = name,
            bounds = bounds.map(KClass<*>::asTypeName).ifEmpty(::NULLABLE_ANY_LIST),
            variance = variance
        )

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: Type, variance: KModifier? = null) =
        TypeVariableName.of(
            name = name,
            bounds = bounds.map(Type::asTypeName).ifEmpty(::NULLABLE_ANY_LIST),
            variance = variance
        )

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, bounds: List<TypeName>, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.ifEmpty(::NULLABLE_ANY_LIST), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("getWithClasses") @JvmOverloads
    operator fun invoke(name: String, bounds: Iterable<KClass<*>>, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.map { it.asTypeName() }.ifEmpty(::NULLABLE_ANY_LIST), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("getWithTypes") @JvmOverloads
    operator fun invoke(name: String, bounds: Iterable<Type>, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.map { it.asTypeName() }.ifEmpty(::NULLABLE_ANY_LIST), variance)

    /**
     * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
     * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
     * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
     * map before looking up the bounds. Then if we encounter this TypeVariable again while
     * constructing the bounds, we can just return it from the map. And, the code that put the entry
     * in `variables` will make sure that the bounds are filled in before returning.
     */
    internal fun get(
      mirror: javax.lang.model.type.TypeVariable,
      typeVariables: MutableMap<TypeParameterElement, TypeVariableName>
    ): TypeVariableName {
      val element = mirror.asElement() as TypeParameterElement
      var typeVariableName: TypeVariableName? = typeVariables[element]
      if (typeVariableName == null) {
        // Since the bounds field is public, we need to make it an unmodifiableList. But we control
        // the List that that wraps, which means we can change it before returning.
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
        typeVariables[element] = typeVariableName
        for (typeMirror in element.bounds) {
          bounds += TypeName.get(typeMirror, typeVariables)
        }
        bounds.remove(ANY)
        bounds.remove(JAVA_OBJECT)
        if (bounds.isEmpty()) {
          bounds.add(NULLABLE_ANY)
        }
      }
      return typeVariableName
    }

    /** Returns type variable equivalent to `type`.  */
    internal fun get(
      type: TypeVariable<*>,
      map: MutableMap<Type, TypeVariableName> = mutableMapOf()
    ): TypeVariableName {
      var result: TypeVariableName? = map[type]
      if (result == null) {
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        result = TypeVariableName(type.name, visibleBounds)
        map[type] = result
        for (bound in type.bounds) {
          bounds += TypeName.get(bound, map)
        }
        bounds.remove(ANY)
        bounds.remove(JAVA_OBJECT)
        if (bounds.isEmpty()) {
          bounds.add(NULLABLE_ANY)
        }
      }
      return result
    }

    internal val NULLABLE_ANY_LIST = listOf(NULLABLE_ANY)
    private val JAVA_OBJECT = ClassName("java.lang", "Object")
  }
}

class WildcardTypeName private constructor(
  outTypes: List<TypeName>,
  inTypes: List<TypeName>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap()
) : TypeName(nullable, annotations, TagMap(tags)) {
  val outTypes = outTypes.toImmutableList()
  val inTypes = inTypes.toImmutableList()

  init {
    require(this.outTypes.size == 1) { "unexpected out types: $outTypes" }
  }

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): WildcardTypeName {
    return WildcardTypeName(outTypes, inTypes, nullable, annotations, tags)
  }

  override fun emit(out: CodeWriter): CodeWriter {
    return when {
      inTypes.size == 1 -> out.emitCode("in %T", inTypes[0])
      outTypes == STAR.outTypes -> out.emit("*")
      else -> out.emitCode("out %T", outTypes[0])
    }
  }

  companion object {
    /**
     * Returns a type that represents an unknown type that produces `outType`. For example, if
     * `outType` is `CharSequence`, this returns `out CharSequence`. If `outType` is `Any?`, this
     * returns `*`, which is shorthand for `out Any?`.
     */
    @JvmStatic fun producerOf(outType: TypeName) = WildcardTypeName(listOf(outType), emptyList())

    @JvmStatic fun producerOf(outType: Type) = producerOf(outType.asTypeName())

    @JvmStatic fun producerOf(outType: KClass<*>) = producerOf(outType.asTypeName())

    /**
     * Returns a type that represents an unknown type that consumes `inType`. For example, if
     * `inType` is `String`, this returns `in String`.
     */
    @JvmStatic fun consumerOf(inType: TypeName) = WildcardTypeName(listOf(ANY), listOf(inType))

    @JvmStatic fun consumerOf(inType: Type) = consumerOf(inType.asTypeName())

    @JvmStatic fun consumerOf(inType: KClass<*>) = consumerOf(inType.asTypeName())

    internal fun get(
      mirror: javax.lang.model.type.WildcardType,
      typeVariables: Map<TypeParameterElement, TypeVariableName>
    ): TypeName {
      val outType = mirror.extendsBound
      if (outType == null) {
        val inType = mirror.superBound
        return if (inType == null) {
          STAR
        } else {
          consumerOf(TypeName.get(inType, typeVariables))
        }
      } else {
        return producerOf(TypeName.get(outType, typeVariables))
      }
    }

    internal fun get(
      wildcardName: WildcardType,
      map: MutableMap<Type, TypeVariableName>
    ): TypeName {
      return WildcardTypeName(
          wildcardName.upperBounds.map { TypeName.get(it, map = map) },
          wildcardName.lowerBounds.map { TypeName.get(it, map = map) })
    }
  }
}

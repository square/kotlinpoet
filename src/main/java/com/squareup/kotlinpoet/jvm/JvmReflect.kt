@file:JvmName("JvmReflect")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.squareup.kotlinpoet.jvm

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import java.lang.annotation.Annotation
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.Arrays
import java.util.Collections
import java.util.Objects

/** Returns a [TypeName] equivalent to this [Type].  */
@JvmName("asTypeName")
fun Type.asTypeName() = asTypeName(this, mutableMapOf())

private fun asTypeName(type: Type, map: MutableMap<Type, TypeVariableName>): TypeName {
  when (type) {
    is Class<*> -> {
      when {
        type === Void.TYPE -> return UNIT
        type === Boolean::class.javaPrimitiveType -> return BOOLEAN
        type === Byte::class.javaPrimitiveType -> return BYTE
        type === Short::class.javaPrimitiveType -> return SHORT
        type === Int::class.javaPrimitiveType -> return INT
        type === Long::class.javaPrimitiveType -> return LONG
        type === Char::class.javaPrimitiveType -> return CHAR
        type === Float::class.javaPrimitiveType -> return FLOAT
        type === Double::class.javaPrimitiveType -> return DOUBLE
        type.isArray -> return ParameterizedTypeName.get(ARRAY, asTypeName(type.componentType, map))
        else -> return type.asClassName()
      }
    }
    is ParameterizedType -> return asParameterizedTypeName(type, map)
    is WildcardType -> return asWildcardTypeName(type, map)
    is TypeVariable<*> -> return asTypeVariableName(type, map)
    is GenericArrayType -> return ParameterizedTypeName.get(ARRAY,
        asTypeName(type.genericComponentType, map))
    else -> throw IllegalArgumentException("unexpected type: " + type)
  }
}

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

@JvmOverloads
fun Annotation.asAnnotationSpec(includeDefaultValues: Boolean = false): AnnotationSpec {
  try {
    val builder = AnnotationSpec.builder(annotationType().kotlin)
    val methods = annotationType().declaredMethods
    Arrays.sort(methods, { m1, m2 -> m1.name.compareTo(m2.name) })
    for (method in methods) {
      val value = method.invoke(this)
      if (!includeDefaultValues) {
        if (Objects.deepEquals(value, method.defaultValue)) {
          continue
        }
      }
      if (value.javaClass.isArray) {
        for (i in 0 until Array.getLength(value)) {
          builder.addMemberForValue(method.name, Array.get(value, i))
        }
        continue
      }
      if (value is Annotation) {
        builder.addMember(method.name, "%L", value.asAnnotationSpec())
        continue
      }
      builder.addMemberForValue(method.name, value)
    }
    return builder.build()
  } catch (e: Exception) {
    throw RuntimeException("Reflecting $this failed!", e)
  }
}

fun WildcardType.asWildcardTypeName() = asWildcardTypeName(this, mutableMapOf())

private fun asWildcardTypeName(
    wildcardName: WildcardType,
    map: MutableMap<Type, TypeVariableName>)
    : TypeName {
  return WildcardTypeName(
      wildcardName.upperBounds.map { asTypeName(it, map = map) },
      wildcardName.lowerBounds.map { asTypeName(it, map = map) })
}

private fun asTypeVariableName(
    type: java.lang.reflect.TypeVariable<*>,
    map: MutableMap<Type, TypeVariableName> = mutableMapOf())
    : TypeVariableName {
  var result: TypeVariableName? = map[type]
  if (result == null) {
    val bounds = mutableListOf<TypeName>()
    val visibleBounds = Collections.unmodifiableList(bounds)
    result = TypeVariableName(type.name, visibleBounds)
    map.put(type, result)
    for (bound in type.bounds) {
      bounds += asTypeName(bound, map)
    }
    bounds.remove(ANY)
  }
  return result
}

/** Returns a parameterized type equivalent to `type`.  */
fun ParameterizedType.asParameterizedTypeName() = asParameterizedTypeName(this, mutableMapOf())

/** Returns a parameterized type equivalent to `type`.  */
private fun asParameterizedTypeName(
    type: ParameterizedType,
    map: MutableMap<Type, TypeVariableName>): ParameterizedTypeName {
  val rawType = (type.rawType as Class<*>).asClassName()
  val ownerType = if (type.ownerType is ParameterizedType
      && !Modifier.isStatic((type.rawType as Class<*>).modifiers))
    type.ownerType as ParameterizedType else
    null

  val typeArguments = type.actualTypeArguments.map { asTypeName(it, map = map) }
  return if (ownerType != null)
    asParameterizedTypeName(ownerType, map = map).nestedClass(rawType.simpleName(), typeArguments) else
    ParameterizedTypeName(null, rawType, typeArguments)
}

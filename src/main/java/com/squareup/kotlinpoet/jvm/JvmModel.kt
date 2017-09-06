@file:JvmName("JvmModel")

package com.squareup.kotlinpoet.jvm

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.Builder
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.FINAL
import com.squareup.kotlinpoet.KModifier.VARARG
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import java.util.Collections
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind.MEMBER
import javax.lang.model.element.NestingKind.TOP_LEVEL
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.NoType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.SimpleAnnotationValueVisitor7
import javax.lang.model.util.SimpleTypeVisitor7
import javax.lang.model.util.Types

/** Returns a [TypeName] equivalent to this [TypeMirror]. */
fun TypeMirror.asTypeName() = asTypeName(this, mutableMapOf())

private fun asTypeName(
    mirror: TypeMirror,
    typeVariables: MutableMap<TypeParameterElement, TypeVariableName>)
    : TypeName {
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
          && !t.asElement().modifiers.contains(Modifier.STATIC))
        enclosingType.accept(this, null) else
        null
      if (t.typeArguments.isEmpty() && enclosing !is ParameterizedTypeName) {
        return rawType
      }

      val typeArgumentNames = mutableListOf<TypeName>()
      for (typeArgument in t.typeArguments) {
        typeArgumentNames += asTypeName(typeArgument, typeVariables)
      }
      return if (enclosing is ParameterizedTypeName)
        enclosing.nestedClass(rawType.simpleName(), typeArgumentNames) else
        ParameterizedTypeName(null, rawType, typeArgumentNames)
    }

    override fun visitError(t: ErrorType, p: Void?): TypeName {
      return visitDeclared(t, p)
    }

    override fun visitArray(t: ArrayType, p: Void?): ParameterizedTypeName {
      return ParameterizedTypeName.get(ARRAY, asTypeName(t.componentType, typeVariables))
    }

    override fun visitTypeVariable(t: javax.lang.model.type.TypeVariable, p: Void?): TypeName {
      return asTypeVariableName(t, typeVariables.toMutableMap())
    }

    override fun visitWildcard(t: javax.lang.model.type.WildcardType, p: Void?): TypeName {
      return asWildcardTypeName(t, typeVariables)
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

/** Returns the class name for `element`.  */
fun TypeElement.asClassName(): ClassName {
  val names = mutableListOf<String>()
  var e: Element = this
  while (isClassOrInterface(e)) {
    val eType = e as TypeElement
    require(eType.nestingKind == TOP_LEVEL || eType.nestingKind == MEMBER) {
      "unexpected type testing"
    }
    names += eType.simpleName.toString()
    e = eType.enclosingElement
  }
  names += getPackage(this).qualifiedName.toString()
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

fun AnnotationMirror.asAnnotationSpec(): AnnotationSpec {
  val element = annotationType.asElement() as TypeElement
  val builder = AnnotationSpec.builder(element.asClassName())
  val visitor = Visitor(builder)
  for (executableElement in elementValues.keys) {
    val name = executableElement.simpleName.toString()
    val value = elementValues[executableElement]!!
    value.accept(visitor, name)
  }
  return builder.build()
}

private class Visitor internal constructor(internal val builder: Builder)
  : SimpleAnnotationValueVisitor7<Builder, String>(builder) {

  override fun defaultAction(o: Any, name: String)
      = builder.addMemberForValue(name, o)

  override fun visitAnnotation(a: AnnotationMirror, name: String)
      = builder.addMember(name, "%L", a.asAnnotationSpec())

  override fun visitEnumConstant(c: VariableElement, name: String)
      = builder.addMember(name, "%T.%L", c.asType(), c.simpleName)

  override fun visitType(t: TypeMirror, name: String)
      = builder.addMember(name, "%T::class", t)

  override fun visitArray(values: List<AnnotationValue>, name: String): Builder {
    for (value in values) {
      value.accept(this, name)
    }
    return builder
  }
}

fun javax.lang.model.type.WildcardType.asWildcardTypeName()
    = asWildcardTypeName(this, mutableMapOf())

private fun asWildcardTypeName(
    mirror: javax.lang.model.type.WildcardType,
    typeVariables: MutableMap<TypeParameterElement, TypeVariableName>)
    : TypeName {
  val extendsBound = mirror.extendsBound
  if (extendsBound == null) {
    val superBound = mirror.superBound
    return if (superBound == null)
      WildcardTypeName.subtypeOf(ANY) else
      WildcardTypeName.supertypeOf(asTypeName(superBound, typeVariables))
  } else {
    return WildcardTypeName.subtypeOf(asTypeName(extendsBound, typeVariables))
  }
}

/** Returns type variable equivalent to `mirror`.  */
fun TypeVariable.asTypeVariableName()
    = (asElement() as TypeParameterElement).asTypeVariableName()

/** Returns type variable equivalent to `element`.  */
fun TypeParameterElement.asTypeVariableName(): TypeVariableName {
  val name = simpleName.toString()
  val boundsTypeNames = bounds.map { it.asTypeName() }
  return TypeVariableName.of(name, boundsTypeNames, variance = null)
}

/**
 * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
 * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
 * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
 * map before looking up the bounds. Then if we encounter this TypeVariable again while
 * constructing the bounds, we can just return it from the map. And, the code that put the entry
 * in `variables` will make sure that the bounds are filled in before returning.
 */
private fun asTypeVariableName(
    mirror: TypeVariable,
    typeVariables: MutableMap<TypeParameterElement, TypeVariableName>): TypeVariableName {
  val element = mirror.asElement() as TypeParameterElement
  var typeVariableName: TypeVariableName? = typeVariables[element]
  if (typeVariableName == null) {
    // Since the bounds field is public, we need to make it an unmodifiableList. But we control
    // the List that that wraps, which means we can change it before returning.
    val bounds = mutableListOf<TypeName>()
    val visibleBounds = Collections.unmodifiableList(bounds)
    typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
    typeVariables.put(element, typeVariableName)
    for (typeMirror in element.bounds) {
      bounds += asTypeName(typeMirror, typeVariables)
    }
    bounds.remove(ANY)
  }
  return typeVariableName
}

fun VariableElement.asParameterSpec(): ParameterSpec {
  val name = simpleName.toString()
  val type = asType().asTypeName()
  val builder = ParameterSpec.builder(name, type)
  for (modifier in modifiers) {
    when (modifier) {
      Modifier.FINAL -> builder.addModifiers(FINAL)
      else -> throw IllegalArgumentException("unexpected parameter modifier $modifier")
    }
  }
  return builder.build()
}

fun ExecutableElement.parameterSpecs() = parameters.map { it.asParameterSpec() }


/**
 * Returns a new fun spec builder that overrides `method`.
 *
 * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
 * throws declarations. An `override` modifier will be added.
 */
fun overriding(method: ExecutableElement): FunSpec.Builder {
  var modifiers: MutableSet<Modifier> = method.modifiers
  require(Modifier.PRIVATE !in modifiers
      && Modifier.FINAL !in modifiers
      && Modifier.STATIC !in modifiers) {
    "cannot override method with modifiers: $modifiers"
  }

  val methodName = method.simpleName.toString()
  val funBuilder = FunSpec.builder(methodName)

  funBuilder.addModifiers(KModifier.OVERRIDE)

  modifiers = modifiers.toMutableSet()
  modifiers.remove(Modifier.ABSTRACT)

  var visibility = KModifier.INTERNAL
  for (modifier in modifiers) {
    when (modifier) {
      Modifier.PUBLIC -> visibility = KModifier.PUBLIC
      Modifier.PROTECTED -> visibility = KModifier.PROTECTED
      Modifier.PRIVATE -> visibility = KModifier.PRIVATE
      Modifier.ABSTRACT -> funBuilder.addModifiers(KModifier.ABSTRACT)
      Modifier.FINAL -> funBuilder.addModifiers(KModifier.FINAL)
      Modifier.NATIVE -> funBuilder.addModifiers(KModifier.EXTERNAL)
      Modifier.DEFAULT -> Unit
      Modifier.STATIC -> funBuilder.addAnnotation(JvmStatic::class)
      Modifier.SYNCHRONIZED -> funBuilder.addAnnotation(Synchronized::class)
      Modifier.STRICTFP -> funBuilder.addAnnotation(Strictfp::class)
      else -> throw IllegalArgumentException("unexpected fun modifier $modifier")
    }
  }
  funBuilder.addModifiers(visibility)

  method.typeParameters
      .map { it.asType() as TypeVariable }
      .map { it.asTypeVariableName() }
      .forEach { funBuilder.addTypeVariable(it) }

  funBuilder.returns(method.returnType.asTypeName())
  funBuilder.addParameters(method.parameterSpecs())
  if (method.isVarArgs) {
    funBuilder.parameters[funBuilder.parameters.lastIndex] = funBuilder.parameters.last()
        .toBuilder()
        .addModifiers(VARARG)
        .build()
  }

  if (method.thrownTypes.isNotEmpty()) {
    val throwsValueString = method.thrownTypes.joinToString { "%T::class" }
    funBuilder.addAnnotation(AnnotationSpec.builder(Throws::class)
        .addMember("value", throwsValueString, *method.thrownTypes.toTypedArray())
        .build())
  }

  return funBuilder
}

/**
 * Returns a new function spec builder that overrides `method` as a member of `enclosing`. This
 * will resolve type parameters: for example overriding [Comparable.compareTo] in a type that
 * implements `Comparable<Movie>`, the `T` parameter will be resolved to `Movie`.
 *
 * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
 * throws declarations. An `override` modifier will be added.
 */
fun overriding(
    method: ExecutableElement, enclosing: DeclaredType, types: Types): FunSpec.Builder {
  val executableType = types.asMemberOf(enclosing, method) as ExecutableType
  val resolvedParameterTypes = executableType.parameterTypes
  val resolvedReturnType = executableType.returnType

  val builder = overriding(method)
  builder.returns(resolvedReturnType.asTypeName())
  var i = 0
  val size = builder.parameters.size
  while (i < size) {
    val parameter = builder.parameters[i]
    val type = resolvedParameterTypes[i].asTypeName()
    builder.parameters[i] = parameter.toBuilder(parameter.name, type).build()
    i++
  }

  return builder
}

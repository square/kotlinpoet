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
package com.squareup.javapoet;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor7;
import kotlin.Unit;

/**
 * Any type in Java's type system, plus {@code void}. This class is an identifier for primitive
 * types like {@code int} and raw reference types like {@code String} and {@code List}. It also
 * identifies composite types like {@code char[]} and {@code Set<Long>}.
 *
 * <p>Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for {@code java.lang.List} doesn't know about the {@code size()} method, the fact that
 * lists are collections, or even that it accepts a single type parameter.
 *
 * <p>Instances of this class are immutable value objects that implement {@code equals()} and {@code
 * hashCode()} properly.
 *
 * <h3>Referencing existing types</h3>
 *
 * <p>In an annotation processor you can get a type name instance for a type mirror by calling
 * {@link #get(TypeMirror)}. In reflection code, you can use {@link #get(Type)}.
 *
 * <h3>Defining new types</h3>
 *
 * <p>Create new reference types like {@code com.example.HelloWorld} with {@link
 * ClassName#get(String, String, String...)}. To build composite types like {@code char[]} and
 * {@code Set<Long>}, use the factory methods on {@link ArrayTypeName}, {@link
 * ParameterizedTypeName}, {@link TypeVariableName}, and {@link WildcardTypeName}.
 */
public abstract class TypeName {
  public static final ClassName ANY = ClassName.get("kotlin", "Any");
  static final ClassName UNIT = ClassName.get(Unit.class);
  static final ClassName BOOLEAN = ClassName.get("kotlin", "Boolean");
  static final ClassName BYTE = ClassName.get("kotlin", "Byte");
  static final ClassName SHORT = ClassName.get("kotlin", "Short");
  static final ClassName INT = ClassName.get("kotlin", "Int");
  static final ClassName LONG = ClassName.get("kotlin", "Long");
  static final ClassName CHAR = ClassName.get("kotlin", "Char");
  static final ClassName FLOAT = ClassName.get("kotlin", "Float");
  static final ClassName DOUBLE = ClassName.get("kotlin", "Double");

  /** Lazily-initialized toString of this type name. */
  private String cachedString;

  TypeName() {
    // No external subclasses.
  }

  @Override public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public final int hashCode() {
    return toString().hashCode();
  }

  @Override public final String toString() {
    String result = cachedString;
    if (result == null) {
      try {
        StringBuilder resultBuilder = new StringBuilder();
        CodeWriter codeWriter = new CodeWriter(resultBuilder);
        emit(codeWriter);
        result = resultBuilder.toString();
        cachedString = result;
      } catch (IOException e) {
        throw new AssertionError();
      }
    }
    return result;
  }

  abstract CodeWriter emit(CodeWriter out) throws IOException;

  /** Returns a type name equivalent to {@code mirror}. */
  public static TypeName get(TypeMirror mirror) {
    return get(mirror, new LinkedHashMap<TypeParameterElement, TypeVariableName>());
  }

  static TypeName get(TypeMirror mirror,
      final Map<TypeParameterElement, TypeVariableName> typeVariables) {
    return mirror.accept(new SimpleTypeVisitor7<TypeName, Void>() {
      @Override public TypeName visitPrimitive(PrimitiveType t, Void p) {
        switch (t.getKind()) {
          case BOOLEAN:
            return BOOLEAN;
          case BYTE:
            return BYTE;
          case SHORT:
            return SHORT;
          case INT:
            return INT;
          case LONG:
            return LONG;
          case CHAR:
            return CHAR;
          case FLOAT:
            return FLOAT;
          case DOUBLE:
            return DOUBLE;
          default:
            throw new AssertionError();
        }
      }

      @Override public TypeName visitDeclared(DeclaredType t, Void p) {
        ClassName rawType = ClassName.get((TypeElement) t.asElement());
        TypeMirror enclosingType = t.getEnclosingType();
        TypeName enclosing =
            (enclosingType.getKind() != TypeKind.NONE)
                    && !t.asElement().getModifiers().contains(Modifier.STATIC)
                ? enclosingType.accept(this, null)
                : null;
        if (t.getTypeArguments().isEmpty() && !(enclosing instanceof ParameterizedTypeName)) {
          return rawType;
        }

        List<TypeName> typeArgumentNames = new ArrayList<>();
        for (TypeMirror mirror : t.getTypeArguments()) {
          typeArgumentNames.add(get(mirror, typeVariables));
        }
        return enclosing instanceof ParameterizedTypeName
            ? ((ParameterizedTypeName) enclosing).nestedClass(
            rawType.simpleName(), typeArgumentNames)
            : new ParameterizedTypeName(null, rawType, typeArgumentNames);
      }

      @Override public TypeName visitError(ErrorType t, Void p) {
        return visitDeclared(t, p);
      }

      @Override public ArrayTypeName visitArray(ArrayType t, Void p) {
        return ArrayTypeName.get(t, typeVariables);
      }

      @Override public TypeName visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
        return TypeVariableName.get(t, typeVariables);
      }

      @Override public TypeName visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
        return WildcardTypeName.get(t, typeVariables);
      }

      @Override public TypeName visitNoType(NoType t, Void p) {
        if (t.getKind() == TypeKind.VOID) return UNIT;
        return super.visitUnknown(t, p);
      }

      @Override protected TypeName defaultAction(TypeMirror e, Void p) {
        throw new IllegalArgumentException("Unexpected type mirror: " + e);
      }
    }, null);
  }

  /** Returns a type name equivalent to {@code type}. */
  public static TypeName get(Type type) {
    return get(type, new LinkedHashMap<Type, TypeVariableName>());
  }

  static TypeName get(Type type, Map<Type, TypeVariableName> map) {
    if (type instanceof Class<?>) {
      Class<?> classType = (Class<?>) type;
      if (type == void.class) return UNIT;
      if (type == boolean.class) return BOOLEAN;
      if (type == byte.class) return BYTE;
      if (type == short.class) return SHORT;
      if (type == int.class) return INT;
      if (type == long.class) return LONG;
      if (type == char.class) return CHAR;
      if (type == float.class) return FLOAT;
      if (type == double.class) return DOUBLE;
      if (classType.isArray()) return ArrayTypeName.of(get(classType.getComponentType(), map));
      return ClassName.get(classType);

    } else if (type instanceof ParameterizedType) {
      return ParameterizedTypeName.get((ParameterizedType) type, map);

    } else if (type instanceof WildcardType) {
      return WildcardTypeName.get((WildcardType) type, map);

    } else if (type instanceof TypeVariable<?>) {
      return TypeVariableName.get((TypeVariable<?>) type, map);

    } else if (type instanceof GenericArrayType) {
      return ArrayTypeName.get((GenericArrayType) type, map);

    } else {
      throw new IllegalArgumentException("unexpected type: " + type);
    }
  }

  /** Converts an array of types to a list of type names. */
  static List<TypeName> list(Type[] types) {
    return list(types, new LinkedHashMap<Type, TypeVariableName>());
  }

  static List<TypeName> list(Type[] types, Map<Type, TypeVariableName> map) {
    List<TypeName> result = new ArrayList<>(types.length);
    for (Type type : types) {
      result.add(get(type, map));
    }
    return result;
  }

  /** Returns the array component of {@code type}, or null if {@code type} is not an array. */
  static TypeName arrayComponent(TypeName type) {
    return type instanceof ArrayTypeName
        ? ((ArrayTypeName) type).componentType
        : null;
  }
}

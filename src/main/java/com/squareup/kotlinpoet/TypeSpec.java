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
package com.squareup.kotlinpoet;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import kotlin.reflect.KClass;

import static com.squareup.kotlinpoet.TypeNameKt.OBJECT;
import static com.squareup.kotlinpoet.Util.checkArgument;
import static com.squareup.kotlinpoet.Util.checkNotNull;
import static com.squareup.kotlinpoet.Util.checkState;
import static com.squareup.kotlinpoet.Util.hasDefaultModifier;
import static com.squareup.kotlinpoet.Util.requireExactlyOneOf;

/** A generated class, interface, or enum declaration. */
public final class TypeSpec {
  public final Kind kind;
  public final String name;
  public final CodeBlock anonymousTypeArguments;
  public final CodeBlock kdoc;
  public final List<AnnotationSpec> annotations;
  public final Set<Modifier> modifiers;
  public final List<TypeVariableName> typeVariables;
  public final TypeName superclass;
  public final List<TypeName> superinterfaces;
  public final Map<String, TypeSpec> enumConstants;
  public final List<PropertySpec> propertySpecs;
  public final CodeBlock staticBlock;
  public final CodeBlock initializerBlock;
  public final List<FunSpec> funSpecs;
  public final List<TypeSpec> typeSpecs;
  public final List<Element> originatingElements;

  private TypeSpec(Builder builder) {
    this.kind = builder.kind;
    this.name = builder.name;
    this.anonymousTypeArguments = builder.anonymousTypeArguments;
    this.kdoc = builder.kdoc.build();
    this.annotations = Util.immutableList(builder.annotations);
    this.modifiers = Util.immutableSet(builder.modifiers);
    this.typeVariables = Util.immutableList(builder.typeVariables);
    this.superclass = builder.superclass;
    this.superinterfaces = Util.immutableList(builder.superinterfaces);
    this.enumConstants = Util.immutableMap(builder.enumConstants);
    this.propertySpecs = Util.immutableList(builder.propertySpecs);
    this.staticBlock = builder.staticBlock.build();
    this.initializerBlock = builder.initializerBlock.build();
    this.funSpecs = Util.immutableList(builder.funSpecs);
    this.typeSpecs = Util.immutableList(builder.typeSpecs);

    List<Element> originatingElementsMutable = new ArrayList<>();
    originatingElementsMutable.addAll(builder.originatingElements);
    for (TypeSpec typeSpec : builder.typeSpecs) {
      originatingElementsMutable.addAll(typeSpec.originatingElements);
    }
    this.originatingElements = Util.immutableList(originatingElementsMutable);
  }

  public boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  public static Builder classBuilder(String name) {
    return new Builder(Kind.CLASS, checkNotNull(name, "name == null"), null);
  }

  public static Builder classBuilder(ClassName className) {
    return classBuilder(checkNotNull(className, "className == null").simpleName());
  }

  public static Builder interfaceBuilder(String name) {
    return new Builder(Kind.INTERFACE, checkNotNull(name, "name == null"), null);
  }

  public static Builder interfaceBuilder(ClassName className) {
    return interfaceBuilder(checkNotNull(className, "className == null").simpleName());
  }

  public static Builder enumBuilder(String name) {
    return new Builder(Kind.ENUM, checkNotNull(name, "name == null"), null);
  }

  public static Builder enumBuilder(ClassName className) {
    return enumBuilder(checkNotNull(className, "className == null").simpleName());
  }

  public static Builder anonymousClassBuilder(String typeArgumentsFormat, Object... args) {
    return new Builder(Kind.CLASS, null, CodeBlock.builder()
        .add(typeArgumentsFormat, args)
        .build());
  }

  public static Builder annotationBuilder(String name) {
    return new Builder(Kind.ANNOTATION, checkNotNull(name, "name == null"), null);
  }

  public static Builder annotationBuilder(ClassName className) {
    return annotationBuilder(checkNotNull(className, "className == null").simpleName());
  }

  public Builder toBuilder() {
    Builder builder = new Builder(kind, name, anonymousTypeArguments);
    builder.kdoc.add(kdoc);
    builder.annotations.addAll(annotations);
    builder.modifiers.addAll(modifiers);
    builder.typeVariables.addAll(typeVariables);
    builder.superclass = superclass;
    builder.superinterfaces.addAll(superinterfaces);
    builder.enumConstants.putAll(enumConstants);
    builder.propertySpecs.addAll(propertySpecs);
    builder.funSpecs.addAll(funSpecs);
    builder.typeSpecs.addAll(typeSpecs);
    builder.initializerBlock.add(initializerBlock);
    builder.staticBlock.add(staticBlock);
    return builder;
  }

  void emit(CodeWriter codeWriter, String enumName, Set<Modifier> implicitModifiers)
      throws IOException {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    int previousStatementLine = codeWriter.getStatementLine();
    codeWriter.setStatementLine(-1);

    try {
      if (enumName != null) {
        codeWriter.emitKdoc(kdoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emit("%L", enumName);
        if (!anonymousTypeArguments.formatParts.isEmpty()) {
          codeWriter.emit("(");
          codeWriter.emit(anonymousTypeArguments);
          codeWriter.emit(")");
        }
        if (propertySpecs.isEmpty() && funSpecs.isEmpty() && typeSpecs.isEmpty()) {
          return; // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n");
      } else if (anonymousTypeArguments != null) {
        TypeName supertype = !superinterfaces.isEmpty() ? superinterfaces.get(0) : superclass;
        codeWriter.emit("new %T(", supertype);
        codeWriter.emit(anonymousTypeArguments);
        codeWriter.emit(") {\n");
      } else {
        codeWriter.emitKdoc(kdoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emitModifiers(modifiers, Util.union(implicitModifiers, kind.asMemberModifiers));
        if (kind == Kind.ANNOTATION) {
          codeWriter.emit("%L %L", "@interface", name);
        } else {
          codeWriter.emit("%L %L", kind.name().toLowerCase(Locale.US), name);
        }
        codeWriter.emitTypeVariables(typeVariables);

        List<TypeName> extendsTypes;
        List<TypeName> implementsTypes;
        if (kind == Kind.INTERFACE) {
          extendsTypes = superinterfaces;
          implementsTypes = Collections.emptyList();
        } else {
          extendsTypes = superclass.equals(OBJECT)
              ? Collections.<TypeName>emptyList()
              : Collections.singletonList(superclass);
          implementsTypes = superinterfaces;
        }

        if (!extendsTypes.isEmpty()) {
          codeWriter.emit(" extends");
          boolean firstType = true;
          for (TypeName type : extendsTypes) {
            if (!firstType) codeWriter.emit(",");
            codeWriter.emit(" %T", type);
            firstType = false;
          }
        }

        if (!implementsTypes.isEmpty()) {
          codeWriter.emit(" implements");
          boolean firstType = true;
          for (TypeName type : implementsTypes) {
            if (!firstType) codeWriter.emit(",");
            codeWriter.emit(" %T", type);
            firstType = false;
          }
        }

        codeWriter.emit(" {\n");
      }

      codeWriter.pushType(this);
      codeWriter.indent();
      boolean firstMember = true;
      for (Iterator<Map.Entry<String, TypeSpec>> i = enumConstants.entrySet().iterator();
          i.hasNext(); ) {
        Map.Entry<String, TypeSpec> enumConstant = i.next();
        if (!firstMember) codeWriter.emit("\n");
        enumConstant.getValue()
            .emit(codeWriter, enumConstant.getKey(), Collections.<Modifier>emptySet());
        firstMember = false;
        if (i.hasNext()) {
          codeWriter.emit(",\n");
        } else if (!propertySpecs.isEmpty() || !funSpecs.isEmpty() || !typeSpecs.isEmpty()) {
          codeWriter.emit(";\n");
        } else {
          codeWriter.emit("\n");
        }
      }

      // Static properties.
      for (PropertySpec propertySpec : propertySpecs) {
        if (!propertySpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers);
        firstMember = false;
      }

      if (!staticBlock.isEmpty()) {
        if (!firstMember) codeWriter.emit("\n");
        codeWriter.emit(staticBlock);
        firstMember = false;
      }

      // Non-static properties.
      for (PropertySpec propertySpec : propertySpecs) {
        if (propertySpec.hasModifier(Modifier.STATIC)) continue;
        if (!firstMember) codeWriter.emit("\n");
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers);
        firstMember = false;
      }

      // Initializer block.
      if (!initializerBlock.isEmpty()) {
        if (!firstMember) codeWriter.emit("\n");
        codeWriter.emit(initializerBlock);
        firstMember = false;
      }

      // Constructors.
      for (FunSpec funSpec : funSpecs) {
        if (!funSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers);
        firstMember = false;
      }

      // Functions (static and non-static).
      for (FunSpec funSpec : funSpecs) {
        if (funSpec.isConstructor()) continue;
        if (!firstMember) codeWriter.emit("\n");
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers);
        firstMember = false;
      }

      // Types.
      for (TypeSpec typeSpec : typeSpecs) {
        if (!firstMember) codeWriter.emit("\n");
        typeSpec.emit(codeWriter, null, kind.implicitTypeModifiers);
        firstMember = false;
      }

      codeWriter.unindent();
      codeWriter.popType();

      codeWriter.emit("}");
      if (enumName == null && anonymousTypeArguments == null) {
        codeWriter.emit("\n"); // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.setStatementLine(previousStatementLine);
    }
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    return toString().equals(o.toString());
  }

  @Override public int hashCode() {
    return toString().hashCode();
  }

  @Override public String toString() {
    StringWriter out = new StringWriter();
    try {
      CodeWriter codeWriter = new CodeWriter(out);
      emit(codeWriter, null, Collections.<Modifier>emptySet());
      return out.toString();
    } catch (IOException e) {
      throw new AssertionError();
    }
  }

  public enum Kind {
    CLASS(
        Collections.<Modifier>emptySet(),
        Collections.<Modifier>emptySet(),
        Collections.<Modifier>emptySet(),
        Collections.<Modifier>emptySet()),

    INTERFACE(
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC)),
        Util.immutableSet(Arrays.asList(Modifier.STATIC))),

    ENUM(
        Collections.<Modifier>emptySet(),
        Collections.<Modifier>emptySet(),
        Collections.<Modifier>emptySet(),
        Collections.singleton(Modifier.STATIC)),

    ANNOTATION(
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT)),
        Util.immutableSet(Arrays.asList(Modifier.PUBLIC, Modifier.STATIC)),
        Util.immutableSet(Arrays.asList(Modifier.STATIC)));

    private final Set<Modifier> implicitPropertyModifiers;
    private final Set<Modifier> implicitFunctionModifiers;
    private final Set<Modifier> implicitTypeModifiers;
    private final Set<Modifier> asMemberModifiers;

    Kind(Set<Modifier> implicitPropertyModifiers,
        Set<Modifier> implicitFunctionModifiers,
        Set<Modifier> implicitTypeModifiers,
        Set<Modifier> asMemberModifiers) {
      this.implicitPropertyModifiers = implicitPropertyModifiers;
      this.implicitFunctionModifiers = implicitFunctionModifiers;
      this.implicitTypeModifiers = implicitTypeModifiers;
      this.asMemberModifiers = asMemberModifiers;
    }
  }

  public static final class Builder {
    private final Kind kind;
    private final String name;
    private final CodeBlock anonymousTypeArguments;

    private final CodeBlock.Builder kdoc = CodeBlock.builder();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<TypeVariableName> typeVariables = new ArrayList<>();
    private TypeName superclass = OBJECT;
    private final List<TypeName> superinterfaces = new ArrayList<>();
    private final Map<String, TypeSpec> enumConstants = new LinkedHashMap<>();
    private final List<PropertySpec> propertySpecs = new ArrayList<>();
    private final CodeBlock.Builder staticBlock = CodeBlock.builder();
    private final CodeBlock.Builder initializerBlock = CodeBlock.builder();
    private final List<FunSpec> funSpecs = new ArrayList<>();
    private final List<TypeSpec> typeSpecs = new ArrayList<>();
    private final List<Element> originatingElements = new ArrayList<>();

    private Builder(Kind kind, String name,
        CodeBlock anonymousTypeArguments) {
      checkArgument(name == null || SourceVersion.isName(name), "not a valid name: %s", name);
      this.kind = kind;
      this.name = name;
      this.anonymousTypeArguments = anonymousTypeArguments;
    }

    public Builder addKdoc(String format, Object... args) {
      kdoc.add(format, args);
      return this;
    }

    public Builder addKdoc(CodeBlock block) {
      kdoc.add(block);
      return this;
    }

    public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
      checkArgument(annotationSpecs != null, "annotationSpecs == null");
      for (AnnotationSpec annotationSpec : annotationSpecs) {
        this.annotations.add(annotationSpec);
      }
      return this;
    }

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(ClassName annotation) {
      return addAnnotation(AnnotationSpec.builder(annotation).build());
    }

    public Builder addAnnotation(Class<?> annotation) {
      return addAnnotation(ClassName.get(annotation));
    }

    public Builder addAnnotation(KClass<?> annotation) {
      return addAnnotation((ClassName) ClassName.get(annotation));
    }

    public Builder addModifiers(Modifier... modifiers) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      Collections.addAll(this.modifiers, modifiers);
      return this;
    }

    public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      checkArgument(typeVariables != null, "typeVariables == null");
      for (TypeVariableName typeVariable : typeVariables) {
        this.typeVariables.add(typeVariable);
      }
      return this;
    }

    public Builder addTypeVariable(TypeVariableName typeVariable) {
      checkState(anonymousTypeArguments == null, "forbidden on anonymous types.");
      typeVariables.add(typeVariable);
      return this;
    }

    public Builder superclass(TypeName superclass) {
      checkState(this.kind == Kind.CLASS, "only classes have super classes, not " + this.kind);
      checkState(this.superclass == OBJECT,
          "superclass already set to " + this.superclass);
      this.superclass = superclass;
      return this;
    }

    public Builder superclass(Type superclass) {
      return superclass(TypeName.get(superclass));
    }

    public Builder addSuperinterfaces(Iterable<? extends TypeName> superinterfaces) {
      checkArgument(superinterfaces != null, "superinterfaces == null");
      for (TypeName superinterface : superinterfaces) {
        addSuperinterface(superinterface);
      }
      return this;
    }

    public Builder addSuperinterface(TypeName superinterface) {
      checkArgument(superinterface != null, "superinterface == null");
      this.superinterfaces.add(superinterface);
      return this;
    }

    public Builder addSuperinterface(Type superinterface) {
      return addSuperinterface(TypeName.get(superinterface));
    }

    public Builder addSuperinterface(KClass<?> superinterface) {
      return addSuperinterface(TypeName.get(superinterface));
    }

    public Builder addEnumConstant(String name) {
      return addEnumConstant(name, anonymousClassBuilder("").build());
    }

    public Builder addEnumConstant(String name, TypeSpec typeSpec) {
      checkState(kind == Kind.ENUM, "%s is not enum", this.name);
      checkArgument(typeSpec.anonymousTypeArguments != null,
          "enum constants must have anonymous type arguments");
      checkArgument(SourceVersion.isName(name), "not a valid enum constant: %s", name);
      enumConstants.put(name, typeSpec);
      return this;
    }

    public Builder addProperties(Iterable<PropertySpec> propertySpecs) {
      checkArgument(propertySpecs != null, "propertySpecs == null");
      for (PropertySpec propertySpec : propertySpecs) {
        addProperty(propertySpec);
      }
      return this;
    }

    public Builder addProperty(PropertySpec propertySpec) {
      if (kind == Kind.INTERFACE || kind == Kind.ANNOTATION) {
        requireExactlyOneOf(propertySpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE);
        Set<Modifier> check = EnumSet.of(Modifier.STATIC, Modifier.FINAL);
        checkState(propertySpec.modifiers.containsAll(check), "%s %s.%s requires modifiers %s",
            kind, name, propertySpec.name, check);
      }
      propertySpecs.add(propertySpec);
      return this;
    }

    public Builder addProperty(TypeName type, String name, Modifier... modifiers) {
      return addProperty(PropertySpec.builder(type, name, modifiers).build());
    }

    public Builder addProperty(Type type, String name, Modifier... modifiers) {
      return addProperty(TypeName.get(type), name, modifiers);
    }

    public Builder addProperty(KClass<?> type, String name, Modifier... modifiers) {
      return addProperty(TypeName.get(type), name, modifiers);
    }

    public Builder addStaticBlock(CodeBlock block) {
      staticBlock.beginControlFlow("static").add(block).endControlFlow();
      return this;
    }

    public Builder addInitializerBlock(CodeBlock block) {
      if ((kind != Kind.CLASS && kind != Kind.ENUM)) {
        throw new UnsupportedOperationException(kind + " can't have initializer blocks");
      }
      initializerBlock.add("{\n")
          .indent()
          .add(block)
          .unindent()
          .add("}\n");
      return this;
    }

    public Builder addFunctions(Iterable<FunSpec> funSpecs) {
      checkArgument(funSpecs != null, "funSpecs == null");
      for (FunSpec funSpec : funSpecs) {
        addFun(funSpec);
      }
      return this;
    }

    public Builder addFun(FunSpec funSpec) {
      if (kind == Kind.INTERFACE) {
        requireExactlyOneOf(funSpec.modifiers, Modifier.ABSTRACT, Modifier.STATIC, Util.DEFAULT);
        requireExactlyOneOf(funSpec.modifiers, Modifier.PUBLIC, Modifier.PRIVATE);
      } else if (kind == Kind.ANNOTATION) {
        checkState(funSpec.modifiers.equals(kind.implicitFunctionModifiers),
            "%s %s.%s requires modifiers %s",
            kind, name, funSpec.name, kind.implicitFunctionModifiers);
      }
      if (kind != Kind.ANNOTATION) {
        checkState(funSpec.defaultValue == null, "%s %s.%s cannot have a default value",
            kind, name, funSpec.name);
      }
      if (kind != Kind.INTERFACE) {
        checkState(!hasDefaultModifier(funSpec.modifiers), "%s %s.%s cannot be default",
            kind, name, funSpec.name);
      }
      funSpecs.add(funSpec);
      return this;
    }

    public Builder addTypes(Iterable<TypeSpec> typeSpecs) {
      checkArgument(typeSpecs != null, "typeSpecs == null");
      for (TypeSpec typeSpec : typeSpecs) {
        addType(typeSpec);
      }
      return this;
    }

    public Builder addType(TypeSpec typeSpec) {
      checkArgument(typeSpec.modifiers.containsAll(kind.implicitTypeModifiers),
          "%s %s.%s requires modifiers %s", kind, name, typeSpec.name,
          kind.implicitTypeModifiers);
      typeSpecs.add(typeSpec);
      return this;
    }

    public Builder addOriginatingElement(Element originatingElement) {
      originatingElements.add(originatingElement);
      return this;
    }

    public TypeSpec build() {
      checkArgument(kind != Kind.ENUM || !enumConstants.isEmpty(),
          "at least one enum constant is required for %s", name);

      boolean isAbstract = modifiers.contains(Modifier.ABSTRACT) || kind != Kind.CLASS;
      for (FunSpec funSpec : funSpecs) {
        checkArgument(isAbstract || !funSpec.hasModifier(Modifier.ABSTRACT),
            "non-abstract type %s cannot declare abstract function %s", name, funSpec.name);
      }

      boolean superclassIsObject = superclass.equals(OBJECT);
      int interestingSupertypeCount = (superclassIsObject ? 0 : 1) + superinterfaces.size();
      checkArgument(anonymousTypeArguments == null || interestingSupertypeCount <= 1,
          "anonymous type has too many supertypes");

      return new TypeSpec(this);
    }
  }
}

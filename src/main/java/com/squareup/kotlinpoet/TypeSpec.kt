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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.ANNOTATION
import com.squareup.kotlinpoet.KModifier.COMPANION
import com.squareup.kotlinpoet.KModifier.ENUM
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.EXTERNAL
import com.squareup.kotlinpoet.KModifier.PUBLIC
import com.squareup.kotlinpoet.TypeSpec.Kind.Interface
import com.squareup.kotlinpoet.TypeSpec.Kind.Object
import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A generated class, interface, or enum declaration.  */
class TypeSpec private constructor(builder: TypeSpec.Builder) {
  val kind = builder.kind
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val annotationSpecs = builder.annotationSpecs.toImmutableList()
  val modifiers = kind.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val primaryConstructor = builder.primaryConstructor
  val superclass = builder.superclass
  val superclassConstructorParameters = builder.superclassConstructorParameters.toImmutableList()

  val isEnum = builder.isEnum
  val isAnnotation = builder.isAnnotation
  val isCompanion = builder.isCompanion
  val isAnonymousClass = builder.isAnonymousClass

  /**
   * Map of superinterfaces - entries with a null value represent a regular superinterface (with
   * no delegation), while non-null [CodeBlock] values represent delegates
   * for the corresponding [TypeSpec] interface (key) value
   */
  val superinterfaces = builder.superinterfaces.toImmutableMap()
  val enumConstants = builder.enumConstants.toImmutableMap()
  val propertySpecs = builder.propertySpecs.toImmutableList()
  val initializerBlock = builder.initializerBlock.build()
  val funSpecs = builder.funSpecs.toImmutableList()
  val typeSpecs = builder.typeSpecs.toImmutableList()

  fun toBuilder(): Builder {
    val builder = Builder(kind, name)
    builder.kdoc.add(kdoc)
    builder.annotationSpecs += annotationSpecs
    builder.typeVariables += typeVariables
    builder.superclass = superclass
    builder.superclassConstructorParameters += superclassConstructorParameters
    builder.enumConstants += enumConstants
    builder.propertySpecs += propertySpecs
    builder.funSpecs += funSpecs
    builder.typeSpecs += typeSpecs
    builder.initializerBlock.add(initializerBlock)
    builder.superinterfaces.putAll(superinterfaces)
    builder.primaryConstructor = primaryConstructor
    return builder
  }

  internal fun emit(codeWriter: CodeWriter, enumName: String?, isNestedExternal: Boolean = false) {
    // Nested classes interrupt wrapped line indentation. Stash the current wrapping state and put
    // it back afterwards when this type is complete.
    val previousStatementLine = codeWriter.statementLine
    codeWriter.statementLine = -1

    val constructorProperties: Map<String, PropertySpec> = constructorProperties()
    val superclassConstructorParametersBlock = superclassConstructorParameters.joinToCode()

    try {
      if (enumName != null) {
        codeWriter.emitKdoc(kdocWithConstructorParameters())
        codeWriter.emitAnnotations(annotationSpecs, false)
        codeWriter.emitCode("%L", enumName)
        if (superclassConstructorParametersBlock.isNotEmpty()) {
          codeWriter.emit("(")
          codeWriter.emitCode(superclassConstructorParametersBlock)
          codeWriter.emit(")")
        }
        if (hasNoBody) {
          return // Avoid unnecessary braces "{}".
        }
        codeWriter.emit(" {\n")
      } else if (isAnonymousClass) {

        codeWriter.emitCode("object")
        val supertype = if (superclass != ANY) {
          listOf(CodeBlock.of(" %T(%L)", superclass, superclassConstructorParametersBlock))
        } else {
          listOf()
        }

        val allSuperTypes = supertype + if (superinterfaces.isNotEmpty())
          superinterfaces.keys.map { CodeBlock.of(" %T", it) } else
          emptyList()

        if (allSuperTypes.isNotEmpty()) {
          codeWriter.emitCode(" :")
          codeWriter.emitCode(allSuperTypes.joinToCode(","))
        }
        if (hasNoBody) {
          codeWriter.emit(" {\n}")
          return
        }
        codeWriter.emit(" {\n")
      } else {
        codeWriter.emitKdoc(kdocWithConstructorParameters())
        codeWriter.emitAnnotations(annotationSpecs, false)
        codeWriter.emitModifiers(kind.modifiers,
            if (isNestedExternal) setOf(PUBLIC, EXTERNAL) else setOf(PUBLIC))
        codeWriter.emit(kind.declarationKeyword)
        if (name != null) {
          codeWriter.emitCode(" %L", escapeIfNecessary(name))
        }
        codeWriter.emitTypeVariables(typeVariables)

        primaryConstructor?.let {
          codeWriter.pushType(this) // avoid name collisions when emitting primary constructor
          var useKeyword = false
          var emittedAnnotations = false

          if (it.annotations.isNotEmpty()) {
            codeWriter.emit(" ")
            codeWriter.emitAnnotations(it.annotations, true)
            useKeyword = true
            emittedAnnotations = true
          }

          if (it.modifiers.isNotEmpty()) {
            if (!emittedAnnotations) codeWriter.emit(" ")
            codeWriter.emitModifiers(it.modifiers)
            useKeyword = true
          }

          if (useKeyword) {
            codeWriter.emit("constructor")
          }

          val forceNewLines = it.parameters
              .any { param -> constructorProperties[param.name]?.kdoc?.isNotEmpty() == true }

          it.parameters.emit(codeWriter, forceNewLines = forceNewLines) { param ->
            val property = constructorProperties[param.name]
            if (property != null) {
              property.emit(codeWriter, setOf(PUBLIC), withInitializer = false, inline = true)
              param.emitDefaultValue(codeWriter)
            } else {
              param.emit(codeWriter)
            }
          }
          codeWriter.popType()
        }

        val types = listOf(superclass).filter { it != ANY }.map {
          if (primaryConstructor != null || funSpecs.none { it.isConstructor }) {
            CodeBlock.of("%T(%L)", it, superclassConstructorParametersBlock)
          } else {
            CodeBlock.of("%T", it)
          }
        }
        val superTypes = types + superinterfaces.entries.map { (type, init) ->
            if (init == null)  CodeBlock.of("%T", type) else CodeBlock.of("%T by $init", type)
        }

        if (superTypes.isNotEmpty()) {
          codeWriter.emitCode(superTypes.joinToCode(separator = ",%W", prefix = " : "))
        }

        codeWriter.emitWhereBlock(typeVariables)

        if (hasNoBody) {
          codeWriter.emit("\n")
          return // Avoid unnecessary braces "{}".
        }
        if (!isAnnotation) {
          codeWriter.emit(" {\n")
        }
      }

      codeWriter.pushType(this)
      codeWriter.indent()
      var firstMember = true
      val i = enumConstants.entries.iterator()
      while (i.hasNext()) {
        val enumConstant = i.next()
        if (!firstMember) codeWriter.emit("\n")
        enumConstant.value
            .emit(codeWriter, enumConstant.key)
        firstMember = false
        if (i.hasNext()) {
          codeWriter.emit(",\n")
        } else if (propertySpecs.isNotEmpty() || funSpecs.isNotEmpty() || typeSpecs.isNotEmpty()) {
          codeWriter.emit(";\n")
        } else {
          codeWriter.emit("\n")
        }
      }

      // Properties.
      for (propertySpec in propertySpecs) {
        if (constructorProperties.containsKey(propertySpec.name)) {
          continue
        }
        if (!firstMember) codeWriter.emit("\n")
        propertySpec.emit(codeWriter, kind.implicitPropertyModifiers)
        firstMember = false
      }

      if (primaryConstructor != null && primaryConstructor.body.isNotEmpty()) {
        codeWriter.emit("init {\n")
        codeWriter.indent()
        codeWriter.emitCode(primaryConstructor.body)
        codeWriter.unindent()
        codeWriter.emit("}\n")
      }

      // Initializer block.
      if (initializerBlock.isNotEmpty()) {
        if (!firstMember) codeWriter.emit("\n")
        codeWriter.emitCode(initializerBlock)
        firstMember = false
      }

      // Constructors.
      for (funSpec in funSpecs) {
        if (!funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers, false)
        firstMember = false
      }

      // Functions.
      for (funSpec in funSpecs) {
        if (funSpec.isConstructor) continue
        if (!firstMember) codeWriter.emit("\n")
        funSpec.emit(codeWriter, name, kind.implicitFunctionModifiers, true)
        firstMember = false
      }

      // Types.
      val areNestedExternal = EXTERNAL in kind.modifiers || isNestedExternal

      for (typeSpec in typeSpecs) {
        if (!firstMember) codeWriter.emit("\n")
        typeSpec.emit(codeWriter, null, isNestedExternal = areNestedExternal)
        firstMember = false
      }

      codeWriter.unindent()
      codeWriter.popType()

      if (!isAnnotation) {
        codeWriter.emit("}")
      }
      if (enumName == null && !isAnonymousClass) {
        codeWriter.emit("\n") // If this type isn't also a value, include a trailing newline.
      }
    } finally {
      codeWriter.statementLine = previousStatementLine
    }
  }

  /** Returns the properties that can be declared inline as constructor parameters. */
  private fun constructorProperties(): Map<String, PropertySpec> {
    if (primaryConstructor == null) return emptyMap()

    val result: MutableMap<String, PropertySpec> = LinkedHashMap()
    for (property in propertySpecs) {
      val parameter = primaryConstructor.parameter(property.name) ?: continue
      if (parameter.type != property.type) continue
      if (CodeBlock.of("%N", parameter) != property.initializer) continue
      result[property.name] = property.toBuilder()
          .addAnnotations(parameter.annotations)
          .addModifiers(*parameter.modifiers.toTypedArray())
          .build()
    }
    return result
  }

  /** Returns KDoc comments including those of primary constructor parameters. */
  private fun kdocWithConstructorParameters(): CodeBlock {
    if (primaryConstructor == null || primaryConstructor.parameters.isEmpty()) {
      return kdoc
    }

    val constructorProperties = constructorProperties()

    return with(kdoc.toBuilder()) {
      for (parameterSpec in primaryConstructor.parameters) {
        val kdoc = parameterSpec.kdoc.takeUnless { it.isEmpty() }
                ?: constructorProperties[parameterSpec.name]?.kdoc?.takeUnless { it.isEmpty() }
        if (kdoc != null) {
          add("@param %L %L", parameterSpec.name, kdoc)
        }
      }
      build()
    }
  }

  private val hasNoBody: Boolean
    get() {
      if (isAnnotation) {
        return true
      }
      if (propertySpecs.isNotEmpty()) {
        val constructorProperties = constructorProperties()
        for (propertySpec in propertySpecs) {
          if (!constructorProperties.containsKey(propertySpec.name)) {
            return false
          }
        }
      }
      return enumConstants.isEmpty() &&
          initializerBlock.isEmpty() &&
          (primaryConstructor?.body?.isEmpty() ?: true) &&
          funSpecs.isEmpty() &&
          typeSpecs.isEmpty()
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { emit(CodeWriter(this), null) }

  sealed class Kind(
    internal val declarationKeyword: String,
    internal val defaultImplicitPropertyModifiers: Set<KModifier>,
    internal val defaultImplicitFunctionModifiers: Set<KModifier>,
    internal val modifiers: Set<KModifier> = emptySet()
  ) {

    internal val implicitPropertyModifiers get() = defaultImplicitPropertyModifiers + when {
        ANNOTATION in modifiers -> emptySet()
        EXPECT in modifiers -> setOf(EXPECT)
        EXTERNAL in modifiers -> setOf(EXTERNAL)
        else -> emptySet()
      }

    internal val implicitFunctionModifiers get() = defaultImplicitFunctionModifiers + when {
      ANNOTATION in modifiers -> setOf(ABSTRACT)
      EXPECT in modifiers -> setOf(EXPECT)
      EXTERNAL in modifiers -> setOf(EXTERNAL)
      else -> emptySet()
    }

    abstract fun plusModifiers(vararg modifiers: KModifier): Kind

    override fun toString() = javaClass.simpleName.toUpperCase()

    class Class(vararg modifiers: KModifier) : Kind(
        "class",
        setOf(PUBLIC),
        setOf(PUBLIC),
        modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: KModifier) =
        Class(*(this.modifiers.toTypedArray() + modifiers))
    }

    class Object(vararg modifiers: KModifier) : Kind(
        "object",
        setOf(PUBLIC),
        setOf(PUBLIC),
        modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: KModifier) =
          Object(*(this.modifiers.toTypedArray() + modifiers))
    }

    class Interface(vararg modifiers: KModifier): Kind(
        "interface",
        setOf(PUBLIC),
        setOf(PUBLIC, ABSTRACT),
        modifiers.toSet()) {

      override fun plusModifiers(vararg modifiers: KModifier) =
          Interface(*(this.modifiers.toTypedArray() + modifiers))
    }
  }

  class Builder internal constructor(
    internal var kind: Kind,
    internal val name: String?
  ) {
    internal val kdoc = CodeBlock.builder()
    internal var primaryConstructor: FunSpec? = null
    internal var superclass: TypeName = ANY
    internal val initializerBlock = CodeBlock.builder()
    internal val isAnonymousClass get() = name == null && kind is Kind.Class
    internal val isEnum get() = kind is Kind.Class && ENUM in kind.modifiers
    internal val isAnnotation get() = kind is Kind.Class && ANNOTATION in kind.modifiers
    internal val isCompanion get() = kind is Kind.Object && COMPANION in kind.modifiers
    internal val isSimpleClass = kind is Kind.Class && !isEnum && !isAnnotation

    val superinterfaces = mutableMapOf<TypeName, CodeBlock?>()
    val enumConstants = mutableMapOf<String, TypeSpec>()
    val annotationSpecs = mutableListOf<AnnotationSpec>()
    val typeVariables = mutableListOf<TypeVariableName>()
    val superclassConstructorParameters = mutableListOf<CodeBlock>()
    val propertySpecs = mutableListOf<PropertySpec>()
    val funSpecs = mutableListOf<FunSpec>()
    val typeSpecs = mutableListOf<TypeSpec>()

    init {
      require(name == null || name.isName) { "not a valid name: $name" }
    }

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
      this.annotationSpecs += annotationSpecs
    }

    fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
      annotationSpecs += annotationSpec
    }

    fun addAnnotation(annotation: ClassName)
        = addAnnotation(AnnotationSpec.builder(annotation).build())

    fun addAnnotation(annotation: Class<*>) = addAnnotation(annotation.asClassName())

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(annotation.asClassName())

    fun addModifiers(vararg modifiers: KModifier) = apply {
      check(!isAnonymousClass) { "forbidden on anonymous types." }
      kind = kind.plusModifiers(*modifiers)
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    fun primaryConstructor(primaryConstructor: FunSpec?) = apply {
      check(kind is Kind.Class) {
        "$kind can't have initializer blocks"
      }
      if (primaryConstructor != null) {
        require(primaryConstructor.isConstructor) {
          "expected a constructor but was ${primaryConstructor.name}"
        }
      }
      this.primaryConstructor = primaryConstructor
    }

    fun superclass(superclass: TypeName) = apply {
      checkCanHaveSuperclass()
      check(this.superclass === ANY) { "superclass already set to ${this.superclass}" }
      this.superclass = superclass
    }

    private fun checkCanHaveSuperclass() {
      check(isSimpleClass || kind is Object) {
        "only classes can have super classes, not $kind"
      }
    }

    fun superclass(superclass: Type) = superclass(superclass.asTypeName())

    fun superclass(superclass: KClass<*>) = superclass(superclass.asTypeName())

    fun addSuperclassConstructorParameter(format: String, vararg args: Any) = apply {
      addSuperclassConstructorParameter(CodeBlock.of(format, *args))
    }

    fun addSuperclassConstructorParameter(codeBlock: CodeBlock) = apply {
      checkCanHaveSuperclass()
      this.superclassConstructorParameters += codeBlock
    }

    fun addSuperinterfaces(superinterfaces: Iterable<TypeName>) = apply {
      this.superinterfaces.putAll(superinterfaces.map {
        Pair(it, null)
      })
    }

    fun addSuperinterface(superinterface: TypeName, delegate: CodeBlock = CodeBlock.EMPTY) = apply {
      if (delegate.isEmpty()) {
        this.superinterfaces.put(superinterface, null)
      } else {
        require(isSimpleClass) {
          "delegation only allowed for classes (found $kind '$name')"
        }
        require(!superinterface.nullable) {
          "expected non-nullable type but was '${superinterface.asNonNullable()}'"
        }
        require(this.superinterfaces[superinterface] == null) {
          "'$name' can not delegate to $superinterface by $delegate with existing declaration by " +
              "${this.superinterfaces[superinterface]}"
        }
        this.superinterfaces[superinterface] = delegate
      }
    }

    fun addSuperinterface(superinterface: Type, delegate: CodeBlock = CodeBlock.EMPTY)
        = addSuperinterface(superinterface.asTypeName(), delegate)

    fun addSuperinterface(superinterface: KClass<*>, delegate: CodeBlock = CodeBlock.EMPTY)
        = addSuperinterface(superinterface.asTypeName(), delegate)

    fun addSuperinterface(superinterface: KClass<*>, constructorParameterName: String) =
        addSuperinterface(superinterface.asTypeName(), constructorParameterName)

    fun addSuperinterface(superinterface: TypeName, constructorParameter: String) = apply {
      requireNotNull(primaryConstructor) {
        "delegating to constructor parameter requires not-null constructor"
      }
      val parameter = primaryConstructor?.parameter(constructorParameter)
      requireNotNull(parameter) {
        "no such constructor parameter '$constructorParameter' to delegate to for type '$name'"
      }
      addSuperinterface(superinterface, CodeBlock.of(constructorParameter))
    }

    @JvmOverloads fun addEnumConstant(
      name: String,
      typeSpec: TypeSpec = anonymousClassBuilder().build()
    ) = apply {
      enumConstants[name] = typeSpec
    }

    fun addProperties(propertySpecs: Iterable<PropertySpec>) = apply {
      propertySpecs.map(this::addProperty)
    }

    fun addProperty(propertySpec: PropertySpec) = apply {
      if (EXPECT in kind.modifiers) {
        require(propertySpec.initializer == null) {
          "properties in expect classes can't have initializers"
        }
        require(propertySpec.getter == null && propertySpec.setter == null) {
          "properties in expect classes can't have getters and setters"
        }
      }
      propertySpecs += propertySpec
    }

    fun addProperty(name: String, type: TypeName, vararg modifiers: KModifier)
        = addProperty(PropertySpec.builder(name, type, *modifiers).build())

    fun addProperty(name: String, type: Type, vararg modifiers: KModifier)
        = addProperty(name, type.asTypeName(), *modifiers)

    fun addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = addProperty(name, type.asTypeName(), *modifiers)

    fun addInitializerBlock(block: CodeBlock) = apply {
      check(isSimpleClass || isEnum || kind is Object) {
        "$kind can't have initializer blocks"
      }
      check(EXPECT !in kind.modifiers) {
        "expect $kind can't have initializer blocks"
      }
      initializerBlock.add("init {\n")
          .indent()
          .add(block)
          .unindent()
          .add("}\n")
    }

    fun addFunctions(funSpecs: Iterable<FunSpec>) = apply {
      funSpecs.forEach { addFunction(it) }
    }

    fun addFunction(funSpec: FunSpec) = apply {
      funSpecs += funSpec
    }

    fun addTypes(typeSpecs: Iterable<TypeSpec>) = apply {
      this.typeSpecs += typeSpecs
    }

    fun addType(typeSpec: TypeSpec) = apply {
      typeSpecs += typeSpec
    }

    fun build(): TypeSpec {
      if (enumConstants.isNotEmpty()) {
        check(isEnum) { "${this.name} is not enum and cannot have enum constants" }
        for (it in enumConstants.keys) {
          require(it.isName) { "not a valid enum constant: $name" }
        }
      }

      if (superclassConstructorParameters.isNotEmpty()) {
        checkCanHaveSuperclass()
      }

      check(!(isAnonymousClass && typeVariables.isNotEmpty())) {
        "typevariables are forbidden on anonymous types"
      }

      require(!isEnum || enumConstants.isNotEmpty()) {
        "at least one enum constant is required for $name"
      }

      val isAbstract = ABSTRACT in kind.modifiers || kind !is Kind.Class || !isSimpleClass
      for (funSpec in funSpecs) {
        require(isAbstract || ABSTRACT !in funSpec.modifiers) {
          "non-abstract type $name cannot declare abstract function ${funSpec.name}"
        }
        when {
          kind is Interface -> {
            requireNoneOf(funSpec.modifiers, KModifier.INTERNAL, KModifier.PROTECTED)
            requireNoneOrOneOf(funSpec.modifiers, KModifier.ABSTRACT, KModifier.PRIVATE)
          }
          isAnnotation -> require(funSpec.modifiers == kind.implicitFunctionModifiers) {
            "annotation class $name.${funSpec.name} requires modifiers ${kind.implicitFunctionModifiers}"
          }
          EXPECT in kind.modifiers -> require(funSpec.body.isEmpty()) {
            "functions in expect classes can't have bodies"
          }
        }
      }

      if (primaryConstructor == null) {
        require(funSpecs.none { it.isConstructor } || superclassConstructorParameters.isEmpty()) {
          "types without a primary constructor cannot specify secondary constructors and superclass constructor parameters"
        }
      }

      val companionObjectsCount = typeSpecs.count { it.isCompanion }
      when (companionObjectsCount) {
        0 -> Unit
        1 -> {
          require(isSimpleClass || kind is Interface || isEnum) {
            "$kind types can't have a companion object"
          }
        }
        else -> {
          throw IllegalArgumentException("Multiple companion objects are present but only one is allowed.")
        }
      }

      return TypeSpec(this)
    }
  }

  companion object {
    @JvmStatic fun classBuilder(name: String) = Builder(Kind.Class(), name)

    @JvmStatic fun classBuilder(className: ClassName) = classBuilder(className.simpleName)

    @JvmStatic fun expectClassBuilder(name: String) = Builder(Kind.Class(EXPECT), name)

    @JvmStatic fun expectClassBuilder(className: ClassName) = expectClassBuilder(className.simpleName)

    @JvmStatic fun objectBuilder(name: String) = Builder(Kind.Object(), name)

    @JvmStatic fun objectBuilder(className: ClassName) = objectBuilder(className.simpleName)

    @JvmStatic @JvmOverloads fun companionObjectBuilder(name: String? = null) =
        Builder(Kind.Object(COMPANION), name)

    @JvmStatic fun interfaceBuilder(name: String) = Builder(Kind.Interface(), name)

    @JvmStatic fun interfaceBuilder(className: ClassName) = interfaceBuilder(className.simpleName)

    @JvmStatic fun enumBuilder(name: String) = Builder(Kind.Class(ENUM), name)

    @JvmStatic fun enumBuilder(className: ClassName) = enumBuilder(className.simpleName)

    @JvmStatic fun anonymousClassBuilder(): Builder {
      return Builder(Kind.Class(), null)
    }

    @JvmStatic fun annotationBuilder(name: String) = Builder(Kind.Class(ANNOTATION), name)

    @JvmStatic fun annotationBuilder(className: ClassName)
        = annotationBuilder(className.simpleName)
  }
}

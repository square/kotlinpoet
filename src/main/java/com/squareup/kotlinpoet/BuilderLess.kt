package com.squareup.kotlinpoet

import kotlin.reflect.KClass

fun main(args: Array<String>) {

  val greetFunSpec = FunSpec.builder("greet")
    .addStatement("println(%S)", "Hello, \$name")
    .build()

  val greeterTypeSpec = TypeSpec.of(
    className = "Greeter",
    constructorProperties = listOf(PropertySpec.property("name", typeName<String>())),
    functions = listOf(greetFunSpec)
  )

  val mainFunSpec = FunSpec.builder("main")
    .addParameter("args", String::class, KModifier.VARARG)
    .addStatement("%T(args[0]).greet()", ClassName("", "Greeter"))
    .build()

  val file = FileSpec.of(
    packageName = "",
    fileName = "HelloWorld",
    types = listOf(greeterTypeSpec),
    functions = listOf(mainFunSpec)
  )
  file.writeTo(System.out)
}

inline fun <reified T : Any> typeName(clazz: Class<T> = T::class.java): ClassName
    = T::class.asTypeName()


fun PropertySpec.Companion.property(
  name: String,
  type: ClassName,
  modifiers: List<KModifier> = emptyList(),
  initializer: CodeBlock? = null,
  configuration: PropertySpec.Builder.() -> Unit = {}
): PropertySpec = PropertySpec.builder(name, type, *modifiers.toTypedArray())
  .apply {
    if (initializer != null) initializer(initializer)
    configuration()
  }
  .build()

fun PropertySpec.Companion.varProperty(
  name: String,
  type: ClassName,
  modifiers: List<KModifier> = emptyList(),
  initializer: CodeBlock? = null,
  configuration: PropertySpec.Builder.() -> Unit = {}
) : PropertySpec = PropertySpec.varBuilder(name, type, *modifiers.toTypedArray())
  .apply {
    if (initializer != null) initializer(initializer)
    configuration()
  }.build()

fun PropertySpec.Companion.nullableProperty(
  name: String,
  type: ClassName,
  modifiers: List<KModifier> = emptyList(),
  initializer: CodeBlock? = null,
  configuration: PropertySpec.Builder.() -> Unit = {}
) : PropertySpec = PropertySpec.property(name, type.asNullable(), modifiers, initializer)

fun PropertySpec.Companion.nullableVarProperty(
  name: String,
  type: ClassName,
  modifiers: List<KModifier> = emptyList(),
  initializer: CodeBlock? = null,
  configuration: PropertySpec.Builder.() -> Unit = {}
): PropertySpec = PropertySpec.nullableProperty(name, type.asNullable(), modifiers, initializer, configuration)


fun TypeSpec.Companion.of(
  className: String,
  constructorProperties: List<PropertySpec> = emptyList(),
  modifiers: List<KModifier> = emptyList(),
  types: Iterable<TypeSpec> = emptyList(),
  primaryConstructor: FunSpec? = null,
  functions: Iterable<FunSpec> = emptyList(),
  annotations: Annotations = Annotations(),
  properties: List<PropertySpec> = emptyList(),
  configuration: TypeSpec.Builder.() -> Unit = {}
): TypeSpec {
  return TypeSpec.classBuilder(className)
    .apply {
      require(constructorProperties.isEmpty() || primaryConstructor == null)
      if (primaryConstructor != null) {
        primaryConstructor(primaryConstructor)
      }
      if (constructorProperties.isNotEmpty()) {
        val parameterSpecs = mutableListOf<ParameterSpec>()
        for (cp in constructorProperties) {
          parameterSpecs += ParameterSpec.builder(cp.name, cp.type, *cp.modifiers.toTypedArray())
            .apply {
              if (cp.initializer != null) defaultValue(cp.initializer)
            }
            .build()
          addProperty(cp.toBuilder().initializer(cp.name).build())
        }

        primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameters(parameterSpecs)
            .build())
      }
      for (t in types) addType(t)
      for (f in functions) addFunction(f)
      for (a in annotations.specs) addAnnotation(a)
      for (a in annotations.classes) addAnnotation(a)
      for (a in annotations.classNames) addAnnotation(a)
      for (a in annotations.kclasses) addAnnotation(a)
      addModifiers(*modifiers.toTypedArray())
      addProperties(properties)

      configuration()
    }
    .build()
}

fun FileSpec.Companion.of(
  packageName: String,
  fileName: String,
  types: Iterable<TypeSpec> = emptyList(),
  functions: Iterable<FunSpec> = emptyList(),
  annotations: Annotations = Annotations(),
  properties: List<PropertySpec> = emptyList(),
  comment: String = "",
  typeAliases: Iterable<TypeAliasSpec> = emptyList(),
  configuration: FileSpec.Builder.() -> Unit = {}
): FileSpec = FileSpec.builder(packageName, fileName)
  .apply {
    for (t in types) addType(t)
    for (f in functions) addFunction(f)
    for (a in annotations.specs) addAnnotation(a)
    for (a in annotations.classes) addAnnotation(a)
    for (a in annotations.classNames) addAnnotation(a)
    for (a in annotations.kclasses) addAnnotation(a)
    for (p in properties) addProperty(p)
    if (comment.isNotEmpty()) addComment(comment)
    for (ty in typeAliases) addTypeAlias(ty)
    configuration()
  }
  .build()


data class Annotations(
  val specs: Iterable<AnnotationSpec> = emptyList(),
  val classes: Iterable<Class<*>> = emptyList(),
  val kclasses: Iterable<KClass<*>> = emptyList(),
  val classNames: Iterable<ClassName> = emptyList()
)

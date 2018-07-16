Change Log
==========

## Version 1.0.0-RC1

_2018-07-16_

 * New: Escape keywords in imports and canonical class names.
 * New: Improve `external` support.
 * New: Extensions for `KType` and `KTypeParameter`.
 * New: Add builder methods to simplify adding common kotlin.jvm annotations.
 * New: Enums are able to have companion objects.
 * New: Add missing primaryConstructor & companionObject to `TypeSpec#toBuilder()`.
 * New: Make subtype checking vals inside Kind public.
 * New: Escape (class/property/function/variable) names automatically if they contain space, hyphen, or other symbols.
 * New: Improve `ParameterizedTypeName` API.
 * New: Add `WildcardTypeName.STAR` constant.
 * New: Expose mutable builder properties and move their validations to build-time.
 * Fix: Use regular indents for parameter lists.
 * Fix: Inline annotations on properties defined in primary constructor.
 * Fix: Use `Any?` as the default type variable bounds.
 * Fix: Fix importing annotated `TypeName`.
 * Fix: If any primary constructor property has KDoc, put properties on new lines.
 * Fix: Properly emit where block in type signature.
 * Fix: Avoid type name collisions in primary constructor.
 * Fix: Remove implicit `TypeVariable` bound when more bounds are added.
 * Fix: Combine annotations and modifiers from constructor params and properties.
 * Fix: Replace delegate constructor args along with the constructor.

## Version 0.7.0

_2018-02-16_

 * New: Increase indent to 4 spaces.
 * New: Delegate super interfaces as constructor parameters.
 * New: Support `PropertySpec`s as `CodeBlock` literals.
 * New: Support KDoc for `TypeAliasSpec`.
 * New: Allow for adding an initializer block inside a companion object.
 * New: Escape name in `ParameterSpec` which is also a keyword.
 * New: Escape names in statements.
 * New: Set com.squareup.kotlinpoet as automatic module name.
 * New: Support suspending lambda types.
 * New: Support named `LambdaTypeName` parameters.
 * New: Support dynamic type.
 * New: Disallow wildcard imports.
 * New: Depend on Kotlin 1.2.21.
 * Fix: Correct handling of super-classes/interfaces on anonymous classes.
 * Fix: Fix boundary filtering to `Any?`.
 * Fix: Wrap long property initializers.
 * Fix: Fix formatting and indentation of parameter lists.

## Version 0.6.0

_2017-11-03_

 * New: Support lambda extensions.
 * New: Support renames in imports like `import bar.Bar as bBar`.
 * New: Support extension and inline properties.
 * New: Support reified types.
 * New: Expose enclosed types inside `LambdaTypeName`.
 * New: Depend on Kotlin Kotlin 1.1.51.
 * New: Improved API and formatting of annotations.
 * New: Improved multiplatform support.
 * Fix: Escape function and package names if they are a Kotlin keyword.
 * Fix: Properly format WildcardTypeName's class declaration.


## Version 0.5.0

_2017-09-13_

 * New: Rename `addFun()` to `addFunction()`.
 * New: Rename `KotlinFile` to `FileSpec`.
 * New: Rename `KotlinFile.addFileAnnotation()` to `addAnnotation()`.
 * New: Rename `KotlinFile.addFileComment()` to `addComment()`.
 * New: Support cross-platform code, including `HEADER` and `IMPL` modifiers.
 * New: Support type variables for type aliases.
 * New: Support constructor delegation.
 * New: Support named companion objects.
 * New: Depend on Kotlin 1.1.4-3.
 * Fix: Format one parameter per line when there are more than two parameters.
 * Fix: Don't emit braces when the constructor body is empty.
 * Fix: Do not invoke superclass constructor when no primary constructor.
 * Fix: Enforce the right modifiers on functions.


## Version 0.4.0

_2017-08-08_

 * New: Change KotlinPoet's extensions like `asClassName()` to be top-level functions.
 * New: Add declaration-site variance support.
 * New: Improve handling of single expression bodies.
 * New: Support file annotations.
 * New: Support imports from the top-level file.
 * New: Accept superclass constructor parameters.
 * New: Support primary constructors using the `constructor` keyword.
 * Fix: Don't emit setter parameter types.
 * Fix: Support Kotlin keywords in `NameAllocator`.
 * Fix: Emit the right default parameters for primary constructors.
 * Fix: Format annotations properly when used as parameters.
 * Fix: Recognize imports when emitting nullable types.
 * Fix: Call through to the superclass constructor when superclass has a no-args constructor.
 * Fix: Omit class braces if all properties are declared in primary constructor.
 * Fix: Don't emit empty class bodies.
 * Fix: Emit the right syntax for declaring multiple generic type constraints.
 * Fix: Support properties on objects, companions and interfaces.
 * Fix: Use `AnnotationSpec` for throws.


## Version 0.3.0

_2017-06-11_

 * New: Objects and companion objects.
 * New: `TypeAliasSpec` to create type aliases.
 * New: `LambdaTypeName` to create lambda types.
 * New: Collapse property declarations into constructor params.
 * New: Extension and invoke functions for creating type names: `Runnable::class.asClassName()`.
 * New: Basic support for expression bodies.
 * New: Basic support for custom accessors.
 * New: Remove `Filer` writing and originating elements concept. These stem from `javac` annotation
   processors.
 * Fix: Generate valid annotation classes.
 * Fix: Use `KModifier` for varargs.
 * Fix: Use `ParameterizedTypeName` for array types.
 * Fix: Extract Kotlin name from `KClass` instead of Java name.
 * Fix: Emit valid class literals: `Double::class` instead of `Double.class`.
 * Fix: Emit modifiers in the expected order.
 * Fix: Emit the correct syntax for enum classes and overridden members.


## Version 0.2.0

_2017-05-21_

 * New: Flip API signatures to be (name, type) instead of (type, name).
 * New: Support for nullable types.
 * New: Support delegated properties.
 * New: Extension functions.
 * New: Support top-level properties.
 * Fix: Inheritance should use `:` instead of `extends` and `implements`.
 * Fix: Make initializerBlock emit `init {}`.


## Version 0.1.0

_2017-05-16_

 * Initial public release.

Change Log
==========

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

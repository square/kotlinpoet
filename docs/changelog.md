Change Log
==========

## Version 1.6.0

_2020-05-28_

 * New: Deprecate Mirror API integrations.
 
   Mirror API integrations, such as `TypeElement.asClassName()` and 
   `FunSpec.overriding(ExecutableElement)`, are being deprecated in this release. These KotlinPoet
   APIs are most often used in annotation processors. Since kapt runs annotation processors over
   stubs, which are Java files, a lot of the Kotlin-specific information gets lost in translation 
   and cannot be accessed by KotlinPoet through the Mirror API integrations. Examples include:
   
   - Alias types, such as `kotlin.String`, get converted to their JVM representations, such as 
     `java.lang.String`.
   - Type nullability information is not accessible.
   - `suspend` functions are seen as simple functions with an additional `Continuation` parameter.
   
   The correct solution is to switch to [KotlinPoet-metadata][kotlinpoet-metadata] or 
   [KotlinPoet-metadata-specs][kotlinpoet-metadata-specs] API, which fetches Kotlin-specific
   information from the `@Metadata` annotation and produces correct KotlinPoet Specs. We may explore
   adding new metadata-based alternatives to the deprecated APIs in the future.
 
 * New: Kotlin 1.3.72.
 * New: Improve `MemberName` to support operator overloading.
 * New: Support generics in `AnnotationSpec`.
 * New: Add support for functional interfaces.
 * New: Make more `FunSpec.Builder` members public for easier mutation.
 * Fix: Properly propagate implicit type and function modifiers in nested declarations.
 * Fix: Properly escape type names containing `$` character.
 * Fix: Don't emit `LambdaTypeName` annotations twice.
 * Fix: Preserve tags in `TypeName.copy()`.

## Version 1.5.0

_2020-01-09_

 KotlinPoet now targets JDK8, which means that executing a build that includes KotlinPoet as a
 dependency on a machine with an older version of JDK installed won't work. **This has no effect on 
 the code that KotlinPoet produces**: the code can still be compiled against JDK6, as long as it 
 doesn't use any features that were introduced in newer releases.
 
 * New: Kotlin 1.3.61.
 * New: Add support for processing FileFacades in KotlinPoet-metadata.
 * New: Add support for inner nested and companion objects on annotation classes.
 * New: Improve error messages for mismatched open/close statement characters.
 * New: Tag `AnnotationSpec`s with the annotation mirror when available.
 * New: Include annotations on enum entries when creating `TypeSpec`s from metadata.
 * Fix: Fix metadata parsing for types.
 * Fix: Allow file names that are Kotlin keywords.
 * Fix: Properly escape type alias names with backticks.
 * Fix: Allow creating `TypeSpec`s with names that can be escaped with backticks.
 * Fix: Properly escape enum constant names with backticks.
 * Fix: Maintain proper ordering of properties and initializers when emitting a `TypeSpec`. 
   **Note**: with this change, any properties declared after any initializer blocks will not be
   added to the primary constructor and will instead be emitted inside the `TypeSpec` body.
 * Fix: Don't emit a leading new line if type KDoc is empty but parameter KDocs are present.
 * Fix: Ensure KotlinPoet-metadata resolves package names properly.
 
 ## Version 1.4.4

_2019-11-16_

 * Fix: Support reified inline types in KotlinPoet-metadata.
 
## Version 1.4.3

_2019-10-30_

 * Fix: Don't emit stubs for abstract functions in KotlinPoet-metadata.
 
## Version 1.4.2

_2019-10-28_

 * Fix: Properly handle abstract elements in KotlinPoet-metadata.
 * Fix: Properly handle typealiases in KotlinPoet-metadata.
 * Fix: Properly render % symbols at the end of KDocs.

## Version 1.4.1

_2019-10-18_

 * New: Add annotations support to `TypeAliasSpec`.
 * New: Read type annotations from Kotlin `Metadata`.
 * New: Introduce `ImmutableKmDeclarationContainer`.
 * Fix: Use full package name for shading `auto-common`.
 * Fix: Support reading self-type variables (e.g. `Asset<A : Asset<A>>`) from Kotlin `Metadata`.

## Version 1.4.0

_2019-09-24_

 * New: This release introduces the new KotlinPoet-metadata API that makes it easy to introspect 
   Kotlin types and build KotlinPoet Specs based on that data. 
   
   The strategy for type introspection is driven by `ClassInspector`, which is a basic interface for 
   looking up JVM information about a given Class. This optionally is used by the 
   `toTypeSpec()`/`toFileSpec()` APIs in `kotlinpoet-metadata-specs` artifact to inform about 
   Classes with information that isn’t present in metadata (overrides, JVM modifiers, etc). There 
   are two batteries-included implementations available in `ReflectiveClassInspector` 
   (for reflection) and `ElementsClassInspector` (for the javax Elements API in annotation 
   processing). These implementations are available through their respective 
   `kotlinpoet-classinspector-*` artifacts. For more information refer to the 
   [KotlinPoet-metadata-specs README][kotlinpoet-metadata-specs].
   
   At the time of this release the API is in experimental mode and has to be opted into via the
   `KotlinPoetMetadataPreview` annotation.
 
 * New: Kotlin 1.3.50.
 * New: A new constructor to simplify creation of `ParameterSpec` instances.
 * New: New `ClassName` constructors.
 * New: `TypeName` and subclasses can now store tags.
 * New: Optional parameters added to `toBuilder()` methods of most Specs.
 * New: `List` overrides for Spec methods that accept `vararg`s.
 * New: `CodeBlock.Builder.clear()` helper method.
 * New: `FunSpec.Builder.clearBody()` helper method.
 * Fix: Properly escape enum constant names.
 * Fix: Ensure trailing newlines in KDoc and function bodies.
 * Fix: `TypeVariableName`s with empty bounds will now default to `Any?`.
 * Fix: Don't emit parens for primary constructors.
 * Fix: `ClassName`s with empty simple names are not allowed anymore.
 * Fix: Throw if names contain illegal characters that can't be escaped with backticks.

## Version 1.3.0

_2019-05-30_

 * New: Don't inline annotations in the primary constructor.
 * New: Force new lines when emitting primary constructors.
 * New: Support using MemberNames as arguments to %N.
 * New: Add more ClassName constants: ClassName.STRING, ClassName.LIST, etc.
 * New: Add ClassName.constructorReference() and MemberName.reference().
 * New: Make %N accept MemberNames.
 * New: Escape spaces in import aliases.
 * New: Escape spaces in ClassNames.
 * New: Escape spaces in MemberNames.
 * New: Escape imports containing spaces.
 * New: Escape package name containing spaces.
 * New: Use 2-space indents.
 * New: Only indent one level on annotation values.
 * Fix: Pass only unique originating elements to Filer.
 * Fix: Fix bug with MemberNames in same package nested inside a class.

## Version 1.2.0

_2019-03-28_
 
 * New: Add writeTo(Filer) and originating element API.
 * New: Make *Spec types taggable.
 * New: Make FunSpec.Builder#addCode take vararg Any?.
 * Fix: Import members from default package.
 * Fix: Add non-wrapping spaces in control flow creation methods.
 * Fix: Named "value" argument being omitted in annotation array types. 

## Version 1.1.0

_2019-02-28_

 * New: Kotlin 1.3.21.
 * New: Support referencing members using `%M` and `MemberName` type.
 * New: Add extensions for getting a `MemberName` from a `ClassName`, `KClass` and `Class`.
 * New: Allow passing `CodeBlock`s as arguments to `%P`.
 * New: Allow interface delegation for objects.
 * Fix: Don't emit visible whitespace in `toString()`.
 * Fix: Prevent line wrapping in weird places inside function signature.
 * Fix: No line wrapping between val and property name.
 * Fix: Allow passing line prefix into `LineWrapper` to enable proper line wrapping in KDoc.
 * Fix: Add newline for `TypeSpec` Kdoc with no tags.
 * Fix: Add newline for remaining Specs.
 * Fix: Fix kdoc formatting for property getter/setters.
 * Fix: Don't wrap single line comments inside `FunSpec`.
 * Fix: Add non-wrapping package name.
 * Fix: Remove n^2 algorithm in `CodeWriter.resolve()` by precomputing all of the nested simple names of a `TypeSpec`.
 * Fix: Fix edge case with empty enum classes.
 * Fix: Fix Nullable Type Parameter handling in `KType.asTypeName()`.
 * Fix: Fix incorrect long comment wrapping in `FileSpec`.
 * Fix: Attach primary constructor param/property KDoc to the element vs emitting it inside the type header.

## Version 1.0.1

_2019-01-02_

 * New: Allow enums without constants.
 * New: Improved formatting of TypeSpec KDoc.
 * New: Support @property and @param KDoc tags in TypeSpec.
 * Fix: Use pre-formatted strings for arguments to %P.

## Version 1.0.0

_2018-12-10_

 * New: Kotlin 1.3.11.
 * Fix: Prevent wrapping in import statements.

## Version 1.0.0-RC3

_2018-11-28_

 * New: Kotlin 1.3.10.
 * New: Add `%P` placeholder for string templates.
 * New: Add support for receiver kdoc.
 * New: Avoid emitting `Unit` as return type.
 * New: Add support for empty setters.
 * New: Add checks for inline classes.
 * New: Escape property and variable names if keywords.
 * New: Replace `%>`, `%<`, `%[`, `%]` placeholders with `⇥`, `⇤`, `«`, `»`.
 * New: Replace `%W` with space, and add `·` as a non-breaking space.
 * New: Change `TypeName` to sealed class.
 * New: Documentation improvements.
 * New: Replace `TypeName` modifier methods with `copy()`.
 * New: Rename members of `WildcardTypeName` to match with the producer/consumer generics model.
 * New: Rename `TypeName.nullable` into `TypeName.isNullable`.
 * New: Rename `LambdaTypeName.suspending` into `LambdaTypeName.isSuspending`.
 * New: Rename `TypeVariableName.reified` into `TypeVariableName.isReified`.
 * Fix: Emit star-projection only for types with `Any?` upper bound.
 * Fix: Fold property with escaped name.

## Version 1.0.0-RC2

_2018-10-22_

 * New: Kotlin 1.2.71.
 * New: README improvements.
 * New: Allow opening braces and params in `beginControlFlow()`.
 * New: Add KDoc to `ParameterSpec`, collapse into parent KDoc.
 * New: Support `TypeVariable`s in `PropertySpec`.
 * New: Add parens for annotated types in `LambdaTypeName`.
 * New: Improve error messaging and documentation for inline properties.
 * New: Allow sealed classes to declare abstract properties.
 * New: Added `buildCodeBlock()` helper function.
 * New: Allow using `CodeBlock`s with statements as property initializers and default parameter values.
 * New: Rename `NameAllocator.clone()` into `NameAllocator.copy().
 * New: Rename `TypeName.asNonNullable()` to `TypeName.asNonNull()`.
 * New: Remove `PropertySpec.varBuilder()` (use `mutable()` instead).
 * New: Allow importing top-level members in default package.
 * New: Add overloads to add KDoc to return type.
 * Fix: Distinguishing `IntArray` and `Array<Int>` when creating `TypeName`.
 * Fix: Use `TypeName` instead of `ClassName` as parameter type of `plusParameter()`.
 * Fix: Keep type-parameter variance when constructing `TypeName` from `KType`.
 * Fix: Don't validate modifiers when merging properties with primary constructor parameters.
 * Fix: Escape $ characters in formatted strings.
 * Fix: `FileSpec.Builder` blank package and subfolder fix.
 * Fix: Append new line at end of parameter KDoc.
 * Fix: Add parameter KDoc in `toBuilder()`.

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

 [kotlinpoet-metadata]: ../kotlinpoet_metadata
 [kotlinpoet-metadata-specs]: ../kotlinpoet_metadata_specs

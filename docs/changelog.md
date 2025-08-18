Change Log
==========

## Unreleased

 * Fix: Support variance emission for TypeVariableName in parameterized types. (#1933)
 * Fix: FunSpec.beginControlFlow to accept nullable arguments for consistency with CodeBlock.beginControlFlow. (#2174)
 * New: Add support for type aliases in types.
 * Fix: Annotation array parameters with annotation elements now correctly handled. (#2142)
 * Fix: `KType.asTypeName` now correctly handles recursively bound generics (e.g. `T : Comparable<T>`). (#1914)
 * Fix: Don't convert multi-statement function to expression body. (#1979)
 * In-development snapshots are now published to the Central Portal Snapshots repository at https://central.sonatype.com/repository/maven-snapshots/.
 * New: Add NameAllocator.contains to check if a given tag is already allocated (#2154)

## Version 2.2.0

Thanks to [@IRus][IRus] for contributing to this release.

_2025-05-16_

 * New: Kotlin 2.1.21.
 * New: KSP 2.1.21-2.0.1.
 * New: Add support for context parameters. (#2112)
 * New: Eliminate Guava dependency. (#2110)
 * New: Migrate to Dokka plugin V2. (#2113)

## Version 2.1.0

Thanks to [@ForteScarlet][ForteScarlet], [@TrevorSStone][TrevorSStone],
[@RaoPrashanth][RaoPrashanth], [@damianw][damianw], [@ansehoon1999][ansehoon1999],
[@TheMrMilchmann][TheMrMilchmann] for contributing to this release.

_2025-02-25_

 * New: Kotlin 2.1.10.
 * New: KSP 2.1.10-1.0.30.
 * Fix: Support delegates on anonymous classes. (#2034)
 * Fix: Prevent aliased imports from colliding with existing imports. (#2052)
 * Fix: `TypeName.MUTABLE_MAP_ENTRY` now correctly uses the `MutableEntry` nested class name. (#2061)
 * Fix: Use the same aliased import for both the nullable and non-nullable versions of a type. (#2068)
 * Fix: Allow zero methods in a functional interface if it has a superinterface. (#2075)

## Version 2.0.0

Thanks to [@brokenhappy][brokenhappy], [@tajobe][tajobe], [@niyajali][niyajali],
[@ForteScarlet][ForteScarlet] for contributing to this release.

_2024-10-23_

This release is source- and binary-compatible with KotlinPoet 1.x.

The most important behavior change in this release is that spaces in generated code don't wrap by
default anymore.

KotlinPoet 1.x used to replace space characters with newline characters whenever a given line of
code exceeded the length limit. This usually led to better code formatting, but could also lead to
compilation errors in generated code. Non-breaking spaces could be marked by the `·` character, but
the discoverability of this feature wasn't great.

KotlinPoet 2.0 does not wrap spaces, even if the line of code they occur in exceeds the length
limit. The newly introduced `♢` character can be used to mark spaces that are safe to wrap, which
can improve code formatting. The `·` character has been preserved for compatibility, but its
behavior is now equivalent to a regular space character.

 * New: Kotlin 2.0.10.
 * New: Spaces don't break by default.
 * New: New `♢` placeholder representing a space that is safe to wrap.
 * New: Add `KSTypeAlias.toClassName()`. (#1956)
 * New: Add `KSType.toClassNameOrNull()`. (#1956)
 * Fix: Enum classes that only have an init block now also generate the required semicolon. (#1953)
 * Fix: Preserve typealiases in `KSAnnotation.toAnnotationSpec()`. (#1956)
 * Fix: Preserve nullability in `KSType.toClassName()`. (#1956)

## Version 1.18.1

Thanks to [@mitasov-ra][mitasov-ra] for contributing to this release.

_2024-07-15_

 * Fix: Workaround for [KT-18706][kt-18706]: KotlinPoet now generates import aliases without backticks (#1920).

   ```kotlin
   // before, doesn't compile due to KT-18706
   import com.example.one.`$Foo` as `One$Foo`
   import com.example.two.`$Foo` as `Two$Foo`

   // now, compiles
   import com.example.one.`$Foo` as One__Foo
   import com.example.two.`$Foo` as Two__Foo
   ```

## Version 1.18.0

Thanks to [@DanielGronau][DanielGronau] for contributing to this release.

_2024-07-05_

 * New: Kotlin 2.0.0.
 * New: KSP 2.0.0-1.0.22.
 * New: Promote `kotlinpoet-metadata` out of preview to stable.
 * New: Migrate `kotlinpoet-metadata` to stable `org.jetbrains.kotlin:kotlin-metadata-jvm` artifact for Metadata parsing.
 * New: Make enum entry references in `KSAnnotation.toAnnotationSpec()` and `KSClassDeclaration.toClassName()` more robust.
 * Fix: Don't expand typealiases of function types to `LambdaTypeName`s in `KSTypeReference.toTypeName()`.
 * Fix: Avoid rounding small double and float values in `%L` translation (#1927).
 * Fix: Fix typealias type argument resolution in KSP2 (#1929).

## Version 1.17.0

Thanks to [@jisungbin][jisungbin], [@hfhbd][hfhbd], [@evant][evant], [@sgjesse][sgjesse], [@sebek64][sebek64] for
contributing to this release.

_2024-05-24_

* Change: kotlinx-metadata 0.9.0. Note that the `KotlinClassMetadata.read` is deprecated in 0.9.0 and replaced with
  `readStrict` (#1830).
  * Note: we now also provide `lenient` parameters to map to the underlying `readStrict()` and `readLenient()` calls
    (#1766).
  * We have also removed various `Class`/`TypeElement`/`Metadata`-to-`KmClass` APIs from the public API, as these are
    trivial to write now with kotlinx-metadata's newer APIs and allows us to focus the API surface area of this artifact
    better (#1891).
* New: Supertype list wraps to one-per-line if the primary constructor spans multiple lines (#1866).
* New: Extract `MemberSpecHolder` interface for constructs that can hold `PropertySpec`s and `FunSpec`s and their
  builders (#1877).
* New: `joinToCode` variant which operates on any type, but requires a transform lambda to convert each element into a
  `CodeBlock` (#1874).
* New: Support annotation type arguments in `KSAnnotation.toAnnotationSpec()` (#1889).
* Fix: Prevent name clashes between a function in class and a function call in current scope (#1850).
* Fix: Fix extension function imports (#1814).
* Fix: Omit implicit modifiers on `FileSpec.scriptBuilder` (#1813).
* Fix: Fix trailing newline in `PropertySpec` (#1827).
* Fix: `KSAnnotation.toAnnotationSpec` writes varargs in place instead of making them an array to work around a Kotlin
  issue with `OptIn` annotations (#1833).
* Fix: `MemberName`s without a package are now correctly imported (#1841)
* Fix: Throw if primary constructor delegates to other constructors (#1859).
* Fix: Aliased imports with nested class (#1876).
* Fix: Check for error types in `KSType.toClassName()` (#1890).
* Fix: Support generating a single import for overloaded `MemberName`s (#1909).

## Version 1.16.0

Thanks to [@drawers][drawers], [@rickclephas][rickclephas] for contributing to this release.

_2024-01-18_

 * New: Kotlin 1.9.22.
 * New: KSP 1.9.22-1.0.16.
 * New: Add `NameAllocator` API to control keyword pre-allocation (#1803).
 * Fix: Fix issue with missing `suspend` modifier in `KSTypeReference.toTypeName` (#1793).
 * Fix: Honour same-package import aliases (#1794).
 * Fix: Always include parameter docs in the type header (#1800).

## Version 1.15.3

Thanks to [@gabrielittner][gabrielittner] for contributing to this release.

_2023-12-04_

 * Fix: Fix nullability of lambdas in `KSTypeReference.toTypeName` (#1756).

## Version 1.15.2

Thanks to [@evant][evant] for contributing to this release.

_2023-11-30_

 * New: Kotlin 1.9.21.
 * New: KSP 1.9.21-1.0.15.
 * New: KSP: more accurately represent function types (#1742).

## Version 1.15.1

_2023-11-19_

 * Fix: Fix a regression introduced by #1637, where a superfluous newline is added to a type's KDoc
 if it has a primary constructor with no docs (#1727).

## Version 1.15.0

_2023-11-18_

Thanks to [@drawers][drawers], [@fejesjoco][fejesjoco], [@takahirom][takahirom],
[@martinbonnin][martinbonnin], [@mcarleio][mcarleio] for contributing to this release.

In this release the `:kotlinpoet` module has been converted to a Kotlin Multiplatform module
(#1654), though for now it only supports the JVM target. **Important**: unless you're building
with Gradle, you will now need to depend on the `kotlinpoet-jvm` artifact, instead of `kotlinpoet` -
see [Downloads](index.md#download) for instructions.

 * New: Kotlin 1.9.20.
 * New: KSP 1.9.20-1.0.14.
 * New: Extract `TypeSpecHolder` interface for constructs that can hold a TypeSpec and their builders (#1723).
 * New: Expose relative path from `FileSpec` (#1720).
 * New: Return the generated path from `FileSpec.writeTo()`. (#1514).
 * New: Remove default compatibility from unstable types (#1662).
 * New: Deprecate `TypeSpec.expectClassBuilder()` and `TypeSpec.valueClassBuilder()` (#1589).
 * New: Add option to convert `KSAnnotation` to `AnnotationSpec` while omitting default values (#1538).
 * New: Add `FileSpec.builder` convenience for `MemberName` (#1585).
 * Fix: Set `DecimalFormatSymbols.minusSign` for consistency across locales (#1658).
 * Fix: Fix link to incremental KSP in KDoc (#1638).
 * Fix: Emit primary constructor KDoc (#1637).
 * Change: kotlinx-metadata 0.7.0. This is a breaking change for users of the `:kotlinpoet-metadata`
   module, as most `Flags`-API extensions have been removed in favor of the now-available first-party versions.

## Version 1.14.2

_2023-05-30_

 * Fix: Fix one more missing API in binary compatibility override in `Annotatable.Builder` (#1581).

## Version 1.14.1

_2023-05-29_

 * Fix: Restore ABI stability for annotatable and documentable builders (#1580).

## Version 1.14.0

_2023-05-29_

Thanks to [@Omico][Omico], [@drawers][drawers], [@RBusarow][RBusarow] for contributing to this release.

 * New: Kotlin 1.8.21.
 * New: KSP 1.8.21-1.0.11.
 * New: Enable default methods in Java bytecode (#1561).
 * New: Group Kotlin and Renovate updates together in Renovate (#1562).
 * New: Extract trait interface for annotatable constructs and their builders (#1564).
 * New: Extract trait interface for documentable constructs and their builders (#1571).
 * New: Document the usage of `STAR` (#1572).
 * New: Add builder for `FunSpec` which accepts a `MemberName` (#1574).
 * Fix: Omit public modifier on override function or constructor parameters (#1550).
 * Fix: Correct handling of members in various types (#1558).
 * Fix: Function return types now default to `Unit` unless explicitly set (#1559).

    Previously, when `FunSpec` didn't have a return type specified and an expression body was produced, no return
    type would be emitted. However, starting from `1.14.0`, KotlinPoet will not add `Unit` as a return type in such
    cases. In order to correct the generated output, you are to specify the actual return type of the `FunSpec`.

    Before `1.14.0`, if omitted, no return type is produced:
    ```kotlin
    val funSpec = FunSpec.builder("foo")
      .addStatement("return 1")
      .build()
    ```
    ```kotlin
    public fun foo() = 1
    ```

    From `1.14.0`, the return type defaults to `Unit` if not otherwise set:
    ```kotlin
    val funSpec = FunSpec.builder("foo")
      .addStatement("return 1")
      .build()
    ```
    ```kotlin
    public fun foo(): Unit = 1 // ❌
    ```

    To fix it, explicitly define the return type:
    ```diff
     val funSpec = FunSpec.builder("foo")
    +  .returns(INT)
       .addStatement("return 1")
       .build()
    ```
    ```kotlin
    public fun foo(): Int = 1 // ✅
    ```

    Additionally, as part of this change, `FunSpec.returnType` has changed to be non-nullable. This is a source- and
    binary-compatible change, although if you were performing null-checks then new warnings may appear after upgrade.

 * Fix: Append nested class names to alias during name lookup (#1568).
 * Fix: Allow PropertySpec with context receivers and without getter or setter (#1575).

## Version 1.13.2

_2023-05-05_

Thanks to [@Squiry][Squiry] for contributing to this release.

* Fix: `KSType.toTypeName` fixed to work with aliased types (#1534).

## Version 1.13.1

_2023-04-28_

Thanks to [@rickclephas][rickclephas] for contributing to this release.

 * Fix: Look at canonical names instead of just package names when generating import aliases (#1519).
 * Fix: Ignore KSP annotation arguments without a value (#1523).
 * Fix: Fix arguments handling in `KSType.toTypeName()` (#1529).

## Version 1.13.0

_2023-04-06_

Thanks to [@popematt][popematt], [@bitPogo][bitPogo], [@mars885][mars885], [@sjudd][sjudd], [@Sironheart][Sironheart],
[@polarene][polarene], [@DeoTimeTheGithubUser][DeoTimeTheGithubUser], [@drawers][drawers] for contributing to this release.

 * New: Kotlin 1.8.0.
 * New: KSP 1.8.0-1.0.9.
 * New: Support context receivers on TypeSpecs + extract ContextReceivable API (#1269).
 * New: Optimize `OriginatingElements` and `TagMap` implementations (#1270).
 * New: Auto-generate import aliases for types and members (#1355).
 * New: Insert underscores into large decimal literals (#1384).
 * New: New factory function `FileSpec.builder(ClassName)` (#1397).
 * Fix: Fix StackOverflowError when calling `KSTypeArgument.toTypeName()` for a wildcard in a recursive type bound (#1272).
 * Fix: Fix transitive aliases (#1306).
 * Fix: Fix Aliases as TypeArgument (#1321).
 * Fix: Don't escape special characters inside raw strings (#1331).
 * Fix: Fix KSP interop's output of the annotation parameter value of type Char (#1338).
 * Fix: Fix KSP interop's output for primitive arrays (#1340).
 * Fix: Avoid emitting public if `shouldEmitPublicModifier` returns false (#1342).
 * Fix: Fix context receivers being rendered in an incorrect position when on a nullable/suspending `LambdaTypeName` (#1454).
 * Fix: Do not use `bestGuess` for `KClass.asClassName` (#1469).
 * Fix: Handle fake nested types with platform mapped parents (#1472).
 * Fix: Fix `TypeName` equals (#1477).
 * Fix: Make equals consistent with compareTo for `ClassName` (#1506).

## Version 1.12.0

_2022-06-13_

Thanks to [@WhosNickDoglio][WhosNickDoglio], [@sullis][sullis], [@DRSchlaubi][DRSchlaubi],
[@martinbonnin][martinbonnin], [@seriouslyhypersonic][seriouslyhypersonic], [@ephemient][ephemient],
[@dkilmer][dkilmer], [@aksh1618][aksh1618], [@zsqw123][zsqw123], [@roihershberg][roihershberg] for
contributing to this release.

 * New: Kotlin 1.7.0.
 * New: Add support for context receivers.
 * New: Add support for external property getter.
 * New: `interop-ksp` API promoted to stable.
 * Fix: Resolve enum constants when emitting types.
 * Fix: Fix type argument mapping when processing typealiases with KSP.
 * Fix: Properly unwrap `KSTypeAlias` with an unused type parameter.
 * Fix: Unwrap nested `KSTypeAlias`-es recursively.
 * Fix: Add support for context receivers `@PropertySpec` and fix issues with annotations.
 * Fix: Treat `header` and `impl` as keywords (workaround for KT-52315).
 * Fix: Use `%N` instead of `%L` for annotation arg names so keywords are handled.
 * Fix: Improve handling of long `return` expressions.

## Version 1.11.0

_2022-03-24_

Thanks to [@liujingxing][liujingxing] and [@BoD][BoD] for contributing to this release.

* New: Kotlin scripting support in `FileSpec`.

```kotlin
val spec = FileSpec.scriptBuilder("Taco")
  .addStatement("println(%S)", "hello world!")
  .addKotlinDefaultImports()
  .build()
```

Generates a `Taco.kts` file with the following contents:

```kotlin
println("hello world!")
```

* New: Emit trailing commas for multi-line parameters and annotations.
* New: Add `KSAnnotation.toAnnotationSpec()`.
* New: Add `Unit` and `CharSequence` conversions in `javapoet-interop`.
* New: Add support for default imports in `FileSpec`.
  * This is particularly oriented at scripting support, but can also be used in non-script files.
* New: Update to Kotlin 1.6.10.
* Fix: Fail compilation if you only pass one string to `ClassName`.
* Fix: Inline `val` property if its getter is `inline`.
* Fix: Add `yield` to the list of reserved keywords.
* Fix: Enforce only allowed parameter modifiers in `ParameterSpec` (i.e. `crossinline`, `vararg`, and `noinline`).
* Fix: Fix `CodeBlock`s in class delegation getting `toString()`'d instead of participating in code writing.
* Fix: Error when attempting to convert KSP error types (i.e. if `KSType.isError` is true) to `TypeName`.

## Version 1.10.2

_2021-10-22_

Thanks to [@glureau][glureau] and [@goooler][goooler] for contributing to this release.

* New: Switch `AnnotationSpec.get()` to use the `arrayOf()` syntax instead of `[]`.
* Fix: Don't wrap aliasing imports with long package names.
* Fix: Don't wrap type names inside line comments.
* Fix: Ignore Java's `@Deprecated` annotations on synthetic methods for annotations.

## Version 1.10.1

_2021-09-21_

Thanks to [@evant][evant] for contributing to this release.

 * Fix: Correct generation of typealiases with type args in KSP interop.
 * Fix: Add missing default `TypeParameterResolver.EMPTY` argument to
   `fun KSTypeArgument.toTypeName` in KSP interop.

## Version 1.10.0

_2021-09-20_

Thanks to [@martinbonnin][martinbonnin], [@idanakav][idanakav], [@goooler][goooler], and
[@anandwana001][anandwana001] for contributing to this release.

 * New: Add a new [KSP][ksp] interop artifact. See [docs][ksp-interop-docs] for more details.
 * New: Add a new [JavaPoet][javapoet] interop artifact. See [docs][javapoet-interop-docs] for more
   details.
 * New: Allow copying a `ParameterizedTypeName` with new type arguments via new `copy()` overload.
 * kotlinx-metadata artifacts have been consolidated to a single `com.squareup:kotlinpoet-metadata`
   maven artifact. The previous `kotlinpoet-metadata-*` subartifacts are no longer published.
 * New: `TypeNameAliasTag` has been moved to KotlinPoet's main artifact under `TypeAliasTag`, for
   reuse with KSP interop.
 * `ImmutableKm*` classes have been removed. They were deemed to be a needless abstraction over the base `kotlinx-metadata` Km types. All usages of these should be substituted with their non-immutable base types.
 * Fix: Fix self-referencing type variables in metadata parsing.
 * Fix: Use delicate APIs rather than noisy logging ones when converting annotation mirrors in
   `AnnotationSpec.get`.
 * Fix: Update error message when metadata cannot be read to a more actionable one.
 * Fix: Avoid escaping already escaped strings.
 * Add docs about `kotlin-reflect` usage.
 * Avoid using kotlin-reflect for looking up `Unit` types where possible.
 * Test all the way up to JDK 17.
 * Update Kotlin to 1.5.31.

## Version 1.9.0

_2021-06-22_

 * New: Kotlin 1.5.10.
 * New: Previously deprecated API to interop with Java reflection and Mirror API have been
   un-deprecated and marked with `@DelicateKotlinPoetApi` annotation.
 * New: `CodeBlock.Builder.withIndent` helper function.
 * New: Allow changing initializers and default values in `ParameterSpec.Builder` and
   `PropertySpec.Builder` after they were set.
 * New: `MemberName.isExtension` property that instructs KotlinPoet to always import the member,
   even if conflicting declarations are present in the same scope.
 * Fix: Escape member names that only contain underscores.
 * Fix: Always emit an empty primary constructor if it was set via `TypeSpec.primaryConstructor`.

## Version 1.8.0

_2021-03-29_

 * New: Kotlin 1.4.31.
 * New: Add `KModifier.VALUE` to support `value class` declarations.
 * New: Allow using a custom `ClassLoader` with `ReflectiveClassInspector`.
 * New: Update to kotlinx-metadata 0.2.0.
 * Fix: Ensure `ImmutableKmProperty.toMutable()` copies `fieldSignature`.
 * Fix: Prevent name clashes between an imported `MemberName` and a member in current scope.
 * Fix: Prevent name clashes between a type and a supertype with the same name.
 * Fix: Don't generate empty body for `expect` and `external` functions.
 * Fix: Don't allow `expect` or `external` classes to initialize supertypes.
 * Fix: Disallow delegate constructor calls in `external` classes.
 * Fix: Allow non-public primary constructors inside inline/value classes.
 * Fix: Allow init blocks inside inline/value classes.
 * Fix: Omit redundant `abstract` modifiers on members inside interfaces

## Version 1.7.2

_2020-10-20_

 * New: Detect expression bodies with `return·` and `throw·` prefixes.
 * Fix: Omit visibility modifiers on custom accessors.

## Version 1.7.1

_2020-10-15_

 * Fix: 1.7.0 was published using JDK 11 which set `"org.gradle.jvm.version"` to `"11"` in Gradle
   metadata, making it impossible to use the library on earlier Java versions (see
   [#999][issue-999]). 1.7.1 is published with JDK 8, which fixes the problem.

## Version 1.7.0

_2020-10-14_

 * New: Kotlin 1.4.10.
 * New: Generated code is now compatible with the [explicit API mode][explicit-api-mode] by default.
 * New: Escape soft and modifier keywords, in addition to hard keywords.
 * New: Improve enum constants generation for cleaner diffs.
 * New: Disallow setters on immutable properties.
 * New: Ensure trailing new lines in expression bodies.
 * New: Ensure trailing new lines after parameterless custom setters.
 * Fix: Don't auto-convert properties with custom accessors to primary constructor properties.
 * Fix: Don't allow parameterless setters with body.
 * Fix: Prevent auto-wrapping spaces inside escaped keywords.

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
 [explicit-api-mode]: https://kotlinlang.org/docs/reference/whatsnew14.html#explicit-api-mode-for-library-authors
 [issue-999]: https://github.com/square/kotlinpoet/issues/999
 [ksp]: https://github.com/google/ksp
 [ksp-interop-docs]: https://square.github.io/kotlinpoet/interop-ksp/
 [javapoet]: https://github.com/square/javapoet
 [javapoet-interop-docs]: https://square.github.io/kotlinpoet/interop-javapoet/
 [kt-18706]: https://youtrack.jetbrains.com/issue/KT-18706

 [martinbonnin]: https://github.com/martinbonnin
 [idanakav]: https://github.com/idanakav
 [goooler]: https://github.com/goooler
 [anandwana001]: https://github.com/anandwana001
 [evant]: https://github.com/evant
 [glureau]: https://github.com/glureau
 [liujingxing]: https://github.com/liujingxing
 [BoD]: https://github.com/BoD
 [WhosNickDoglio]: https://github.com/WhosNickDoglio
 [sullis]: https://github.com/sullis
 [DRSchlaubi]: https://github.com/DRSchlaubi
 [seriouslyhypersonic]: https://github.com/seriouslyhypersonic
 [ephemient]: https://github.com/ephemient
 [dkilmer]: https://github.com/dkilmer
 [aksh1618]: https://github.com/aksh1618
 [zsqw123]: https://github.com/zsqw123
 [roihershberg]: https://github.com/roihershberg
 [popematt]: https://github.com/popematt
 [bitPogo]: https://github.com/bitPogo
 [mars885]: https://github.com/mars885
 [sjudd]: https://github.com/sjudd
 [Sironheart]: https://github.com/Sironheart
 [polarene]: https://github.com/polarene
 [DeoTimeTheGithubUser]: https://github.com/DeoTimeTheGithubUser
 [drawers]: https://github.com/drawers
 [rickclephas]: https://github.com/rickclephas
 [Squiry]: https://github.com/Squiry
 [Omico]: https://github.com/Omico
 [RBusarow]: https://github.com/RBusarow
 [fejesjoco]: https://github.com/fejesjoco
 [takahirom]: https://github.com/takahirom
 [mcarleio]: https://github.com/mcarleio
 [gabrielittner]: https://github.com/gabrielittner
 [jisungbin]: https://github.com/jisungbin
 [hfhbd]: https://github.com/hfhbd
 [sgjesse]: https://github.com/sgjesse
 [sebek64]: https://github.com/sebek64
 [DanielGronau]: https://github.com/DanielGronau
 [mitasov-ra]: https://github.com/mitasov-ra
 [brokenhappy]: https://github.com/brokenhappy
 [tajobe]: https://github.com/tajobe
 [niyajali]: https://github.com/niyajali
 [ForteScarlet]: https://github.com/ForteScarlet
 [TrevorSStone]: https://github.com/TrevorSStone
 [RaoPrashanth]: https://github.com/RaoPrashanth
 [damianw]: https://github.com/damianw
 [ansehoon1999]: https://github.com/ansehoon1999
 [TheMrMilchmann]: https://github.com/TheMrMilchmann
 [IRus]: https://github.com/IRus

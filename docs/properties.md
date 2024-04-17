Properties
==========

Like parameters, properties can be created either with builders or by using convenient helper
methods:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .addModifiers(KModifier.PRIVATE)
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .addProperty(android)
  .addProperty("robot", String::class, KModifier.PRIVATE)
  .build()
```

Which generates:

```kotlin
class HelloWorld {
  private val android: String

  private val robot: String
}
```

The extended `Builder` form is necessary when a field has KDoc, annotations, or a field
initializer. Field initializers use the same [`String.format()`][formatter]-like syntax as the code
blocks above:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .addModifiers(KModifier.PRIVATE)
  .initializer("%S + %L", "Oreo v.", 8.1)
  .build()
```

Which generates:

```kotlin
private val android: String = "Oreo v." + 8.1
```

By default `PropertySpec.Builder` produces `val` properties. Use `mutable()` if you need a
`var`:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .mutable()
  .addModifiers(KModifier.PRIVATE)
  .initializer("%S + %L", "Oreo v.", 8.1)
  .build()
```

## Inline properties

The way KotlinPoet models inline properties deserves special mention. The following snippet of code:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .mutable()
  .addModifiers(KModifier.INLINE)
  .build()
```

will produce an error:

```
java.lang.IllegalArgumentException: KotlinPoet doesn't allow setting the inline modifier on
properties. You should mark either the getter, the setter, or both inline.
```

Indeed, a property marked with `inline` should have at least one accessor which will be inlined by
the compiler. Let's add a getter to this property:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .mutable()
  .getter(
    FunSpec.getterBuilder()
      .addModifiers(KModifier.INLINE)
      .addStatement("return %S", "foo")
      .build()
  )
  .build()
```

The result is the following:

```kotlin
var android: kotlin.String
  inline get() = "foo"
```

Now, what if we wanted to add a non-inline setter to the property above? We can do so without
modifying any of the code we wrote previously:

```kotlin
val android = PropertySpec.builder("android", String::class)
  .mutable()
  .getter(
    FunSpec.getterBuilder()
      .addModifiers(KModifier.INLINE)
      .addStatement("return %S", "foo")
      .build()
  )
  .setter(
    FunSpec.setterBuilder()
      .addParameter("value", String::class)
      .build()
  )
  .build()
```

We get the expected result:

```kotlin
var android: kotlin.String
  inline get() = "foo"
  set(`value`) {
  }
```

Finally, if we go back and add `KModifier.INLINE` to the setter, KotlinPoet can wrap it nicely and
produce the following result:

```kotlin
inline var android: kotlin.String
  get() = "foo"
  set(`value`) {
  }
```

Removing the modifier from either the getter or the setter will unwrap the expression back.

If, on the other hand, KotlinPoet had allowed marking a property `inline` directly, the programmer
would have had to manually add/remove the modifier whenever the state of the accessors changes in
order to get correct and compilable output. We're solving this problem by making accessors the
source of truth for the `inline` modifier.

 [formatter]: https://developer.android.com/reference/java/util/Formatter.html

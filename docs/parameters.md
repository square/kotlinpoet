Parameters
==========

Declare parameters on methods and constructors with either `ParameterSpec.builder()` or
`FunSpec`'s convenient `addParameter()` API:

```kotlin
val android = ParameterSpec.builder("android", String::class)
  .defaultValue("\"pie\"")
  .build()

val welcomeOverlords = FunSpec.builder("welcomeOverlords")
  .addParameter(android)
  .addParameter("robot", String::class)
  .build()
```

The code above generates:

```kotlin
fun welcomeOverlords(android: String = "pie", robot: String) {
}
```

The extended `Builder` form is necessary when the parameter has annotations (such as `@Inject`).

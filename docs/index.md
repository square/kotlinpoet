KotlinPoet
==========

`KotlinPoet` is a Kotlin and Java API for generating `.kt` source files.

Source file generation can be useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.


### Example

Here's a `HelloWorld` file:

```kotlin
class Greeter(val name: String) {
  fun greet() {
    println("""Hello, $name""")
  }
}

fun main(vararg args: String) {
  Greeter(args[0]).greet()
}
```

And this is the code to generate it with KotlinPoet:

```kotlin
val greeterClass = ClassName("", "Greeter")
val file = FileSpec.builder("", "HelloWorld")
    .addType(TypeSpec.classBuilder("Greeter")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .addFunction(FunSpec.builder("greet")
            .addStatement("println(%P)", "Hello, \$name")
            .build())
        .build())
    .addFunction(FunSpec.builder("main")
        .addParameter("args", String::class, VARARG)
        .addStatement("%T(args[0]).greet()", greeterClass)
        .build())
    .build()

file.writeTo(System.out)
```

The [KDoc][kdoc] catalogs the complete KotlinPoet API, which is inspired by [JavaPoet][javapoet].

### Code & Control Flow

Most of KotlinPoet's API uses immutable Kotlin objects. There's also builders, method chaining
and varargs to make the API friendly. KotlinPoet offers models for Kotlin files (`FileSpec`),
classes, interfaces & objects (`TypeSpec`), type aliases (`TypeAliasSpec`),
properties (`PropertySpec`), functions & constructors (`FunSpec`), parameters (`ParameterSpec`) and
annotations (`AnnotationSpec`).

But the _body_ of methods and constructors is not modeled. There's no expression class, no
statement class or syntax tree nodes. Instead, KotlinPoet uses strings for code blocks, and you can 
take advantage of Kotlin's multiline strings to make this look nice:

```kotlin
val main = FunSpec.builder("main")
    .addCode("""
        |var total = 0
        |for (i in 0 until 10) {
        |    total += i
        |}
        |""".trimMargin())
    .build()
```

Which generates this:

```kotlin
fun main() {
    var total = 0
    for (i in 0 until 10) {
        total += i
    }
}
```

There are additional APIs to assist with newlines, braces and indentation:

```kotlin
val main = FunSpec.builder("main")
    .addStatement("var total = 0")
    .beginControlFlow("for (i in 0 until 10)")
    .addStatement("total += i")
    .endControlFlow()
    .build()
```

This example is lame because the generated code is constant! Suppose instead of just adding 0 to 10,
we want to make the operation and range configurable. Here's a method that generates a method:

```kotlin
private fun computeRange(name: String, from: Int, to: Int, op: String): FunSpec {
  return FunSpec.builder(name)
      .returns(Int::class)
      .addStatement("var result = 1")
      .beginControlFlow("for (i in $from until $to)")
      .addStatement("result = result $op i")
      .endControlFlow()
      .addStatement("return result")
      .build()
}
```

And here's what we get when we call `computeRange("multiply10to20", 10, 20, "*")`:

```kotlin
fun multiply10to20(): kotlin.Int {
    var result = 1
    for (i in 10 until 20) {
        result = result * i
    }
    return result
}
```

Methods generating methods! And since KotlinPoet generates source instead of bytecode, you can
read through it to make sure it's right.

### %S for Strings

When emitting code that includes string literals, we can use **`%S`** to emit a **string**, complete
with wrapping quotation marks and escaping. Here's a program that emits 3 methods, each of which
returns its own name:

```kotlin
fun main(args: Array<String>) {
  val helloWorld = TypeSpec.classBuilder("HelloWorld")
      .addFunction(whatsMyNameYo("slimShady"))
      .addFunction(whatsMyNameYo("eminem"))
      .addFunction(whatsMyNameYo("marshallMathers"))
      .build()
  
  val kotlinFile = FileSpec.builder("com.example.helloworld", "HelloWorld")
      .addType(helloWorld)
      .build()
  
  kotlinFile.writeTo(System.out)
}

private fun whatsMyNameYo(name: String): FunSpec {
  return FunSpec.builder(name)
      .returns(String::class)
      .addStatement("return %S", name)
      .build()
}
```

In this case, using `%S` gives us quotation marks:

```kotlin
class HelloWorld {
    fun slimShady(): String = "slimShady"

    fun eminem(): String = "eminem"

    fun marshallMathers(): String = "marshallMathers"
}
```

### %P for String Templates

`%S` also handles the escaping of dollar signs (`$`), to avoid inadvertent creation of string 
templates, which may fail to compile in generated code:

```kotlin
val stringWithADollar = "Your total is " + "$" + "50"
val funSpec = FunSpec.builder("printTotal")
    .returns(String::class)
    .addStatement("return %S", stringWithADollar)
    .build()
```

produces:

```kotlin
fun printTotal(): String = "Your total is ${'$'}50"
```

If you need to generate string templates, use `%P`, which doesn't escape dollars:

```kotlin
val amount = 50
val stringWithADollar = "Your total is " + "$" + "amount"
val funSpec = FunSpec.builder("printTotal")
    .returns(String::class)
    .addStatement("return %P", stringWithADollar)
    .build()
```

produces:

```kotlin
fun printTotal(): String = "Your total is $amount"
```

You can also use `CodeBlock`s as arguments to `%P`, which is handy when you need to reference
importable types or members inside the string template:

```kotlin
val file = FileSpec.builder("com.example", "Digits")
    .addFunction(FunSpec.builder("print")
        .addParameter("digits", IntArray::class)
        .addStatement("println(%P)", buildCodeBlock {
          val contentToString = MemberName("kotlin.collections", "contentToString")
          add("These are the digits: \${digits.%M()}", contentToString)
        })
        .build())
    .build()
println(file)
```

The snippet above will produce the following output, handling the imports properly:

```kotlin
package com.example

import kotlin.IntArray
import kotlin.collections.contentToString

fun print(digits: IntArray) {
    println("""These are the digits: ${digits.contentToString()}""")
}
```

### %T for Types

KotlinPoet has rich built-in support for types, including automatic generation of `import`
statements. Just use **`%T`** to reference **types**:

```kotlin
val today = FunSpec.builder("today")
    .returns(Date::class)
    .addStatement("return %T()", Date::class)
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addFunction(today)
    .build()

val kotlinFile = FileSpec.builder("com.example.helloworld", "HelloWorld")
    .addType(helloWorld)
    .build()

kotlinFile.writeTo(System.out)
```

That generates the following `.kt` file, complete with the necessary `import`:

```kotlin
package com.example.helloworld

import java.util.Date

class HelloWorld {
    fun today(): Date = Date()
}
```

We passed `Date::class` to reference a class that just-so-happens to be available when we're
generating code. This doesn't need to be the case. Here's a similar example, but this one
references a class that doesn't exist (yet):

```kotlin
val hoverboard = ClassName("com.mattel", "Hoverboard")

val tomorrow = FunSpec.builder("tomorrow")
    .returns(hoverboard)
    .addStatement("return %T()", hoverboard)
    .build()
```

And that not-yet-existent class is imported as well:

```kotlin
package com.example.helloworld

import com.mattel.Hoverboard

class HelloWorld {
    fun tomorrow(): Hoverboard = Hoverboard()
}
```

The `ClassName` type is very important, and you'll need it frequently when you're using KotlinPoet.
It can identify any _declared_ class. Declared types are just the beginning of Kotlin's rich type
system: we also have arrays, parameterized types, wildcard types, lambda types and type variables. 
KotlinPoet has classes for building each of these:

```kotlin
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

val hoverboard = ClassName("com.mattel", "Hoverboard")
val list = ClassName("kotlin.collections", "List")
val arrayList = ClassName("kotlin.collections", "ArrayList")
val listOfHoverboards = list.parameterizedBy(hoverboard)
val arrayListOfHoverboards = arrayList.parameterizedBy(hoverboard)

val thing = ClassName("com.misc", "Thing")
val array = ClassName("kotlin", "Array")
val producerArrayOfThings = array.parameterizedBy(WildcardTypeName.producerOf(thing))

val beyond = FunSpec.builder("beyond")
    .returns(listOfHoverboards)
    .addStatement("val result = %T()", arrayListOfHoverboards)
    .addStatement("result += %T()", hoverboard)
    .addStatement("result += %T()", hoverboard)
    .addStatement("result += %T()", hoverboard)
    .addStatement("return result")
    .build()

val printThings = FunSpec.builder("printThings")
    .addParameter("things", producerArrayOfThings)
    .addStatement("println(things)")
    .build()
```

KotlinPoet will decompose each type and import its components where possible.

```kotlin
package com.example.helloworld

import com.mattel.Hoverboard
import com.misc.Thing
import kotlin.Array
import kotlin.collections.ArrayList
import kotlin.collections.List

class HelloWorld {
    fun beyond(): List<Hoverboard> {
        val result = ArrayList<Hoverboard>()
        result += Hoverboard()
        result += Hoverboard()
        result += Hoverboard()
        return result
    }

    fun printThings(things: Array<out Thing>) {
        println(things)
    }
}
```

#### Nullable Types

KotlinPoet supports nullable types. To convert a `TypeName` into its nullable counterpart, use the 
`copy()` method with `nullable` parameter set to `true`:

```kotlin
val java = PropertySpec.builder("java", String::class.asTypeName().copy(nullable = true))
    .mutable()
    .addModifiers(KModifier.PRIVATE)
    .initializer("null")
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addProperty(java)
    .addProperty("kotlin", String::class, KModifier.PRIVATE)
    .build()
```

generates:

```kotlin
class HelloWorld {
    private var java: String? = null

    private val kotlin: String
}
```

### %M for Members

Similar to types, KotlinPoet has a special placeholder for **members** (functions and properties),
which comes handy when your code needs to access top-level members and members declared inside
objects. Use **`%M`** to reference members, pass an instance of `MemberName` as the argument for the
placeholder, and KotlinPoet will handle imports automatically:

```kotlin
val createTaco = MemberName("com.squareup.tacos", "createTaco")
val isVegan = MemberName("com.squareup.tacos", "isVegan")
val file = FileSpec.builder("com.squareup.example", "TacoTest")
    .addFunction(FunSpec.builder("main")
        .addStatement("val taco = %M()", createTaco)
        .addStatement("println(taco.%M)", isVegan)
        .build())
    .build()
println(file)
```

The code above generates the following file:

```kotlin
package com.squareup.example

import com.squareup.tacos.createTaco
import com.squareup.tacos.isVegan

fun main() {
    val taco = createTaco()
    println(taco.isVegan)
}
```

As you can see, it's also possible to use `%M` to reference extension functions and properties. You 
just need to make sure the member can be imported without simple name collisions, otherwise 
importing will fail and the code generator output will not pass compilation. There's a way to work 
around such cases though - use `FileSpec.addAliasedImport()` to create an alias for a clashing 
`MemberName`:

```kotlin
val createTaco = MemberName("com.squareup.tacos", "createTaco")
val createCake = MemberName("com.squareup.cakes", "createCake")
val isTacoVegan = MemberName("com.squareup.tacos", "isVegan")
val isCakeVegan = MemberName("com.squareup.cakes", "isVegan")
val file = FileSpec.builder("com.squareup.example", "Test")
    .addAliasedImport(isTacoVegan, "isTacoVegan")
    .addAliasedImport(isCakeVegan, "isCakeVegan")
    .addFunction(FunSpec.builder("main")
        .addStatement("val taco = %M()", createTaco)
        .addStatement("val cake = %M()", createCake)
        .addStatement("println(taco.%M)", isTacoVegan)
        .addStatement("println(cake.%M)", isCakeVegan)
        .build())
    .build()
println(file)
``` 

KotlinPoet will produce an aliased import for `com.squareup.tacos2.isVegan`:

```kotlin
package com.squareup.example

import com.squareup.cakes.createCake
import com.squareup.tacos.createTaco
import com.squareup.cakes.isVegan as isCakeVegan
import com.squareup.tacos.isVegan as isTacoVegan

fun main() {
    val taco = createTaco()
    val cake = createCake()
    println(taco.isTacoVegan)
    println(cake.isCakeVegan)
}
```

#### MemberName and operators

MemberName also supports operators, you can use `MemberName(String, KOperator)` or `MemberName(ClassName, KOperator)`
to import and reference operators.

```kotlin
val taco = ClassName("com.squareup.tacos", "Taco")
val meat = ClassName("com.squareup.tacos.ingredient", "Meat")
val iterator = MemberName("com.squareup.tacos.internal", KOperator.ITERATOR)
val minusAssign = MemberName("com.squareup.tacos.internal", KOperator.MINUS_ASSIGN)
val file = FileSpec.builder("com.example", "Test")
    .addFunction(FunSpec.builder("makeTacoHealthy")
        .addParameter("taco", taco)
        .beginControlFlow("for (ingredient %M taco)", iterator)
        .addStatement("if (ingredient is %T) taco %M ingredient", meat, minusAssign)
        .endControlFlow()
        .addStatement("return taco")
        .build())
    .build()
println(file)
```

KotlinPoet will import the extension operator functions and emit the operator.

```kotlin
package com.example

import com.squareup.tacos.Taco
import com.squareup.tacos.ingredient.Meat
import com.squareup.tacos.internal.iterator
import com.squareup.tacos.internal.minusAssign

fun makeTacoHealthy(taco: Taco) {
  for (ingredient in taco) {
    if (ingredient is Meat) taco -= ingredient
  }
  return taco
}

```

### %N for Names

Generated code is often self-referential. Use **`%N`** to refer to another generated declaration by
its name. Here's a method that calls another:

```kotlin
fun byteToHex(b: Int): String {
  val result = CharArray(2)
  result[0] = hexDigit((b ushr 4) and 0xf)
  result[1] = hexDigit(b and 0xf)
  return String(result)
}

fun hexDigit(i: Int): Char {
  return (if (i < 10) i + '0'.toInt() else i - 10 + 'a'.toInt()).toChar()
}
```

When generating the code above, we pass the `hexDigit()` method as an argument to the `byteToHex()`
method using `%N`:

```kotlin
val hexDigit = FunSpec.builder("hexDigit")
    .addParameter("i", Int::class)
    .returns(Char::class)
    .addStatement("return (if (i < 10) i + '0'.toInt() else i - 10 + 'a'.toInt()).toChar()")
    .build()

val byteToHex = FunSpec.builder("byteToHex")
    .addParameter("b", Int::class)
    .returns(String::class)
    .addStatement("val result = CharArray(2)")
    .addStatement("result[0] = %N((b ushr 4) and 0xf)", hexDigit)
    .addStatement("result[1] = %N(b and 0xf)", hexDigit)
    .addStatement("return String(result)")
    .build()
```

Another handy feature that `%N` provides is automatically escaping names that contain illegal 
identifier characters with double ticks. Suppose your code creates a `MemberName` with a Kotlin
keyword as the simple name:

```kotlin
val taco = ClassName("com.squareup.tacos", "Taco")
val packager = ClassName("com.squareup.tacos", "TacoPackager")
val file = FileSpec.builder("com.example", "Test")
    .addFunction(FunSpec.builder("packageTacos")
        .addParameter("tacos", LIST.parameterizedBy(taco))
        .addParameter("packager", packager)
        .addStatement("packager.%N(tacos)", packager.member("package"))
        .build())
    .build()
```

`%N` will escape the name for you, ensuring that the output will pass compilation:

```kotlin
package com.example

import com.squareup.tacos.Taco
import com.squareup.tacos.TacoPackager
import kotlin.collections.List

fun packageTacos(tacos: List<Taco>, packager: TacoPackager) {
  packager.`package`(tacos)
}
```

### %L for Literals

Although Kotlin's string templates usually work well in cases when you want to include literals into 
generated code, KotlinPoet offers additional syntax inspired-by but incompatible-with
[`String.format()`][formatter]. It accepts **`%L`** to emit a **literal** value in the output. This
works just like `Formatter`'s `%s`:

```kotlin
private fun computeRange(name: String, from: Int, to: Int, op: String): FunSpec {
  return FunSpec.builder(name)
      .returns(Int::class)
      .addStatement("var result = 0")
      .beginControlFlow("for (i in %L until %L)", from, to)
      .addStatement("result = result %L i", op)
      .endControlFlow()
      .addStatement("return result")
      .build()
}
```

Literals are emitted directly to the output code with no escaping. Arguments for literals may be
strings, primitives, and a few KotlinPoet types described below.

### Code block format strings

Code blocks may specify the values for their placeholders in a few ways. Only one style may be used
for each operation on a code block.

#### Relative Arguments

Pass an argument value for each placeholder in the format string to `CodeBlock.add()`. In each
example, we generate code to say "I ate 3 tacos"

```kotlin
CodeBlock.builder().add("I ate %L %L", 3, "tacos")
```

#### Positional Arguments

Place an integer index (1-based) before the placeholder in the format string to specify which
 argument to use.

```kotlin
CodeBlock.builder().add("I ate %2L %1L", "tacos", 3)
```

#### Named Arguments

Use the syntax `%argumentName:X` where `X` is the format character and call `CodeBlock.addNamed()`
with a map containing all argument keys in the format string. Argument names use characters in
`a-z`, `A-Z`, `0-9`, and `_`, and must start with a lowercase character.

```kotlin
val map = LinkedHashMap<String, Any>()
map += "food" to "tacos"
map += "count" to 3
CodeBlock.builder().addNamed("I ate %count:L %food:L", map)
```
  
### Functions

All of the above functions have a code body. Use `KModifier.ABSTRACT` to get a function without any
body. This is only legal if it is enclosed by an abstract class or an interface.

```kotlin
val flux = FunSpec.builder("flux")
    .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(KModifier.ABSTRACT)
    .addFunction(flux)
    .build()
```

Which generates this:

```kotlin
abstract class HelloWorld {
    protected abstract fun flux()
}
```

The other modifiers work where permitted.

Methods also have parameters, varargs, KDoc, annotations, type variables, return type and receiver
type for extension functions. All of these are configured with `FunSpec.Builder`.

#### Extension functions

Extension functions can be generated by specifying a `receiver`.

```kotlin
val square = FunSpec.builder("square")
    .receiver(Int::class)
    .returns(Int::class)
    .addStatement("var s = this * this")
    .addStatement("return s")
    .build()
```

Which outputs:

```kotlin
fun Int.square(): Int {
  val s = this * this
  return s
}
```

#### Single-expression functions

KotlinPoet can recognize single-expression functions and print them out properly. It treats
each function with a body that starts with `return` as a single-expression function:

```kotlin
val abs = FunSpec.builder("abs")
    .addParameter("x", Int::class)
    .returns(Int::class)
    .addStatement("return if (x < 0) -x else x")
    .build()
```

Which outputs:

```kotlin
fun abs(x: Int): Int = if (x < 0) -x else x
```

#### Default function arguments

Consider the example below.
Function argument `b` has a default value of 0 to avoid overloading this function.

```kotlin
fun add(a: Int, b: Int = 0) {
  print("a + b = ${ a + b }")
}
```

Use the `defaultValue()` builder function to declare default value for a function argument.

```kotlin
FunSpec.builder("add")
    .addParameter("a", Int::class)
    .addParameter(ParameterSpec.builder("b", Int::class)
        .defaultValue("%L", 0)
        .build())
    .addStatement("print(\"a + b = ${ a + b }\")")
    .build()
```

#### Spaces wrap by default!

In order to provide meaningful formatting, KotlinPoet would replace spaces, found in blocks of code,
with new line symbols, in cases when the line of code exceeds the length limit. Let's take this 
function for example:

```kotlin
val funSpec = FunSpec.builder("foo")
    .addStatement("return (100..10000).map { number -> number * number }.map { number -> number.toString() }.also { string -> println(string) }")
    .build()
```

Depending on where it's found in the file, it may end up being printed out like this:

```kotlin
fun foo() = (100..10000).map { number -> number * number }.map { number -> number.toString() }.also 
{ string -> println(string) }
```

Unfortunately this code is broken: the compiler expects `also` and `{` to be on the same line. 
KotlinPoet is unable to understand the context of the expression and fix the formatting for you, but 
there's a trick you can use to declare a non-breaking space - use the `·` symbol where you would 
otherwise use a space. Let's apply this to our example:

```kotlin
val funSpec = FunSpec.builder("foo")
    .addStatement("return (100..10000).map·{ number -> number * number }.map·{ number -> number.toString() }.also·{ string -> println(string) }")
    .build()
```

This will now produce the following result:

```kotlin
fun foo() = (100..10000).map { number -> number * number }.map { number -> number.toString()
}.also { string -> println(string) }
```

The code is now correct and will compile properly. It still doesn't look perfect - you can play with
replacing other spaces in the code block with `·` symbols to achieve better formatting.

### Constructors

`FunSpec` is a slight misnomer; it can also be used for constructors:

```kotlin
val flux = FunSpec.constructorBuilder()
    .addParameter("greeting", String::class)
    .addStatement("this.%N = %N", "greeting", "greeting")
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addProperty("greeting", String::class, KModifier.PRIVATE)
    .addFunction(flux)
    .build()
```

Which generates this:

```kotlin
class HelloWorld {
    private val greeting: String

    constructor(greeting: String) {
        this.greeting = greeting
    }
}
```

For the most part, constructors work just like methods. When emitting code, KotlinPoet will place
constructors before methods in the output file.

Often times you'll need to generate the primary constructor for a class:

```kotlin
val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .primaryConstructor(flux)
    .addProperty("greeting", String::class, KModifier.PRIVATE)
    .build()
``` 

This code, however, generates the following:

```kotlin
class HelloWorld(greeting: String) {
    private val greeting: String
    init {
        this.greeting = greeting
    }
}
```

By default, KotlinPoet won't merge primary constructor parameters and properties, even if they share
the same name. To achieve the effect, you have to tell KotlinPoet that the property is initialized 
via the constructor parameter:

```kotlin
val flux = FunSpec.constructorBuilder()
    .addParameter("greeting", String::class)
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .primaryConstructor(flux)
    .addProperty(PropertySpec.builder("greeting", String::class)
        .initializer("greeting")
        .addModifiers(KModifier.PRIVATE)
        .build())
    .build()
```

Now we're getting the following output:

```kotlin
class HelloWorld(private val greeting: String)
```

Notice that KotlinPoet omits `{}` for classes with empty bodies.

### Parameters

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

### Properties

Like parameters, properties can be created either with builders or by using convenient helper methods:

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

#### Inline properties

The way KotlinPoet models inline properties deserves special mention. The following snippet of code:

```kotlin
val android = PropertySpec.builder("android", String::class)
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
    .getter(FunSpec.getterBuilder()
        .addModifiers(KModifier.INLINE)
        .addStatement("return %S", "foo")
        .build())
    .build()
```

The result is the following:

```kotlin
val android: kotlin.String
    inline get() = "foo"
```

Now, what if we wanted to add a non-inline setter to the property above? We can do so without 
modifying any of the code we wrote previously:

```kotlin
val android = PropertySpec.builder("android", String::class)
    .getter(FunSpec.getterBuilder()
        .addModifiers(KModifier.INLINE)
        .addStatement("return %S", "foo")
        .build())
    .setter(FunSpec.setterBuilder()
        .addParameter("value", String::class)
        .build())
    .build()
```

We get the expected result:

```kotlin
val android: kotlin.String
    inline get() = "foo"
    set(value) {
    }
```

Finally, if we go back and add `KModifier.INLINE` to the setter, KotlinPoet can wrap it nicely and
produce the following result:

```kotlin
inline val android: kotlin.String
    get() = "foo"
    set(value) {
    }
```

Removing the modifier from either the getter or the setter will unwrap the expression back. 

If, on the other hand, KotlinPoet had allowed marking a property `inline` directly, the programmer 
would have had to manually add/remove the modifier whenever the state of the accessors changes in 
order to get correct and compilable output. We're solving this problem by making accessors the 
source of truth for the `inline` modifier.  

### Interfaces

KotlinPoet has no trouble with interfaces. Note that interface methods must always be `ABSTRACT`. 
The modifier is necessary when defining the interface:

```kotlin
val helloWorld = TypeSpec.interfaceBuilder("HelloWorld")
    .addProperty("buzz", String::class)
    .addFunction(FunSpec.builder("beep")
        .addModifiers(KModifier.ABSTRACT)
        .build())
    .build()
```

But these modifiers are omitted when the code is generated. These are the default so we don't need
to include them for `kotlinc`'s benefit!

```kotlin
interface HelloWorld {
    val buzz: String

    fun beep()
}
```

Kotlin 1.4 adds support for functional interfaces via `fun interface` syntax. To create this in 
KotlinPoet, use `TypeSpec.funInterfaceBuilder()`.

```kotlin
val helloWorld = TypeSpec.funInterfaceBuilder("HelloWorld")
    .addFunction(FunSpec.builder("beep")
        .addModifiers(KModifier.ABSTRACT)
        .build())
    .build()

// Generates...
fun interface HelloWorld {
  fun beep()
}
```

### Objects

KotlinPoet supports objects:

```kotlin
val helloWorld = TypeSpec.objectBuilder("HelloWorld")
    .addProperty(PropertySpec.builder("buzz", String::class)
        .initializer("%S", "buzz")
        .build())
    .addFunction(FunSpec.builder("beep")
        .addStatement("println(%S)", "Beep!")
        .build())
    .build()
```

Similarly, you can create companion objects and add them to classes using `addType()`: 

```kotlin
val companion = TypeSpec.companionObjectBuilder()
    .addProperty(PropertySpec.builder("buzz", String::class)
        .initializer("%S", "buzz")
        .build())
    .addFunction(FunSpec.builder("beep")
        .addStatement("println(%S)", "Beep!")
        .build())
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addType(companion)
    .build()
```

You can provide an optional name for a companion object.

### Enums

Use `enumBuilder` to create the enum type, and `addEnumConstant()` for each value:

```kotlin
val helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addEnumConstant("ROCK")
    .addEnumConstant("SCISSORS")
    .addEnumConstant("PAPER")
    .build()
```

To generate this:

```kotlin
enum class Roshambo {
    ROCK,

    SCISSORS,

    PAPER
}
```

Fancy enums are supported, where the enum values override methods or call a superclass constructor.
Here's a comprehensive example:

```kotlin
val helloWorld = TypeSpec.enumBuilder("Roshambo")
    .primaryConstructor(FunSpec.constructorBuilder()
        .addParameter("handsign", String::class)
        .build())
    .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder()
        .addSuperclassConstructorParameter("%S", "fist")
        .addFunction(FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return %S", "avalanche!")
            .returns(String::class)
            .build())
        .build())
    .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder()
        .addSuperclassConstructorParameter("%S", "peace")
        .build())
    .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder()
        .addSuperclassConstructorParameter("%S", "flat")
        .build())
    .addProperty(PropertySpec.builder("handsign", String::class, KModifier.PRIVATE)
        .initializer("handsign")
        .build())
    .build()
```

Which generates this:

```kotlin
enum class Roshambo(private val handsign: String) {
    ROCK("fist") {
        override fun toString(): String = "avalanche!"
    },

    SCISSORS("peace"),

    PAPER("flat");
}
```

### Anonymous Inner Classes

In the enum code, we used `TypeSpec.anonymousClassBuilder()`. Anonymous inner classes can also be 
used in code blocks. They are values that can be referenced with `%L`:

```kotlin
val comparator = TypeSpec.anonymousClassBuilder()
    .addSuperinterface(Comparator::class.parameterizedBy(String::class))
    .addFunction(FunSpec.builder("compare")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("a", String::class)
        .addParameter("b", String::class)
        .returns(Int::class)
        .addStatement("return %N.length - %N.length", "a", "b")
        .build())
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addFunction(FunSpec.builder("sortByLength")
        .addParameter("strings", List::class.parameterizedBy(String::class))
        .addStatement("%N.sortedWith(%L)", "strings", comparator)
        .build())
    .build()
```

This generates a method that contains a class that contains a method:

```kotlin
class HelloWorld {
    fun sortByLength(strings: List<String>) {
        strings.sortedWith(object : Comparator<String> {
            override fun compare(a: String, b: String): Int = a.length - b.length
        })
    }
}
```

One particularly tricky part of defining anonymous inner classes is the arguments to the superclass
constructor. To pass them use `TypeSpec.Builder`'s `addSuperclassConstructorParameter()` method. 

### Annotations

Simple annotations are easy:

```kotlin
val test = FunSpec.builder("test string equality")
    .addAnnotation(Test::class)
    .addStatement("assertThat(%1S).isEqualTo(%1S)", "foo")
    .build()
```

Which generates this function with an `@Test` annotation:

```kotlin
@Test
fun `test string equality`() {
    assertThat("foo").isEqualTo("foo")
}
```

Use `AnnotationSpec.builder()` to set properties on annotations:

```kotlin
val logRecord = FunSpec.builder("recordEvent")
    .addModifiers(KModifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(Headers::class)
        .addMember("accept = %S", "application/json; charset=utf-8")
        .addMember("userAgent = %S", "Square Cash")
        .build())
    .addParameter("logRecord", LogRecord::class)
    .returns(LogReceipt::class)
    .build()
```

Which generates this annotation with `accept` and `userAgent` properties:

```kotlin
@Headers(
        accept = "application/json; charset=utf-8",
        userAgent = "Square Cash"
)
abstract fun recordEvent(logRecord: LogRecord): LogReceipt
```

When you get fancy, annotation values can be annotations themselves. Use `%L` for embedded
annotations:

```kotlin
val headerList = ClassName("", "HeaderList")
val header = ClassName("", "Header")
val logRecord = FunSpec.builder("recordEvent")
    .addModifiers(KModifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(headerList)
        .addMember(
            "[\n⇥%L,\n%L⇤\n]",
            AnnotationSpec.builder(header)
                .addMember("name = %S", "Accept")
                .addMember("value = %S", "application/json; charset=utf-8")
                .build(),
            AnnotationSpec.builder(header)
                .addMember("name = %S", "User-Agent")
                .addMember("value = %S", "Square Cash")
                .build())
        .build())
    .addParameter("logRecord", logRecordName)
    .returns(logReceipt)
    .build()
```

Which generates this:

```kotlin
@HeaderList([
    Header(name = "Accept", value = "application/json; charset=utf-8"),
    Header(name = "User-Agent", value = "Square Cash")
])
abstract fun recordEvent(logRecord: LogRecord): LogReceipt
```

KotlinPoet supports use-site targets for annotations:

```kotlin
val utils = FileSpec.builder("com.example", "Utils")
    .addAnnotation(AnnotationSpec.builder(JvmName::class)
        .useSiteTarget(UseSiteTarget.FILE)
        .build())
    .addFunction(FunSpec.builder("abs")
        .receiver(Int::class)
        .returns(Int::class)
        .addStatement("return if (this < 0) -this else this")
        .build())
    .build()
```

Will output this:

```kotlin
@file:JvmName

package com.example

import kotlin.Int
import kotlin.jvm.JvmName

fun Int.abs(): Int = if (this < 0) -this else this
```

### Type Aliases

KotlinPoet provides API for creating Type Aliases, which supports simple class names, parameterized
types and lambdas:

```kotlin
val fileTable = Map::class.asClassName()
    .parameterizedBy(TypeVariableName("K"), Set::class.parameterizedBy(File::class))
val predicate = LambdaTypeName.get(parameters = *arrayOf(TypeVariableName("T")),
    returnType = Boolean::class.asClassName())
val helloWorld = FileSpec.builder("com.example", "HelloWorld")
    .addTypeAlias(TypeAliasSpec.builder("Word", String::class).build())
    .addTypeAlias(TypeAliasSpec.builder("FileTable<K>", fileTable).build())
    .addTypeAlias(TypeAliasSpec.builder("Predicate<T>", predicate).build())
    .build()
```

Which generates the following:

```kotlin
package com.example

import java.io.File
import kotlin.Boolean
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set

typealias Word = String

typealias FileTable<K> = Map<K, Set<File>>

typealias Predicate<T> = (T) -> Boolean
```

### Callable References

[Callable references](https://kotlinlang.org/docs/reference/reflection.html#callable-references) to
constructors, functions, and properties may be emitted via:

- `ClassName.constructorReference()` for constructors
- `MemberName.reference()` for functions and properties

For example,

```kotlin
val helloClass = ClassName("com.example.hello", "Hello")
val worldFunction: MemberName = helloClass.member("world")
val byeProperty: MemberName = helloClass.nestedClass("World").member("bye")

val factoriesFun = FunSpec.builder("factories")
    .addStatement("val hello = %L", helloClass.constructorReference())
    .addStatement("val world = %L", worldFunction.reference())
    .addStatement("val bye = %L", byeProperty.reference())
    .build()

FileSpec.builder("com.example", "HelloWorld")
    .addFunction(factoriesFun)
    .build()
```

would generate:

```kotlin
package com.example

import com.example.hello.Hello

fun factories() {
  val hello = ::Hello
  val world = Hello::world
  val bye = Hello.World::bye
}
```

Top-level classes and members with conflicting names may require aliased imports, as with
[member names](#m-for-members).

Download
--------

Download [the latest .jar][dl] or depend via Maven:

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>kotlinpoet</artifactId>
  <version>1.6.0</version>
</dependency>
```

or Gradle:

```groovy
implementation("com.squareup:kotlinpoet:1.6.0")
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


License
-------

    Copyright 2017 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=kotlinpoet&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/com/squareup/kotlinpoet/
 [kdoc]: https://square.github.io/kotlinpoet/1.x/kotlinpoet/com.squareup.kotlinpoet/
 [javapoet]: https://github.com/square/javapoet/
 [formatter]: https://developer.android.com/reference/java/util/Formatter.html

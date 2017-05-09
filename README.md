KotlinPoet
==========

`KotlinPoet` is a Kotlin and Java API for generating `.kt` source files.

Source file generation can be useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.


### Example

Here's a (boring) `HelloWorld` class:

```kotlin
package com.example.helloworld

fun main(vararg args: String) {
  println("Hello, KotlinPoet!")
}
```

And this is the (exciting) code to generate it with KotlinPoet:

```java
FunSpec main = FunSpec.builder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("%T.out.println(%S)", System.class, "Hello, KotlinPoet!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addFun(main)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.writeTo(System.out);
```

To declare the main function, we've created a `FunSpec` "main" configured with modifiers, return
type, parameters and code statements. We add the main function to a `HelloWorld` class, and then add
that to a `HelloWorld.java` file.

In this case we write the file to `System.out`, but we could also get it as a string
(`JavaFile.toString()`) or write it to the file system (`KotlinPoet.writeTo()`).

The [KDoc][kdoc] catalogs the complete KotlinPoet API, which we explore below.

### Code & Control Flow

Most of KotlinPoet's API uses plain old immutable Java objects. There's also builders, function
chaining and varargs to make the API friendly. KotlinPoet offers models for classes & interfaces
(`TypeSpec`), fields (`FieldSpec`), functions & constructors (`FunSpec`), parameters
(`ParameterSpec`) and annotations (`AnnotationSpec`).

But the _body_ of functions and constructors is not modeled. There's no expression class, no
statement class or syntax tree nodes. Instead, KotlinPoet uses strings for code blocks:

```java
FunSpec main = FunSpec.builder("main")
    .addCode(""
        + "int total = 0;\n"
        + "for (int i = 0; i < 10; i++) {\n"
        + "  total += i;\n"
        + "}\n")
    .build();
```

Which generates this:

```java
void main() {
  int total = 0;
  for (int i = 0; i < 10; i++) {
    total += i;
  }
}
```

The manual semicolons, line wrapping, and indentation are tedious and so KotlinPoet offers APIs to
make it easier. There's `addStatement()` which takes care of semicolons and newline, and
`beginControlFlow()` + `endControlFlow()` which are used together for braces, newlines, and
indentation:

```java
FunSpec main = FunSpec.builder("main")
    .addStatement("int total = 0")
    .beginControlFlow("for (int i = 0; i < 10; i++)")
    .addStatement("total += i")
    .endControlFlow()
    .build();
```

This example is lame because the generated code is constant! Suppose instead of just adding 0 to 10,
we want to make the operation and range configurable. Here's a function that generates a function:

```java
private FunSpec computeRange(String name, int from, int to, String op) {
  return FunSpec.builder(name)
      .returns(int.class)
      .addStatement("int result = 0")
      .beginControlFlow("for (int i = " + from + "; i < " + to + "; i++)")
      .addStatement("result = result " + op + " i")
      .endControlFlow()
      .addStatement("return result")
      .build();
}
```

And here's what we get when we call `computeRange("multiply10to20", 10, 20, "*")`:

```java
int multiply10to20() {
  int result = 0;
  for (int i = 10; i < 20; i++) {
    result = result * i;
  }
  return result;
}
```

Functions generating functions! And since KotlinPoet generates source instead of bytecode, you can
read through it to make sure it's right.


### %L for Literals

The string-concatenation in calls to `beginControlFlow()` and `addStatement` is distracting. Too
many operators. To address this, KotlinPoet offers a syntax inspired-by but incompatible-with
[`String.format()`][formatter]. It accepts **`%L`** to emit a **literal** value in the output. This
works just like `Formatter`'s `%s`:

```java
private FunSpec computeRange(String name, int from, int to, String op) {
  return FunSpec.builder(name)
      .returns(int.class)
      .addStatement("int result = 0")
      .beginControlFlow("for (int i = %L; i < %L; i++)", from, to)
      .addStatement("result = result %L i", op)
      .endControlFlow()
      .addStatement("return result")
      .build();
}
```

Literals are emitted directly to the output code with no escaping. Arguments for literals may be
strings, primitives, and a few KotlinPoet types described below.

### %S for Strings

When emitting code that includes string literals, we can use **`%S`** to emit a **string**, complete
with wrapping quotation marks and escaping. Here's a program that emits 3 functions, each of which
returns its own name:

```java
public static void main(String[] args) throws Exception {
  TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addFun(whatsMyName("slimShady"))
      .addFun(whatsMyName("eminem"))
      .addFun(whatsMyName("marshallMathers"))
      .build();

  JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
      .build();

  javaFile.writeTo(System.out);
}

private static FunSpec whatsMyName(String name) {
  return FunSpec.builder(name)
      .returns(String.class)
      .addStatement("return %S", name)
      .build();
}
```

In this case, using `%S` gives us quotation marks:

```java
public final class HelloWorld {
  String slimShady() {
    return "slimShady";
  }

  String eminem() {
    return "eminem";
  }

  String marshallMathers() {
    return "marshallMathers";
  }
}
```

### %T for Types

We Java programmers love our types: they make our code easier to understand. And KotlinPoet is on
board. It has rich built-in support for types, including automatic generation of `import`
statements. Just use **`%T`** to reference **types**:

```java
FunSpec today = FunSpec.builder("today")
    .returns(Date.class)
    .addStatement("return new %T()", Date.class)
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addFun(today)
    .build();

JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();

javaFile.writeTo(System.out);
```

That generates the following `.java` file, complete with the necessary `import`:

```java
package com.example.helloworld;

import java.util.Date;

public final class HelloWorld {
  Date today() {
    return new Date();
  }
}
```

We passed `Date.class` to reference a class that just-so-happens to be available when we're
generating code. This doesn't need to be the case. Here's a similar example, but this one
references a class that doesn't exist (yet):

```java
ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");

FunSpec today = FunSpec.builder("tomorrow")
    .returns(hoverboard)
    .addStatement("return new %T()", hoverboard)
    .build();
```

And that not-yet-existent class is imported as well:

```java
package com.example.helloworld;

import com.mattel.Hoverboard;

public final class HelloWorld {
  Hoverboard tomorrow() {
    return new Hoverboard();
  }
}
```

The `ClassName` type is very important, and you'll need it frequently when you're using KotlinPoet.
It can identify any _declared_ class. Declared types are just the beginning of Java's rich type
system: we also have arrays, parameterized types, wildcard types, and type variables. KotlinPoet has
classes for building each of these:

```java
ClassName hoverboard = ClassName.get("com.mattel", "Hoverboard");
ClassName list = ClassName.get("java.util", "List");
ClassName arrayList = ClassName.get("java.util", "ArrayList");
TypeName listOfHoverboards = ParameterizedTypeName.get(list, hoverboard);

FunSpec beyond = FunSpec.builder("beyond")
    .returns(listOfHoverboards)
    .addStatement("%T result = new %T<>()", listOfHoverboards, arrayList)
    .addStatement("result.add(new %T())", hoverboard)
    .addStatement("result.add(new %T())", hoverboard)
    .addStatement("result.add(new %T())", hoverboard)
    .addStatement("return result")
    .build();
```

KotlinPoet will decompose each type and import its components where possible.

```java
package com.example.helloworld;

import com.mattel.Hoverboard;
import java.util.ArrayList;
import java.util.List;

public final class HelloWorld {
  List<Hoverboard> beyond() {
    List<Hoverboard> result = new ArrayList<>();
    result.add(new Hoverboard());
    result.add(new Hoverboard());
    result.add(new Hoverboard());
    return result;
  }
}
```

#### Import static

KotlinPoet supports `import static`. It does it via explicitly collecting type member names. Let's
enhance the previous example with some static sugar:

```java
...
ClassName namedBoards = ClassName.get("com.mattel", "Hoverboard", "Boards");

FunSpec beyond = FunSpec.builder("beyond")
    .returns(listOfHoverboards)
    .addStatement("%T result = new %T<>()", listOfHoverboards, arrayList)
    .addStatement("result.add(%T.createNimbus(2000))", hoverboard)
    .addStatement("result.add(%T.createNimbus(\"2001\"))", hoverboard)
    .addStatement("result.add(%T.createNimbus(%T.THUNDERBOLT))", hoverboard, namedBoards)
    .addStatement("%T.sort(result)", Collections.class)
    .addStatement("return result.isEmpty() ? %T.emptyList() : result", Collections.class)
    .build();

TypeSpec hello = TypeSpec.classBuilder("HelloWorld")
    .addFun(beyond)
    .build();

JavaFile.builder("com.example.helloworld", hello)
    .addStaticImport(hoverboard, "createNimbus")
    .addStaticImport(namedBoards, "*")
    .addStaticImport(Collections.class, "*")
    .build();
```

KotlinPoet will first add your `import static` block to the file as configured, match and mangle
all calls accordingly and also import all other types as needed.

```java
package com.example.helloworld;

import static com.mattel.Hoverboard.Boards.*;
import static com.mattel.Hoverboard.createNimbus;
import static java.util.Collections.*;

import com.mattel.Hoverboard;
import java.util.ArrayList;
import java.util.List;

class HelloWorld {
  List<Hoverboard> beyond() {
    List<Hoverboard> result = new ArrayList<>();
    result.add(createNimbus(2000));
    result.add(createNimbus("2001"));
    result.add(createNimbus(THUNDERBOLT));
    sort(result);
    return result.isEmpty() ? emptyList() : result;
  }
}
```

### %N for Names

Generated code is often self-referential. Use **`%N`** to refer to another generated declaration by
its name. Here's a function that calls another:

```java
public String byteToHex(int b) {
  char[] result = new char[2];
  result[0] = hexDigit((b >>> 4) & 0xf);
  result[1] = hexDigit(b & 0xf);
  return new String(result);
}

public char hexDigit(int i) {
  return (char) (i < 10 ? i + '0' : i - 10 + 'a');
}
```

When generating the code above, we pass the `hexDigit()` function as an argument to the `byteToHex()`
functino using `%N`:

```java
FunSpec hexDigit = FunSpec.builder("hexDigit")
    .addParameter(int.class, "i")
    .returns(char.class)
    .addStatement("return (char) (i < 10 ? i + '0' : i - 10 + 'a')")
    .build();

FunSpec byteToHex = FunSpec.builder("byteToHex")
    .addParameter(int.class, "b")
    .returns(String.class)
    .addStatement("char[] result = new char[2]")
    .addStatement("result[0] = %N((b >>> 4) & 0xf)", hexDigit)
    .addStatement("result[1] = %N(b & 0xf)", hexDigit)
    .addStatement("return new String(result)")
    .build();
```

### Code block format strings

Code blocks may specify the values for their placeholders in a few ways. Only one style may be used
for each operation on a code block.

#### Relative Arguments

Pass an argument value for each placeholder in the format string to `CodeBlock.add()`. In each
example, we generate code to say "I ate 3 tacos"

```java
CodeBlock.builder().add("I ate %L %L", 3, "tacos")
```

#### Positional Arguments

Place an integer index (1-based) before the placeholder in the format string to specify which
 argument to use.

```java
CodeBlock.builder().add("I ate %2L %1L", "tacos", 3)
```

#### Named Arguments

Use the syntax `%argumentName:X` where `X` is the format character and call `CodeBlock.addNamed()`
with a map containing all argument keys in the format string. Argument names use characters in
`a-z`, `A-Z`, `0-9`, and `_`, and must start with a lowercase character.

```java
Map<String, Object> map = new LinkedHashMap<>();
map.put("food", "tacos");
map.put("count", 3);
CodeBlock.builder().addNamed("I ate %count:L %food:L", map)
```

### Functions

All of the above functions have a code body. Use `Modifiers.ABSTRACT` to get a function without any
body. This is only legal if the enclosing class is either abstract or an interface.

```java
FunSpec flux = FunSpec.builder("flux")
    .addModifiers(Modifier.ABSTRACT, Modifier.PROTECTED)
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addFun(flux)
    .build();
```

Which generates this:

```java
public abstract class HelloWorld {
  protected abstract void flux();
}
```

The other modifiers work where permitted. Note that when specifying modifiers, KotlinPoet uses
[`javax.lang.model.element.Modifier`][modifier], a class that is not available on Android. This
limitation applies to code-generating-code only; the output code runs everywhere: JVMs, Android,
and GWT.

Functions also have parameters, exceptions, varargs, KDoc, annotations, type variables, and a return
type. All of these are configured with `FunSpec.Builder`.

### Constructors

`FunSpec` is a slight misnomer; it can also be used for constructors:

```java
FunSpec flux = FunSpec.constructorBuilder()
    .addModifiers(Modifier.PUBLIC)
    .addParameter(String.class, "greeting")
    .addStatement("this.%N = %N", "greeting", "greeting")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(String.class, "greeting", Modifier.PRIVATE, Modifier.FINAL)
    .addFun(flux)
    .build();
```

Which generates this:

```java
public class HelloWorld {
  private final String greeting;

  public HelloWorld(String greeting) {
    this.greeting = greeting;
  }
}
```

For the most part, constructors work just like functions. When emitting code, KotlinPoet will place
constructors before functions in the output file.

### Parameters

Declare parameters on functions and constructors with either `ParameterSpec.builder()` or
`FunSpec`'s convenient `addParameter()` API:

```java
ParameterSpec android = ParameterSpec.builder(String.class, "android")
    .addModifiers(Modifier.FINAL)
    .build();

FunSpec welcomeOverlords = FunSpec.builder("welcomeOverlords")
    .addParameter(android)
    .addParameter(String.class, "robot", Modifier.FINAL)
    .build();
```

Though the code above to generate `android` and `robot` parameters is different, the output is the
same:

```java
void welcomeOverlords(final String android, final String robot) {
}
```

The extended `Builder` form is necessary when the parameter has annotations (such as `@Nullable`).

### Fields

Like parameters, fields can be created either with builders or by using convenient helper functions:

```java
FieldSpec android = FieldSpec.builder(String.class, "android")
    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(android)
    .addField(String.class, "robot", Modifier.PRIVATE, Modifier.FINAL)
    .build();
```

Which generates:

```java
public class HelloWorld {
  private final String android;

  private final String robot;
}
```

The extended `Builder` form is necessary when a field has KDoc, annotations, or a field initializer.
Field initializers use the same [`String.format()`][formatter]-like syntax as the code blocks above:

```java
FieldSpec android = FieldSpec.builder(String.class, "android")
    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
    .initializer("%S + %L", "Lollipop v.", 5.0d)
    .build();
```

Which generates:

```java
private final String android = "Lollipop v." + 5.0;
```

### Interfaces

KotlinPoet has no trouble with interfaces. Note that interface functions must always be `PUBLIC
ABSTRACT` and interface fields must always be `PUBLIC STATIC FINAL`. These modifiers are necessary
when defining the interface:

```java
TypeSpec helloWorld = TypeSpec.interfaceBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(FieldSpec.builder(String.class, "ONLY_THING_THAT_IS_CONSTANT")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer("%S", "change")
        .build())
    .addFun(FunSpec.builder("beep")
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .build())
    .build();
```

But these modifiers are omitted when the code is generated. These are the defaults so we don't need
to include them for `javac`'s benefit!

```java
public interface HelloWorld {
  String ONLY_THING_THAT_IS_CONSTANT = "change";

  void beep();
}
```

### Enums

Use `enumBuilder` to create the enum type, and `addEnumConstant()` for each value:

```java
TypeSpec helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("ROCK")
    .addEnumConstant("SCISSORS")
    .addEnumConstant("PAPER")
    .build();
```

To generate this:

```java
public enum Roshambo {
  ROCK,

  SCISSORS,

  PAPER
}
```

Fancy enums are supported, where the enum values override functions or call a superclass
constructor. Here's a comprehensive example:

```java
TypeSpec helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("ROCK", TypeSpec.anonymousClassBuilder("%S", "fist")
        .addFun(FunSpec.builder("toString")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return %S", "avalanche!")
            .build())
        .build())
    .addEnumConstant("SCISSORS", TypeSpec.anonymousClassBuilder("%S", "peace")
        .build())
    .addEnumConstant("PAPER", TypeSpec.anonymousClassBuilder("%S", "flat")
        .build())
    .addField(String.class, "handsign", Modifier.PRIVATE, Modifier.FINAL)
    .addFun(FunSpec.constructorBuilder()
        .addParameter(String.class, "handsign")
        .addStatement("this.%N = %N", "handsign", "handsign")
        .build())
    .build();
```

Which generates this:

```java
public enum Roshambo {
  ROCK("fist") {
    @Override
    public void toString() {
      return "avalanche!";
    }
  },

  SCISSORS("peace"),

  PAPER("flat");

  private final String handsign;

  Roshambo(String handsign) {
    this.handsign = handsign;
  }
}
```

### Anonymous Inner Classes

In the enum code, we used `Types.anonymousInnerClass()`. Anonymous inner classes can also be used in
code blocks. They are values that can be referenced with `%L`:

```java
TypeSpec comparator = TypeSpec.anonymousClassBuilder("")
    .addSuperinterface(ParameterizedTypeName.get(Comparator.class, String.class))
    .addFun(FunSpec.builder("compare")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(String.class, "a")
        .addParameter(String.class, "b")
        .returns(int.class)
        .addStatement("return %N.length() - %N.length()", "a", "b")
        .build())
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addFun(FunSpec.builder("sortByLength")
        .addParameter(ParameterizedTypeName.get(List.class, String.class), "strings")
        .addStatement("%T.sort(%N, %L)", Collections.class, "strings", comparator)
        .build())
    .build();
```

This generates a function that contains a class that contains a function:

```java
void sortByLength(List<String> strings) {
  Collections.sort(strings, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  });
}
```

One particularly tricky part of defining anonymous inner classes is the arguments to the superclass
constructor. In the above code we're passing the empty string for no arguments:
`TypeSpec.anonymousClassBuilder("")`. To pass different parameters use KotlinPoet's code block
syntax with commas to separate arguments.


### Annotations

Simple annotations are easy:

```java
FunSpec toString = FunSpec.builder("toString")
    .addAnnotation(Override.class)
    .returns(String.class)
    .addModifiers(Modifier.PUBLIC)
    .addStatement("return %S", "Hoverboard")
    .build();
```

Which generates this function with an `@Override` annotation:

```java
  @Override
  public String toString() {
    return "Hoverboard";
  }
```

Use `AnnotationSpec.builder()` to set properties on annotations:

```java
FunSpec logRecord = FunSpec.builder("recordEvent")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(Headers.class)
        .addMember("accept", "%S", "application/json; charset=utf-8")
        .addMember("userAgent", "%S", "Square Cash")
        .build())
    .addParameter(LogRecord.class, "logRecord")
    .returns(LogReceipt.class)
    .build();
```

Which generates this annotation with `accept` and `userAgent` properties:

```java
@Headers(
    accept = "application/json; charset=utf-8",
    userAgent = "Square Cash"
)
LogReceipt recordEvent(LogRecord logRecord);
```

When you get fancy, annotation values can be annotations themselves. Use `%L` for embedded
annotations:

```java
FunSpec logRecord = FunSpec.builder("recordEvent")
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addAnnotation(AnnotationSpec.builder(HeaderList.class)
        .addMember("value", "%L", AnnotationSpec.builder(Header.class)
            .addMember("name", "%S", "Accept")
            .addMember("value", "%S", "application/json; charset=utf-8")
            .build())
        .addMember("value", "%L", AnnotationSpec.builder(Header.class)
            .addMember("name", "%S", "User-Agent")
            .addMember("value", "%S", "Square Cash")
            .build())
        .build())
    .addParameter(LogRecord.class, "logRecord")
    .returns(LogReceipt.class)
    .build();
```

Which generates this:

```java
@HeaderList({
    @Header(name = "Accept", value = "application/json; charset=utf-8"),
    @Header(name = "User-Agent", value = "Square Cash")
})
LogReceipt recordEvent(LogRecord logRecord);
```

Note that you can call `addMember()` multiple times with the same property name to populate a list
of values for that property.

### KDoc

Fields, functions, and types can be documented with KDoc:

```java
FunSpec dismiss = FunSpec.builder("dismiss")
    .addKdoc("Hides `message` from the caller's history. Other participants in the\n"
        + "conversation will continue to see the message in their own history\n"
        + " unless they also delete it.\n")
    .addKdoc("\n")
    .addKdoc("Use [Message.delete(%T)] to delete the entire conversation\n"
        + "for all participants.\n", Conversation.class)
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addParameter(Message.class, "message")
    .build();
```

Which generates this:

```java
  /**
   * Hides `message` from the caller's history. Other participants in the
   * conversation will continue to see the message in their own history
   * unless they also delete it.
   *
   * Use [Message.delete(Conversation)] to delete the entire conversation
   * for all participants.
   */
  void dismiss(Message message);
```

Use `%T` when referencing types in Javadoc to get automatic imports.

Download
--------

Download [the latest .jar][dl] or depend via Maven:
```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>kotlinpoet</artifactId>
  <version>0.1.0</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup:kotlinpoet:0.1.0'
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





 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=javapoet&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/com/squareup/javapoet/
 [kdoc]: https://square.github.io/javapoet/1.x/javapoet/
 [formatter]: http://developer.android.com/reference/java/util/Formatter.html
 [modifier]: http://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/Modifier.html

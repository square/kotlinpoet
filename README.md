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
    println("Hello, $name")
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
            .addStatement("println(%S)", "Hello, \$name")
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


Download
--------

Download [the latest .jar][dl] or depend via Maven:

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>kotlinpoet</artifactId>
  <version>0.6.0</version>
</dependency>
```

or Gradle:

```groovy
compile 'com.squareup:kotlinpoet:0.6.0'
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
 [kdoc]: https://square.github.io/kotlinpoet/0.x/kotlinpoet/com.squareup.kotlinpoet/
 [javapoet]: https://github.com/square/javapoet/

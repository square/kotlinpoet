import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation

/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  id("com.github.johnrengelman.shadow") version versions.shadowPlugin
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.classinspector.elements")
  }
}

val shade: Configuration = configurations.maybeCreate("compileShaded")
configurations.getByName("compileOnly").extendsFrom(shade)
dependencies {
  api(project(":kotlinpoet-metadata-specs"))
  // Unshaded stable guava dep for auto-common
  // `api` due to https://youtrack.jetbrains.com/issue/KT-41702
  api(deps.guava)

  // Shade auto-common as it's unstable
  shade(deps.autoCommon) {
    exclude(group = "com.google.guava")
  }
}

val relocateShadowJar = tasks.register<ConfigureShadowRelocation>("relocateShadowJar") {
  target = tasks.shadowJar.get()
}

val shadowJar = tasks.shadowJar.apply {
  configure {
    dependsOn(relocateShadowJar)
    minimize()
    archiveClassifier.set("")
    configurations = listOf(shade)
    relocate(
      "com.google.auto.common",
      "com.squareup.kotlinpoet.classinspector.elements.shaded.com.google.auto.common"
    )
  }
}

artifacts {
  runtime(shadowJar)
  archives(shadowJar)
}

// Shadow plugin doesn't natively support gradle metadata, so we have to tell the maven plugin where
// to get a jar now.
afterEvaluate {
  configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
      if (name == "pluginMaven") {
        // This is to properly wire the shadow jar's gradle metadata and pom information
        setArtifacts(artifacts.matching { it.classifier != "" })
        artifact(shadowJar)
      }
    }
  }
}

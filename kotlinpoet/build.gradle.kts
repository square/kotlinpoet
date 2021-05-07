plugins {
  `java-library`
  `maven-publish`
}

dependencies {
  implementation(libs.reflect)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.compile.testing)
  testImplementation(libs.jimfs)
  testImplementation(libs.ecj)
  testImplementation(libs.kt.compile.testing)
}

java {
  withSourcesJar()
}

publishing {
  repositories {
    maven {
      name = "GithubPackages"
      url = uri("https://maven.pkg.github.com/lg-backbone/hear-ye")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
  publications {
    create<MavenPublication>("hear-ye") {
      from(components["kotlin"])
      artifact(tasks.sourcesJar)
    }
  }
}

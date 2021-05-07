tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.classinspector.elements")
  }
}

dependencies {
  api(project(":interop:kotlinx-metadata:specs"))
  api(libs.guava)
  api(libs.auto.common)
}

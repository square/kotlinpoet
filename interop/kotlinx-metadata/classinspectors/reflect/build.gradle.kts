tasks.named<Jar>("jar") {
  manifest {
    attributes("Automatic-Module-Name" to "com.squareup.kotlinpoet.classinspector.reflective")
  }
}

dependencies {
  api(project(":interop:kotlinx-metadata:specs"))
}

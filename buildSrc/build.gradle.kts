buildscript {
  repositories {
    jcenter()
  }
}

repositories {
  jcenter()
}

plugins {
  `kotlin-dsl`
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

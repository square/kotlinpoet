Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update `docs/changelog.md` for the impending release.
 3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 4. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version).
 5. Update `gradle.properties` to the next SNAPSHOT version.
 6. `git commit -am "Prepare next development version."`.
 7. `git push && git push --tags`.

This will trigger a GitHub Action workflow which will create a GitHub release and upload the
release artifacts to [Maven Central][maven-central].

 [maven-central]: https://repo.maven.apache.org/maven2/com/squareup/kotlinpoet/

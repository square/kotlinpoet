#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material

if [ "$1" = "--local" ]; then local=true; fi

if ! [ $local ]; then
  set -ex

  REPO="git@github.com:square/kotlinpoet.git"
  DIR=temp-clone

  # Delete any existing temporary website clone
  rm -rf $DIR

  # Clone the current repo into temp folder
  git clone $REPO $DIR

  # Move working directory into temp folder
  cd $DIR

  # Generate the API docs
  ./gradlew :kotlinpoet:dokka
fi

# Copy in special files that GitHub wants in the project root.
cat README.md > docs/index.md
cp kotlinpoet-metadata/README.md docs/kotlinpoet_metadata.md
cp kotlinpoet-metadata-specs/README.md docs/kotlinpoet_metadata_specs.md
cp CHANGELOG.md docs/changelog.md
cp CONTRIBUTING.md docs/contributing.md

# Fix *.md links to point to where the docs live under Mkdocs.
# Linux
# sed -i 's/kotlinpoet-metadata-specs\/README.md/\/kotlinpoet_metadata_specs/' docs/changelog.md
# OSX
sed -i "" 's/kotlinpoet-metadata-specs\/README.md/\/kotlinpoet_metadata_specs/' docs/changelog.md

# Build the site and push the new files up to GitHub
if ! [ $local ]; then
  mkdocs gh-deploy
else
  mkdocs serve
fi

# Delete our temp folder
if ! [ $local ]; then
  cd ..
  rm -rf $DIR
fi

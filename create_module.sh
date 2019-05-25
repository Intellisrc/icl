#!/bin/bash
MOD=$1
mkdir -p modules/$MOD/src/main/groovy/com/intellisrc/$MOD/
mkdir -p modules/$MOD/src/test/groovy/com/intellisrc/$MOD/
echo "dependencies {
    compile project(':core')
}" > modules/$MOD/build.gradle
echo "Add in settings.gradle :"
echo "include ':$MOD'"
echo "project(':$MOD').projectDir     = new File(rootProject.projectDir, 'modules/$MOD')"
#!/bin/bash
gradle groovydoc
for modDir in modules/*; do
    mod=$(basename $modDir)
    echo "--- $mod ---";
    rm -rf public/$mod/
    if [[ -d $modDir/docs ]]; then
        rm -rf $modDir/docs;
    fi
    mv $modDir/build/docs/groovydoc/ public/$mod/
done

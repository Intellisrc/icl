#!/bin/bash
gradle groovydoc
for m in modules/*; do
    if [[ -d $m/build/docs/groovydoc ]]; then
        rsync -ia --delete $m/build/docs/groovydoc/ $m/docs/
    fi
done

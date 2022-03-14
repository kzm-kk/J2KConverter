#!/bin/bash

D=/Users/kzm0308/IdeaProjects/J2KConverter/src
if [ -d $D ]; then
    # 存在する場合
    echo "exist"
    array=`find $D -type f -name *.java`
    for a in $array; do
    echo $a
    done
else
    # 存在しない場合
    echo "no exist"
fi
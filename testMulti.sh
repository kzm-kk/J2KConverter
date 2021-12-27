#!/bin/bash

CP=./javaparser-core-3.23.1.jar:./build/classes/java/main
D=/Users/kzm0308/IdeaProjects/J2KConverter/src
if [ -d $D ]; then
    # 存在する場合
    echo "exist"
    java -cp $CP J2KConverterSupporterMulti $D
    array=`find $D -type f -name *.java`
    for a in $array; do
    echo $a
    java -cp $CP J2KConverterMulti $a
    done
else
    # 存在しない場合
    echo "no exist"
fi
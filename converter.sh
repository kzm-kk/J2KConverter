CP=./javaparser-core-3.16.1.jar:./build/classes/java/main
D=/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac2
F=$D/Verf_pac2.java
java -cp $CP J2KConverterFull $F > tmp.kt
kotlinc tmp.kt


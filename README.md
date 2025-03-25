# Class Dump

A tiny Java agent (<3kb) to dump all classes loaded by the JVM (excluding arrays), made using the instrumentation API.

#### As of 1.1, there are no longer two jars, only one!

To use, simply add `-javaagent:path/to/classdump-version.jar` as a JVM arg. (adding `=debug` at the end enables logging each class dumped)

Classes are dumped to `_classdump`, relative to the working directory.

### Building

This project builds like any normal Gradle project, just run `./gradlew build`.

There are also GitHub Actions setup for this repo, allowing you to get a jar for every commit!
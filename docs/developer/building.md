# Building from source

## Prerequisites

- JDK 11 or newer (the CI build uses Temurin 11). The code is compiled for Java 8,
  so the resulting jar also runs on a Java 8 runtime.
- [Apache Maven](https://maven.apache.org/) 3.6 or newer.

## Build

```sh
git clone https://github.com/drhaz/VisualFitsBrowser.git
cd VisualFitsBrowser
mvn package
```

This produces two jars in `target/`:

- `VisualFitsBrowser-<version>.jar` — project classes only;
- `VisualFitsBrowser-<version>-jar-with-dependencies.jar` — the runnable jar
  distributed in releases.

Run it with:

```sh
java -jar target/VisualFitsBrowser-*-jar-with-dependencies.jar
```

or straight from Maven:

```sh
mvn compile exec:java
```

## Tests

```sh
mvn test
```

The test suite is small (JUnit 3). `FITSTextCommentSQLITEImpTest` exercises the
comment database and writes a `temp.db` file into the working directory.

## Libraries not on Maven Central

The jsky libraries (`jsky-util`, `jsky-util-gui`, `jsky-coords`, version 3.0) are
not available from Maven Central. They are stored in a file-based Maven repository
in [`lib/`](https://github.com/drhaz/VisualFitsBrowser/tree/master/lib), which
`pom.xml` declares as `local-maven-repo`. Nothing needs installing by hand.

`lib/jsamp/` is a leftover: jsamp now comes from Maven Central
(`uk.ac.starlink:jsamp`).

## Working in an IDE

Import the project as a Maven project (IntelliJ IDEA: *File → Open* and select
`pom.xml`; Eclipse: *Import → Existing Maven Project*). The main class is
`org.cowjumping.VisualFitsBrowser.VisualFitsBrowserApp`.

Resources (icons, LaTeX templates, log4j configuration, the version file) live in
`src/main/java/resources/` rather than `src/main/resources/`; `pom.xml` points
Maven at that directory and enables filtering so that `${project.version}` is
filled into `resources/properties`.

## Documentation

This documentation lives in `docs/` and is built with
[MkDocs](https://www.mkdocs.org/) and the Material theme. To preview it locally:

```sh
pip install -r docs/requirements.txt
mkdocs serve
```

then open <http://127.0.0.1:8000>. API documentation (Javadoc) is generated with
`mvn javadoc:javadoc` into `target/site/apidocs`.

When you change user-visible behaviour, update the matching page in `docs/` in the
same pull request.

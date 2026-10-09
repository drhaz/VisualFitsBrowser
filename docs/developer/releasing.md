# Releasing

Releases are built and published by GitHub Actions
([`.github/workflows/maven.yml`](https://github.com/drhaz/VisualFitsBrowser/blob/master/.github/workflows/maven.yml))
whenever a tag is pushed.

## Steps

1. Set the new version in `pom.xml` (`<version>1.10.4</version>`). The version is
   shown in the window title and start-up output.
2. Move the entries under **Unreleased** in
   [`CHANGELOG.md`](https://github.com/drhaz/VisualFitsBrowser/blob/master/CHANGELOG.md)
   into a new section headed `## [1.10.4] - YYYY-MM-DD`.
3. Commit, then tag with exactly the version number and push:

    ```sh
    git commit -am "Release 1.10.4"
    git tag 1.10.4
    git push origin master 1.10.4
    ```

4. The workflow builds with JDK 11, creates the GitHub release
   **Release 1.10.4**, uses the matching `CHANGELOG.md` section as the release
   notes, and attaches `VisualFitsBrowser-1.10.4-jar-with-dependencies.jar`.

Use the version from `pom.xml` as the tag so that the jar name and the tag agree.

## Documentation site

The documentation in `docs/` is published to GitHub Pages by
[`.github/workflows/docs.yml`](https://github.com/drhaz/VisualFitsBrowser/blob/master/.github/workflows/docs.yml)
on every push to `master` that touches `docs/`, `mkdocs.yml` or the Java sources.
It does not need a release.

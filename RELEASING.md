# How to Create a Release of OpenTelemetry Operations Java (for Maintainers Only)

## Build Environments  

The system used for build and deploy must be able to run the [mock
server](https://github.com/googleinterns/cloud-operations-api-mock).

## Prerequisites

### Setup OSSRH and Signing

If you haven't deployed artifacts to Maven Central before, you need to set up
your OSSRH (OSS Repository Hosting) account and signing keys.

- Follow the instructions on [this
  page](http://central.sonatype.org/pages/ossrh-guide.html) to set up an account
  with OSSRH.
  - You only need to create the account, not set up a new project
  - Contact an OpenTelemetry Operations Java maintainer to add your account
        after you have created it.
- (For release deployment only) [Install
    GnuPG](http://central.sonatype.org/pages/working-with-pgp-signatures.html#installing-gnupg)
    and [generate your key
    pair](http://central.sonatype.org/pages/working-with-pgp-signatures.html#generating-a-key-pair).
    You'll also need to [publish your public
    key](http://central.sonatype.org/pages/working-with-pgp-signatures.html#distributing-your-public-key)
    to make it visible to the Sonatype servers. For gpg 2.1 or newer, you also
    need to [export the
    keys](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials)
    with command `gpg --keyring secring.gpg --export-secret-keys >
    ~/.gnupg/secring.gpg`.
- Put your GnuPG key password and OSSRH account information in
  `<your-home-directory>/.gradle/gradle.properties`:

    ```text
    # You need the signing properties only if you are making release deployment
    signing.keyId=<8-character-public-key-id>
    signing.password=<key-password>
    signing.secretKeyRingFile=<your-home-directory>/.gnupg/secring.gpg

    ossrhUsername=<ossrh-username>
    ossrhPassword=<ossrh-password>
    checkstyle.ignoreFailures=false
    ```

## Download the mock server

- Run the `get_mock_server.sh` script, which downloads the [mock server
  executable](https://github.com/googleinterns/cloud-operations-api-mock/releases),
  and saves the path.

    ```bash
    $ source ./get_mock_server.sh
    ```

## Tagging the Release

The first step in the release process is to create a release branch, bump
versions, and create a tag for the release. Our release branches follow the
naming convention of `v<major>.<minor>.x`, while the tags include the patch
version `v<major>.<minor>.<patch>`. For example, the same branch `v0.4.x` would
be used to create all `v0.4` tags (e.g. `v0.4.0`, `v0.4.1`).

In this section upstream repository refers to the main
opentelemetry-operations-java github repository.

Before any push to the upstream repository you need to create a [personal access
token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).

1. Create the release branch and push it to GitHub:

    ```bash
    $ MAJOR=0 MINOR=4 PATCH=0 # Set appropriately for new release
    $ git checkout -b v$MAJOR.$MINOR.x master
    $ git push upstream v$MAJOR.$MINOR.x
    ```

2. Prepare `master` for the next development cycle by changing the build file to
        the next minor snapshot (e.g. `0.5.0-SNAPSHOT`).

    _If there are breaking changes in this release, and the example project code
    must be changed before the build would pass, you can skip `./gradlew build`
    for now and fix the example code after you complete the remaining steps in
    this document._

    ```bash
    $ git checkout -b bump-version master
    # Change version to next minor (and keep -SNAPSHOT)
    $ sed -i 's/[0-9]\+\.[0-9]\+\.[0-9]\+\(.*CURRENT_VERSION\)/'$MAJOR.$((MINOR+1)).0'\1/' build.gradle
    # Update the example project dependency to this release version
    $ sed -i 's/[0-9]\+\.[0-9]\+\.[0-9]\+\(.*CURRENT_RELEASE_VERSION\)/'$MAJOR.$MINOR.$PATCH'\1/' build.gradle
    # Run build with the path to the mock server executable
    $ ./gradlew build -Dmock.server.path=$MOCKSERVER
    $ git commit -a -m "Start $MAJOR.$((MINOR+1)).0 development cycle"
    $ git push upstream bump-version
    # Create a pull request to merge into master once this deployment is complete
    ```

3. For `vMajor.Minor.x` branch:

    - Change build file to remove "-SNAPSHOT" for the next release version (e.g.
        `0.4.0`). Commit the result and make a tag:

    ```bash
    $ git checkout -b release v$MAJOR.$MINOR.x
    # Change version to remove -SNAPSHOT
    $ sed -i 's/-SNAPSHOT\(.*CURRENT_VERSION\)/\1/' build.gradle
    $ ./gradlew build -Dmock.server.path=$MOCKSERVER
    $ git commit -a -m "Bump version to $MAJOR.$MINOR.$PATCH"
    $ git tag -a v$MAJOR.$MINOR.$PATCH -m "Version $MAJOR.$MINOR.$PATCH"
    ```

    - Change root build files to the next snapshot version (e.g.
        `0.4.1-SNAPSHOT`). Commit the result:

    ```bash
    # Change version to next patch and add -SNAPSHOT
    $ sed -i 's/[0-9]\+\.[0-9]\+\.[0-9]\+\(.*CURRENT_VERSION\)/'$MAJOR.$MINOR.$((PATCH+1))-SNAPSHOT'\1/' build.gradle
    $ ./gradlew build -Dmock.server.path=$MOCKSERVER
    $ git commit -a -m "Bump version to $MAJOR.$MINOR.$((PATCH+1))-SNAPSHOT"
    ```

    - Go through PR review and push the release tag and updated release branch
        to GitHub (note: do not squash the commits when you merge otherwise you
        will lose the release tag):

    ```bash
    $ git checkout v$MAJOR.$MINOR.x
    $ git merge --ff-only release
    $ git push upstream v$MAJOR.$MINOR.$PATCH
    $ git push upstream v$MAJOR.$MINOR.x
    ```

## Deployment

Deployment to Maven Central (or the snapshot repo) is for all the artifacts from
the project.

### Branch

Before building/deploying, be sure to switch to the appropriate tag. The tag
must reference a commit that has been pushed to the main repository, i.e., has
gone through code review. For the current release use:

```bash
$ git checkout -b v$MAJOR.$MINOR.$PATCH tags/v$MAJOR.$MINOR.$PATCH
```

### Building and Deploying

The following command will build the whole project and upload it to Maven
Central. Parallel building [is not safe during
uploadArchives](https://issues.gradle.org/browse/GRADLE-3420).

```bash
$ ./gradlew clean build  -Dmock.server.path=$MOCKSERVER && ./gradlew -Dorg.gradle.parallel=false uploadArchives
```

If the version has the `-SNAPSHOT` suffix, the artifacts will automatically go
to the snapshot repository, otherwise it's a release deployment, and the
artifacts will go to a staging repository.

When deploying a Release, the deployment will create [a new staging
repository](https://oss.sonatype.org/#stagingRepositories).

## Releasing on Maven Central

Once all the artifacts have been pushed to the staging repository, the
repository must first be `closed`, which will trigger several sanity checks on
the repository. If this completes successfully, the repository can then be
`released`, which will begin the process of pushing the new artifacts to Maven
Central (the staging repository will be destroyed in the process). You can see
the complete process for releasing to Maven Central on the [OSSRH
site](http://central.sonatype.org/pages/releasing-the-deployment.html).

## Announcement

Once deployment finishes, go to Github [release
page](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/releases),
press `Draft a new release` to write release notes about the new release.

You can use `git log upstream/v$MAJOR.$((MINOR-1)).x..upstream/v$MAJOR.$MINOR.x
--graph --first-parent` or the Github [compare
tool](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/compare/)
to view a summary of all commits since last release as a reference.

Please pick major or important user-visible changes only.

## Patch Release

All patch releases should include only bug-fixes, and must avoid
adding/modifying the public APIs. To cherry-pick one commit use the following
command:

```bash
$ COMMIT=1224f0a # Set the right commit hash.
$ git cherry-pick -x $COMMIT
```

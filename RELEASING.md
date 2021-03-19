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

### Using GPG-Agent for artifact signing

If you're running in linux and would like to use the GPG agent to remember your PGP key passwords instead of keeping them in a plain-text file on your home directory,
you can configure the following in `<your-home-directory>/.gradle/gradle.properties`:

    ```text
    signing.gnupg.executable=gpg
    signing.gnupg.keyName=<secret key id (large hash)>
    signing.secretKeyRingFile=<your-home-directory>/.gnupg/pubring.kbx
    ```

Note: these instructions are for modern linux where `gpg` refers to the 2.0 version.

### Ensuring you can push tags to Github upstream

Before any push to the upstream repository you need to create a [personal access
token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).


## Release a Snapshot

If you've followed the above steps, you can release snapshots for consumption using the following:

```bash
$ ./gradlew snapshot
```

## Releasing a Candidate

After following the above steps, you can release candidates from `main` or `v<major>.<minor>.x` branches.

For example, to release the v0.14.0-RC1 candidate, do the following:

```bash
# Create the Candidate.
$ ./gradlew candidate -Prelease.version=0.14.0-RC1
# Push the tag publically.
$ git push origin v0.14.0-RC1
```

Next follow [Releasing on Maven Central](#releasing-on-maven-central) to close + publish the
[repository on OSSRH](https://oss.sonatype.org/#stagingRepositories).


Note:  In the future, the `-Prelease.version` flag should not be required.

## Release a final verison

After following the above steps, you can release candidates from `main` or `v<major>.<minor>.x` branches.

For example, to release the v0.14.0 candidate, do the following:

```bash
# Create the Release.
$ ./gradlew candidate -Prelease.version=0.14.0
# Push the tag publically.
$ git push origin v0.14.0
```

Next follow [Releasing on Maven Central](#releasing-on-maven-central) to close + publish the
[repository on OSSRH](https://oss.sonatype.org/#stagingRepositories).

After this, follow the [Announcment](#Announcement) documentation to advertise the release and update README files.


Note:  In the future, the `-Prelease.version` flag should not be required.

### Branch

Before building/deploying, be sure to switch to the appropriate tag. The tag
must reference a commit that has been pushed to the main repository, i.e., has
gone through code review. For the current release use:

```bash
$ git checkout -b v$MAJOR.$MINOR.$PATCH tags/v$MAJOR.$MINOR.$PATCH
```

## Releasing on Maven Central

Once all the artifacts have been pushed to the staging repository, the
repository must first be `closed`, which will trigger several sanity checks on
the repository. If this completes successfully, the repository can then be
`released`, which will begin the process of pushing the new artifacts to Maven
Central (the staging repository will be destroyed in the process). You can see
the complete process for releasing to Maven Central on the [OSSRH
site](http://central.sonatype.org/pages/releasing-the-deployment.html).

Note: This can/will be automated in the future.

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

# How to Create a Release of OpenTelemetry Operations Java (for Maintainers Only)

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

> [!TIP]
> If your key-generation is failing, checkout the [help section](#help-timeout-during-key-generation-process) at the bottom of this document.

### Using GPG-Agent for artifact signing

> [!NOTE]
> These instructions are for modern linux where `gpg` refers to the 2.0 version.

If you're running in linux and would like to use the GPG agent to remember your PGP key passwords instead of keeping them in a plain-text file on your home directory,
you can configure the following in `<your-home-directory>/.gradle/gradle.properties`:

    ```text
    signing.gnupg.executable=gpg
    signing.gnupg.keyName=<secret key id (large hash)>
    signing.secretKeyRingFile=<your-home-directory>/.gnupg/pubring.kbx
    ```
Note: This may not work so if after adding this, the `./gradlew candidate` task fails citing 401 errors, try adding back the `ossrhUsername` & `ossrhPassword` fields back.

> [!IMPORTANT]
> Starting June 2024, due to a change to the OSSRH authentication backend, the maven publish plugin now requires [a user token](https://central.sonatype.org/publish/generate-token/) instead of a typical username and password used in the Nexus UI.
> Follow the steps in the [link](https://central.sonatype.org/publish/generate-token/) to generate a user token, if not done already - this will provide you with a `tokenuser` and `tokenkey`. Replace the `ossrhUsername` and `ossrhPassword` with this `tokenuser` and `tokenkey` in your `gradle.properties` file to successfully publish artifacts.

### Ensuring you can push tags to GitHub upstream

Before any push to the upstream repository you need to create a [personal access
token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).


## Release a Snapshot

If you've followed the above steps, you can release snapshots for consumption using the following:

```bash
$ ./gradlew snapshot
```

## Releasing a Candidate (Optional)

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

*Note: If you do not have a CredentialsProvider registered for GitHub, the `candidate` task may fail to upload tags to the GitHub repository and the overall command may take a long time to report completion on the task. In this case, before moving forward - check if tags were pushed to GitHub. If not, manually push the tags before continuing.*\
*Next, check if the staging repository is created on the [nexus repository manager](https://oss.sonatype.org/#stagingRepositories). If the repository is already created, continue with the next steps.*

Follow [Releasing on Maven Central](#releasing-on-maven-central) to close + publish the
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

### Things to check before 'closing' on Maven Central

Before closing the staging repository, it is important to verify that the contents of all the
published modules are looking good. Particularly, the version numbers should be what are expected,
and they include any custom release qualifiers (like 'alpha') which are set. Make sure that:
 - The generated POM files for the individual module have the correct version number.
 - The dependencies for an individual module in the POM file are the expected ones & they dependencies have the correct versions.
 - The module content includes all the artifacts that are expected to be published - for instance, sourcesJar, javadocs, additional variants like a shaded JAR in some cases, etc.
 - The file sizes for the published artifacts should seem reasonable.

## Announcement

Once deployment finishes, go to GitHub [release
page](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/releases),
press `Draft a new release` to write release notes about the new release.

You can use `git log upstream/v$MAJOR.$((MINOR-1)).x..upstream/v$MAJOR.$MINOR.x
--graph --first-parent` or the GitHub [compare
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

### Help: Timeout during key-generation process
If you see timeout errors when you run `gpg --gen-key` to generate your keys, it maybe because you are running the command on a server and do not have access to a UI. 
A common example is - running this command on a remote machine over ssh. 

The issue here is that this command opens up a UI dialog asking for you to set a passphrase, waiting for input for a fixed time.

The easiest way to fix this is to run it on a machine for which you have UI access.

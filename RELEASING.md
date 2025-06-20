# How to Create a Release of OpenTelemetry Operations Java (for Maintainers Only)

## Prerequisites

### Setup Maven Central Portal Publishing

> [!IMPORTANT]
> The OSSRH service will reach the end-of-life sunset date on June 30th, 2025.
> After this, it is recommended to only use Sonatype's Central Publisher Portal
> to publish artifacts to Maven Central. See
> [the official notice](https://central.sonatype.org/news/20250326_ossrh_sunset/)
> for more details.

If you do not have a Central Portal account on Sonatype, you need to set up your
account to publish via the Central Portal.

- Follow the instructions on [this
  page](https://central.sonatype.org/register/central-portal/) to set up an
  account with Central Portal.
    - You only need to create the account, not set up a new project.
    - Contact an OpenTelemetry Operations Java maintainer to add your account
      after you have created it.

## Setup artifact signing

### Generate the GPG key
The artifacts must be signed before being published for consumption. Follow
these steps to set up artifact signing:
- [Install
  GnuPG](http://central.sonatype.org/pages/working-with-pgp-signatures.html#installing-gnupg)
  and [generate your key
  pair](http://central.sonatype.org/pages/working-with-pgp-signatures.html#generating-a-key-pair).
- You'll also need to [publish your public
  key](http://central.sonatype.org/pages/working-with-pgp-signatures.html#distributing-your-public-key)
  to make it visible to the Sonatype servers.

### Configuring Gradle to use GPG

> [!NOTE]
> These instructions are for modern linux where `gpg` refers to the 2.0 version.

You can configure Gradle to use GPG by adding the following in
`<your-home-directory>/.gradle/gradle.properties`:

    ```text
    centralPortalUsername=<generated-token-user>
    centralPortalPassword=<generated-token-key>

    signingUseGpgCmd=true
    signing.gnupg.executable=gpg
    signing.gnupg.keyName=<secret key id (large hash)>

    checkstyle.ignoreFailures=false
    ```

Note: You can retrieve the list of previously created GPG keys on your machine
by using `gpg --list-secret-keys`. Additionally, you can use a GPG Agent and/or
a password manager (or the built-in Keyring) to avoid entering the password
manually.\
For more details, checkout the
[help section](#help-timeout-with-gpg-operations) on the bottom
of this guide.

> [!IMPORTANT]
> The user tokens for publishing to the Central Portal are different from those
> used for OSSRH. If you haven't already, you must  generate a new Portal Token 
> to publish to the Central Portal.
> Follow the steps in this
> [link](https://central.sonatype.org/publish/generate-portal-token/) to
> generate a user token - this will provide you with a Portal token containing a
> `username` and `password`. Replace `<generated-token-user>` and
> `<generated-token-key>` with the generated `username` and `password` in your
> `gradle.properties` file to successfully publish artifacts.

### Ensuring you can push tags to GitHub upstream

Before any push to the upstream repository you need to create a [personal access
token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/).


## Release a Snapshot

If you've followed the above steps, you can release snapshots for consumption
using the following:

```bash
$ ./gradlew snapshot
```

SNAPSHOT releases are intended for developers to make pre-release versions of
their projects available for testing. Published snapshots should be visible
using the
[directory listing for com.google.cloud.opentelemetry](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/google/cloud/opentelemetry/)
namespace.

See
[Publishing Snapshot Releases](https://central.sonatype.org/publish/publish-portal-snapshots/#publishing-snapshot-releases)
for more details.

## Preparing a release candidate (Optional)

> [!TIP]
> Preparing a release candidate involves the same steps as preparing a final
> version. The only difference is in how a release candidate is tagged.\
> Release candidates are pre-release version of libraries, close to the final
> stable release, this is typically not required for this repository.

After following the above steps, you can release candidates from `main` or
`v<major>.<minor>.x` branches.

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

## Preparing a final verison

> [!IMPORTANT]
> The nebula-release plugin automatically tags the current release with the
> appropriate number based on the previous release. Make sure that the release
> version being provided in the argument for the candidate task matches the
> latest tag.

After following the above steps, you can release candidates from `main` or
`v<major>.<minor>.x` branches.

For example, to release the v0.14.0 candidate, do the following:

```bash
# Create the Release.
$ ./gradlew candidate -Prelease.version=0.14.0
# Push the tag publically.
$ git push origin v0.14.0
```

*Note: If you do not have a CredentialsProvider registered for GitHub, the 
`candidate` task may fail to upload tags to the GitHub repository and the
overall command may take a long time to report completion on the task.
In this case, before moving forward - check if tags were pushed to GitHub.
If not, manually push the tags before continuing.*\

Note:  In the future, the `-Prelease.version` flag should not be required.

### Branch

Before building/deploying, be sure to switch to the appropriate tag. The tag
must reference a commit that has been pushed to the main repository, i.e., has
gone through code review. For the current release use:

```bash
$ git checkout -b v$MAJOR.$MINOR.$PATCH tags/v$MAJOR.$MINOR.$PATCH
```

## Uploading the release artifacts to Central Portal

> [!IMPORTANT]
> This task will create a deployment on the Central Portal, visible on their UI.
> It should only be run after the release is prepared.

After preparing the release, the release artifacts need to be uploaded to
Central Portal so that they can be released on Maven Central.\
To upload the prepared release artifacts, run the following gradle task: 

```shell
./gradlew sonatypeUploadDefaultRepository
```

The task will respond with an HTTP status code. If the status code is 200, the
artifacts are successfully uploaded on the Central Portal and should be visible
on the UI.

## Releasing on Maven Central

Once all the artifacts have been pushed to the Central Portal, a `deployment`
will be created in the Central Portal. This deployment is visible under the
"Deployments" tab on https://central.sonatype.com/publishing (you will have to
log in with your account).\
At this point, you can either manually 'Drop' or 'Publish' the deployment.
 - Publishing the deployment will make the new release available on Maven
   Central.
 - Dropping the deployment will close the deployment and abandon the release.
   You should drop the deployment if you do not wish to proceed with release
   process for any reason.

### Things to check before 'Publishing' on Maven Central

Before publishing the release, it is important to verify that the
contents of all the published modules are looking good. Particularly, the
version numbers should be what are expected, and they include any custom release
qualifiers (like 'alpha') which are set. Make sure that:
 - The generated POM files for the individual module have the correct version 
   number.
 - The dependencies for an individual module in the POM file are the expected
   ones & the dependencies have the correct versions.
 - The module content includes all the artifacts that are expected to be
   published - for instance, sourcesJar, javadocs, additional variants like a
   shaded JAR in some cases, etc.
 - The file sizes for the published artifacts should seem reasonable.

## Announcement

Once deployment finishes, go to GitHub
[release page](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/releases),
press `Draft a new release` to write release notes about the new release.

You can use `git log upstream/v$MAJOR.$((MINOR-1)).x..upstream/v$MAJOR.$MINOR.x
--graph --first-parent` or the GitHub
[compare tool](https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/compare/)
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

### Help: Timeout with gpg operations
If you see a timeout error when running `gpg` commands, then you probably have a
graphical session with a gpg agent that is prompting you for a password. Check
your graphical sessions for a password prompt.

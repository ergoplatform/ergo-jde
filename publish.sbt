ThisBuild / organization := "io.github.ergoplatform"
ThisBuild / organizationName := "ergoplatform"
ThisBuild / organizationHomepage := Some(url("https://www.ergoplatform.org"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ergoplatform/ergo-jde"),
    "scm:git@github.ergoplatform/ergo-jde.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "scalahub",
    name = "scalahub",
    email = "scalahub@gmail.com",
    url = url("https://www.ergoplatform.org")
  )
)

ThisBuild / description := "Library for interfacing with Ergo dApps"
ThisBuild / licenses := List("The Unlicense" -> new URL("https://unlicense.org/"))
ThisBuild / homepage := Some(url("https://github.com/ergoplatform/ergo-jde"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / publishMavenStyle := true

ThisBuild / versionScheme := Some("early-semver")

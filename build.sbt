ThisBuild / scalaVersion := "2.12.10"

ThisBuild / version := "1.1"

ThisBuild / scalacOptions ++= Seq(
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-feature",
  "-unchecked"
)

lazy val commonResolvers = resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

lazy val commonMergeStrategy = assembly / assemblyMergeStrategy := {
  case PathList("reference.conf")           => MergeStrategy.concat
  case PathList("META-INF", xs @ _*)        => MergeStrategy.discard
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x                                    => MergeStrategy.first
}

updateOptions := updateOptions.value.withLatestSnapshots(false)

lazy val commonDependencies = libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.+",
  "org.ergoplatform" %% "ergo-appkit" % "develop-60fd166d-SNAPSHOT",
  "com.squareup.okhttp3" % "mockwebserver" % "3.14.9" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.+" % Test,
  "org.mockito" % "mockito-core" % "2.23.4" % Test
)

lazy val Kiosk = Project("kiosk", file("kiosk")).settings(
  commonResolvers,
  commonDependencies
)

lazy val JDE = Project("jde", file("jde"))
  .dependsOn(Kiosk)
  .settings(
    commonResolvers,
    commonDependencies,
    commonMergeStrategy,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.9.2"
    )
  )

lazy val myMainClass = "cli.Compile"
lazy val myJarName = "jde.jar"

lazy val root = Project("ErgoJDE", file("."))
  .aggregate(JDE, Kiosk)
  .dependsOn(JDE)
  .settings(
    commonResolvers,
    commonDependencies,
    libraryDependencies += "javax.servlet" % "servlet-api" % "2.5" % "provided", // for servlet
    Compile / mainClass := Some(myMainClass),
    assembly / mainClass := Some(myMainClass),
    assembly / assemblyJarName := myJarName,
    commonMergeStrategy
  )

enablePlugins(JettyPlugin)

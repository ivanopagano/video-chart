// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `intro-task` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaHttp,
        library.akkaSprayJson,
        library.akkaStream,
        library.cats,
        library.akkaHttpTestkit % Test,
        library.scalaTest % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************


lazy val library =
  new {
    object Version {
      val akka       = "2.5.13"
      val akkaHttp   = "10.1.3"
      val scalaCheck = "1.14.0"
      val scalaTest  = "3.0.5"
      val cats       = "1.1.0"
    }
    val akkaHttp        = "com.typesafe.akka" %% "akka-http"            % Version.akkaHttp
    val akkaStream      = "com.typesafe.akka" %% "akka-stream"          % Version.akka
    val akkaSprayJson   = "com.typesafe.akka" %% "akka-http-spray-json" % Version.akkaHttp
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit"    % Version.akkaHttp
    val cats            = "org.typelevel"     %% "cats-core"            % Version.cats
    val scalaTest       = "org.scalatest"     %% "scalatest"            % Version.scalaTest
    val scalaCheck      = "org.scalacheck"    %% "scalacheck"           % Version.scalaCheck
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.6",
    organization := "scalac.io",
    organizationName := "Ivano Pagano",
    startYear := Some(2018),
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )

ThisBuild / licenses += "ISC"  -> url("https://opensource.org/licenses/ISC")
ThisBuild / versionScheme      := Some("semver-spec")
ThisBuild / evictionErrorLevel := Level.Warn

publish / skip := true

lazy val pg = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
//  .enablePlugins(ScalablyTypedConverterPlugin)
  .settings(
    name         := "pg",
    version      := "0.0.1",
    scalaVersion := "3.6.3",
    organization := "io.github.edadma",
//    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
    libraryDependencies ++= Seq(
      "org.scalatest"    %%% "scalatest"                   % "3.2.19" % "test",
      "io.github.edadma" %%% "logger"                      % "0.0.6",
      "dev.zio"          %%% "zio-json"                    % "0.7.3",
      "org.scala-js"     %%% "scala-js-macrotask-executor" % "1.1.1",
    ),
//    libraryDependencies += "com.lihaoyi" %%% "pprint" % "0.9.0" % "test",
    jsEnv                                  := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
    Test / scalaJSUseMainModuleInitializer := true,
    Test / scalaJSUseTestModuleInitializer := false,
//    Test / scalaJSUseMainModuleInitializer := false,
//    Test / scalaJSUseTestModuleInitializer := true,
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    publishMavenStyle      := true,
    Test / publishArtifact := false,
    licenses += "ISC"      -> url("https://opensource.org/licenses/ISC"),
  )

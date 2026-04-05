ThisBuild / scalaVersion := "3.8.3"
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / description := "SQL Optimiser"
ThisBuild / organization := "zgharbi.ge"
ThisBuild / developers := List(
  Developer(
    id = "mtavkhelidze",
    name = "Misha Tavkhelidze",
    email = "misha.tavkhelidze@gmail.com",
    url = url("https://github.com/mtavkhelidze"),
  ),
)

Global / excludeLintKeys += idePackagePrefix
Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / scalacOptions ++= Seq("-Wconf:src=src_managed/.*:s")

lazy val commonDeps = Seq(
  "co.fs2" %% "fs2-core" % "3.13.0",
  "co.fs2" %% "fs2-io" % "3.13.0",
  "com.lihaoyi" %% "upickle" % "4.4.3",
  "org.typelevel" %% "cats-effect" % "3.7.0",
)
lazy val testDeps = Nil

lazy val deps = commonDeps ++ testDeps

lazy val root = (project in file("."))
  .settings(
    fork := true,
    idePackagePrefix := Some("kaze"),
    libraryDependencies ++= deps,
    name := "kaze",
    javacOptions := Seq("-Xlint:-options"),
    javaOptions := Seq(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--enable-native-access=ALL-UNNAMED",
      "--sun-misc-unsafe-memory-access=allow",
    ),
    libraryDependencies ++= deps,
  )

val cmake = taskKey[Unit]("Build libkaze")
cmake := {
  import scala.sys.process.*
  val cppDir = baseDirectory.value / "cpp"
  val buildDir = cppDir / "build"
  if (!buildDir.exists()) buildDir.mkdir()

  Process(Seq("cmake", ".."), buildDir).!
  Process(Seq("make"), buildDir).!
}

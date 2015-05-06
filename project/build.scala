//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

import sbt._, Keys._

import com.twitter.scrooge.ScroogeSBT._

import sbtdoge.CrossPerProjectPlugin.{projectSettings => dogeProjectSettings}

import au.com.cba.omnia.uniform.core.scala.Scala
import au.com.cba.omnia.uniform.core.standard.StandardProjectPlugin._
import au.com.cba.omnia.uniform.core.version.UniqueVersionPlugin._
import au.com.cba.omnia.uniform.dependency.UniformDependencyPlugin._
import au.com.cba.omnia.uniform.thrift.UniformThriftPlugin._
import au.com.cba.omnia.uniform.assembly.UniformAssemblyPlugin._

/**
  * This build publishes a Scala 2.10 version of core, generator and plugin since
  * SBT plugins are based on Scala 2.10.
  * It also publishes a Scala 2.11 version of core since that is what the other projects depend on.
  * It uses sbt-doge to help do proper cross compilation, etc. since native SBT support for this is
  * poor.
  */
object build extends Build {
  val compileThrift = TaskKey[Seq[File]](
    "compile-thrift", "generate thrift needed for tests")

  val humbugThriftSourceFolder = SettingKey[File](
    "humbug-thrift-source-folder",
    "directory containing thrift source files"
  )

  lazy val standardSettings =
    Defaults.coreDefaultSettings ++
    uniformDependencySettings ++
    strictDependencySettings ++
    uniform.docSettings("https://github.com/CommBank/humbug") ++
    Seq(
      updateOptions      := updateOptions.value.withCachedResolution(true)
    )

  lazy val scala210Settings = Seq(
    scalaVersion       := "2.10.5",
    crossScalaVersions := Seq(scalaVersion.value),
    scalacOptions      := scalacOptions.value.filter(o =>
      !(scalaBinaryVersion.value == "2.10" && o == "-Ywarn-unused-import")
    )
  )

  lazy val humbugSettings = Seq(
    humbugThriftSourceFolder := baseDirectory.value
  )

  lazy val all = Project(
    id = "all",
    base = file("."),
    settings =
      standardSettings ++
      uniform.project("humbug-all", "au.com.cba.omnia.humbug.all") ++
      scala210Settings ++
      uniform.ghsettings ++
      dogeProjectSettings ++
      Seq(publishArtifact := false)
  ).aggregate(core, generator, plugin, test)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings =
      standardSettings ++
      uniform.project("humbug-core", "au.com.cba.omnia.humbug") ++
      scala210Settings ++
      uniform.ghsettings ++
      Seq(
        // Also publish a 2.11 version since our other projects depend on this library.
        crossScalaVersions := Seq(scalaVersion.value, Scala.version),
        libraryDependencies ++= depend.scrooge() ++ Seq(
          "org.apache.thrift" % "libthrift" % depend.versions.libthrift % "provided" // required for scaladoc
        )
      )
  )

/* FAILS for 2.11
  lazy val test = Project(
    id = "test",
    base = file("test"),
    settings =
      standardSettings ++
      uniform.project("humbug-test", "au.com.cba.omnia.humbug.test") ++
      uniform.ghsettings ++
      uniformThriftSettings ++
      inConfig(Test)(thriftSettings) ++
      Seq(
        libraryDependencies ++= depend.scrooge() ++ Seq(
          "org.apache.thrift" % "libthrift" % depend.versions.libthrift % "provided" // required for scaladoc
        , "org.scala-lang"   % "scala-compiler"   % Scala.version
        , "org.scala-lang"   % "scala-reflect"    % Scala.version
        , "org.scalacheck"  %% "scalacheck"       % depend.versions.scalacheck
        , "com.twitter"     %% "util-eval"        % "6.24.0"                   % "test"
        , "org.specs2"      %% "specs2"           % "3.3.1"                    % "test"
        ),
        scroogeThriftSourceFolder in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "scrooge" },
        humbugThriftSourceFolder in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "humbug" }
       //, addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
      )
  ).dependsOn(core)
  */

/* WORKS FOR 2.10 */
  lazy val test = Project(
    id = "test",
    base = file("test"),
    settings =
      standardSettings ++
      uniform.project("humbug-test", "au.com.cba.omnia.humbug.test") ++
      uniform.ghsettings ++
      uniformThriftSettings ++
      scala210Settings ++
      //humbugSettings ++
      inConfig(Test)(thriftSettings) ++
      Seq(
        libraryDependencies ++= depend.scrooge() ++ Seq(
          "org.apache.thrift" % "libthrift" % depend.versions.libthrift % "provided" // required for scaladoc
        , "org.scala-lang"   % "scala-compiler"   % Scala.version
        , "org.scala-lang"   % "scala-reflect"    % Scala.version
        , "org.scalacheck"  %% "scalacheck"       % depend.versions.scalacheck
        , "com.twitter"     %% "util-eval"        % "6.24.0"                   % "test"
        , "org.scalamacros" %% "quasiquotes"      % "2.0.0"
        //, "org.specs2"     %% "specs2-core"               % depend.versions.specs      % "test"
        //    exclude("org.ow2.asm", "asm")
        , "org.specs2"     %% "specs2-matcher-extra" % depend.versions.specs      % "test"
        //, "org.specs2"      %% "specs2"           % "3.3.1"                    //% "test"
        //, "org.specs2"      %% "specs2"           % depend.versions.specs      % "test"
        ),
        scroogeThriftSourceFolder in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "scrooge" },
        humbugThriftSourceFolder in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "humbug" },
        addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
      )
  ).dependsOn(core, generator)

  lazy val generator = Project(
    id = "generator",
    base = file("generator"),
    settings =
      standardSettings ++
      uniform.project("humbug-generator", "au.com.cba.omnia.humbug.generator") ++
      scala210Settings ++
      strictDependencySettings ++
      uniformAssemblySettings ++
      humbugSettings ++
      inConfig(Test)(thriftSettings) ++
      Seq(
        libraryDependencies ++= depend.hadoopClasspath ++ depend.scalaz() ++ Seq(
          "com.twitter"    %% "scrooge-generator" % depend.versions.scrooge,
          "org.specs2"     %% "specs2-core"               % depend.versions.specs      % "test"
            exclude("org.ow2.asm", "asm"),
          "org.specs2"     %% "specs2-scalacheck"         % depend.versions.specs      % "test" 
            exclude("org.ow2.asm", "asm")
            exclude("org.scalacheck", s"scalacheck_${scalaBinaryVersion.value}"),
          "org.scalacheck" %% "scalacheck"                % depend.versions.scalacheck % "test" 
            exclude("org.scala-lang.modules", s"scala-parser-combinators_${scalaBinaryVersion.value}"),
          "org.scalaz"     %% "scalaz-scalacheck-binding" % depend.versions.scalaz     % "test"
            exclude("org.scalacheck", s"scalacheck_${scalaBinaryVersion.value}"),
          "asm"             %  "asm"                      % depend.versions.asm        % "test",
          "com.twitter"    %% "bijection-scrooge"         % depend.versions.bijection  % "test"
            // exclude clashes with scrooge-generator
            exclude("com.twitter", s"scrooge-core_${scalaBinaryVersion.value}")
            exclude("com.twitter", s"util-core_${scalaBinaryVersion.value}")
            exclude("com.twitter", s"util-codec_${scalaBinaryVersion.value}")//,
        ).map(noHadoop(_))
      )
  ).dependsOn(core)

  lazy val plugin = Project(
    id = "plugin",
    base = file("plugin"),
    settings =
      standardSettings ++
      uniform.project("humbug-plugin", "au.com.cba.omnia.humbug.plugin") ++
      scala210Settings
  ).settings(sbtPlugin := true)
    .dependsOn(generator)

  val thriftSettings = Seq(
    compileThrift <<= (
      streams,
      //humbugThriftSourceFolder,
      baseDirectory,
      fullClasspath in Runtime,
      sourceManaged
    ) map { (out, base, cp, outputDir) =>
      val files = (s"find ${base.getAbsolutePath} -name *.thrift" !!).split("\n")
      //val files = (s"find ${base.getAbsolutePath}/src/test/thrift/humbug -name *.thrift" !!).split("\n")
      val cmd = s"java -cp ${cp.files.absString} au.com.cba.omnia.humbug.Main ${outputDir.getAbsolutePath} ${files.mkString(" ")}"
      out.log.info(cmd)
      cmd ! out.log

      (outputDir ** "*.scala").get.toSeq
    },
    sourceGenerators <+= compileThrift
  )
}

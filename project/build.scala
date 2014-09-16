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

import au.com.cba.omnia.uniform.core.standard.StandardProjectPlugin._
import au.com.cba.omnia.uniform.core.version.UniqueVersionPlugin._
import au.com.cba.omnia.uniform.dependency.UniformDependencyPlugin._
import au.com.cba.omnia.uniform.assembly.UniformAssemblyPlugin._

object build extends Build {
  val compileThrift = TaskKey[Seq[File]](
    "compile-thrift", "generate thrift needed for tests")

  lazy val standardSettings =
    Defaults.coreDefaultSettings ++
    uniformDependencySettings ++
    uniform.docSettings("https://github.com/CommBank/humbug") ++
    Seq(updateOptions := updateOptions.value.withCachedResolution(true))

  lazy val all = Project(
    id = "all",
    base = file("."),
    settings =
      standardSettings ++
      uniform.project("humbug-all", "au.com.cba.omnia.humbug.all") ++
      uniform.ghsettings ++
      Seq(
        publishArtifact := false
      ),
    aggregate = Seq(core, generator, plugin)
    )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings =
      standardSettings ++
      uniform.project("humbug-core", "au.com.cba.omnia.humbug") ++
      uniform.ghsettings ++
      Seq(
        libraryDependencies ++=
          Seq(
            "com.twitter"      %% "scrooge-core"        % depend.versions.scrooge,
            "org.apache.thrift" % "libthrift"           % "0.8.0" % "provided"
          )
      )
  )

  lazy val generator = Project(
    id = "generator",
    base = file("generator"),
    settings =
      standardSettings ++
      uniform.project("humbug-generator", "au.com.cba.omnia.humbug.generator") ++
      uniformAssemblySettings ++
      inConfig(Test)(thriftSettings) ++
      Seq(
        libraryDependencies ++= depend.scalaz() ++ depend.testing() ++
          Seq(
            "com.twitter"      %% "scrooge-generator"  % depend.versions.scrooge,
            "com.twitter"      %% "bijection-scrooge"  % depend.versions.bijection      % "test",
            "au.com.cba.omnia" %% "omnia-test"         % "2.2.0-20150316051318-26deac5" % "test"
          )
      )
  ).dependsOn(core)

  lazy val plugin = Project(
    id = "plugin",
    base = file("plugin"),
    settings =
      standardSettings ++
      uniform.project("humbug-plugin", "au.com.cba.omnia.humbug.plugin")
  ).settings(
    sbtPlugin := true
  ).dependsOn(generator)

  val thriftSettings = Seq(
    compileThrift <<= (
      streams,
      baseDirectory,
      fullClasspath in Runtime,
      sourceManaged
    ) map { (out, base, cp, outputDir) =>
      val files = (s"find ${base.getAbsolutePath} -name *.thrift" !!).split("\n")
      val cmd = s"java -cp ${cp.files.absString} au.com.cba.omnia.humbug.Main ${outputDir.getAbsolutePath} ${files.mkString(" ")}"
      out.log.info(cmd)
      cmd ! out.log

      (outputDir ** "*.scala").get.toSeq
    },
    sourceGenerators <+= compileThrift
  )
}

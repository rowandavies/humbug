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

package au.com.cba.omnia.humbug

import sbt._
import Keys._

import java.io.File
import scala.collection.JavaConverters._

/**
  * This is pretty much a copy of the twitter scrooge sbt plugin https://github.com/twitter/scrooge
  * modified for Humbug.
  */
object HumbugSBT extends Plugin {
  def compile(
    log: Logger,
    outputDir: File,
    thriftFiles: Set[File],
    validators: Seq[ThriftValidator]
  ): Set[File] =
    Main.validateAndGenerate(outputDir.getAbsolutePath, thriftFiles.map(_.getAbsolutePath).toList, validators)

  def filter(dependencies: Classpath, whitelist: Set[String]): Classpath = {
    dependencies.filter { dep =>
      val module = dep.get(AttributeKey[ModuleID]("module-id"))
      module.map { m =>
        whitelist.contains(m.name)
      }.getOrElse(false)
    }
  }

  val humbugThriftDependencies = SettingKey[Seq[String]](
    "humbug-thrift-dependencies",
    "artifacts to extract and compile thrift files from"
  )

  val humbugThriftSourceFolder = SettingKey[File](
    "humbug-thrift-source-folder",
    "directory containing thrift source files"
  )

  val humbugThriftExternalSourceFolder = SettingKey[File](
    "humbug-thrift-external-source-folder",
    "directory containing external source files to compile"
  )

  val humbugThriftSources = TaskKey[Set[File]](
    "humbug-thrift-sources",
    "complete list of thrift source files to compile"
  )

  val humbugThriftOutputFolder = SettingKey[File](
    "humbug-thrift-output-folder",
    "output folder for generated scala files (defaults to sourceManaged)"
  )

  val humbugUnpackDeps = TaskKey[Seq[File]](
    "humbug-unpack-deps",
    "unpack thrift files from dependencies, generating a sequence of source directories"
  )

  val humbugGen = TaskKey[Seq[File]](
    "humbug-gen",
    "generate scala code from thrift files using humbug"
  )

  val humbugThriftValidators = SettingKey[Seq[ThriftValidator]](
    "humbug-thrift-validators",
    "list of validators to apply on parsed com.twitter.scrooge.ast.Document"
  )

  // Dependencies included in the `thrift` configuration will be used
  // in both compile and test.
  val thriftConfig = config("thrift")

  /**
   * these settings will go into both the compile and test configurations.
   * you can add them to other configurations by using inConfig(<config>)(genThriftSettings),
   * e.g. inConfig(Assembly)(genThriftSettings)
   */
  val genThriftSettings: Seq[Setting[_]] = Seq(
    humbugThriftSourceFolder <<= (sourceDirectory) { _ / "thrift" },
    humbugThriftExternalSourceFolder <<= (target) { _ / "thrift_external" },
    humbugThriftOutputFolder <<= (sourceManaged) { identity },
    humbugThriftDependencies := Seq(),
    humbugThriftValidators := Seq(),

    // complete list of source files
    humbugThriftSources <<= (
      humbugThriftSourceFolder,
      humbugUnpackDeps
    ) map { (srcDir, extDirs) =>
      (Set(srcDir) ++ extDirs).flatMap { dir => (dir ** "*.thrift").get }
    },

    // unpack thrift files from all dependencies in the `thrift` configuration
    //
    // returns Seq[File] - directories that include thrift files
    humbugUnpackDeps <<= (
      streams,
      configuration,
      classpathTypes,
      update,
      humbugThriftDependencies,
      humbugThriftExternalSourceFolder
    ) map { (out, configuration, cpTypes, update, deps, extDir) =>
      IO.createDirectory(extDir)
      val whitelist = deps.toSet
      val dependencies =
        Classpaths.managedJars(thriftConfig, cpTypes, update) ++
        filter(Classpaths.managedJars(configuration, cpTypes, update), whitelist)

      val paths = dependencies.map { dep =>
        val module = dep.get(AttributeKey[ModuleID]("module-id"))
        module.flatMap { m =>
          val dest = new File(extDir, m.name)
          IO.createDirectory(dest)
          val thriftFiles = IO.unzip(dep.data, dest, "*.thrift").toSeq
          if (thriftFiles.isEmpty) {
            None
          } else {
            Some(dest)
          }
        }
      }
      paths.filter(_.isDefined).map(_.get)
    },

    // actually run humbug
    humbugGen <<= (
      streams,
      humbugThriftSources,
      humbugThriftOutputFolder,
      humbugThriftValidators
    ) map { (out, sources, outputDir, validators) =>
      val cachingGen =
        FileFunction.cached(outputDir, inStyle = FilesInfo.hash, outStyle = FilesInfo.exists)(
          files => compile(out.log, outputDir, sources.toSet, validators)
        )

      cachingGen(sources).toList
    },
    sourceGenerators <+= humbugGen
  )

  val humbugSettings =
    Seq(ivyConfigurations += thriftConfig) ++
    inConfig(Test)(genThriftSettings) ++
    inConfig(Compile)(genThriftSettings)
}



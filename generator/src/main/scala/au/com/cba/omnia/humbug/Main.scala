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

import java.io.{File, FileWriter}

import com.twitter.scrooge.frontend._
import com.twitter.scrooge.ast._

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Main dst_path thrift_file1 thrift_file2 ...")
      sys.exit(1)
    }

    generate(args(0), args.toList.tail)
  }

  def generate(dst: String, thriftFiles: List[String]): Unit = {
    thriftFiles.foreach { f =>
      val doc = parseFile(f)
      saveThriftAsScala(new File(dst), doc)
    }
  }

  def validateAndGenerate(dst: String, thriftFiles: List[String], validators: Seq[ThriftValidator]): Set[File] = {
    val docs = thriftFiles.map(parseFile)
    val validationErrorsPerFile = thriftFiles.zip(docs).flatMap {
      case (path, doc) => {
        val errors = validate(doc, validators.toList)
        if (errors.isEmpty) List.empty
        else List((path, errors))
      }
    }

    if (validationErrorsPerFile.isEmpty) {
      docs.flatMap(saveThriftAsScala(new File(dst), _)).toSet
    } else {
      throwValidationException(validationErrorsPerFile)
    }
  }

  protected def validate(doc: Document, validators: List[ThriftValidator]): List[ThriftValidator.ErrorMsg] = {
    validators.flatMap(_.validate(doc))
  }

  protected def parseFile(path: String): Document = {
    val importer = Importer(new File("."))
    val parser = new ThriftParser(importer, true, false)
    parser.parseFile(path)
  }

  protected def saveThriftAsScala(dstDir: File, doc: Document): Set[File] = {
    Generator.generateDoc(doc).map { case (sub, code) =>
        saveScalaCode(dstDir, sub, code)
    }.toSet
  }

  protected def saveScalaCode(base: File, filename: String, code: String): File = {
    val dst = new File(base, filename)
    val dir = dst.getParentFile
    if (dir != null && !dir.exists()) {
      dir.mkdirs()
    }

    val writer = new FileWriter(dst)
    writer.write(code)
    writer.close()

    dst
  }

  protected def throwValidationException(validationErrorsPerFile: List[(String, List[String])]) = {
    val builder = new StringBuilder("\n")

    validationErrorsPerFile.foreach {
      case (filepath, errors) =>
        builder.append(filepath + "\n")
        errors.map("  " + _ + "\n").foreach(builder.append)
    }

    throw new ThriftValidationException(builder.toString())
  }
}

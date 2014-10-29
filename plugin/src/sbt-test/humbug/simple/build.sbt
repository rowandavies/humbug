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
import au.com.cba.omnia.humbug.HumbugSBT
import au.com.cba.omnia.humbug.test.DummyNeverFailingValidator

version := "0.1"

scalaVersion := "2.10.2"

//append DummyNeverFailingValidator to check that sbt doesn't complain
HumbugSBT.humbugSettings ++
Seq(HumbugSBT.humbugThriftValidators in Test += new DummyNeverFailingValidator,
  HumbugSBT.humbugThriftValidators in Compile += new DummyNeverFailingValidator)

libraryDependencies ++= depend.scrooge() ++ Seq(
  "au.com.cba.omnia" %% "humbug-core" % System.getProperty("plugin.version")
)

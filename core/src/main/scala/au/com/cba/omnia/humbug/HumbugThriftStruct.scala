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

import com.twitter.scrooge.ThriftStruct

/**
  * HumbugThriftStruct is a marker trait to indicate that a thrift struct was generated using
  * the Humbug generator. This means:
  *  * it has an empty constructor to avoid the JVM 254 method parameters limit.
  *  * that the fields of the thrift struct are mutable and are used to construct it.
  *  * that it has members named after the fields in the thrift file with getters and setters.
  *  * that is has getters and setters `_1` corresponding to the index of a field.
  */
trait HumbugThriftStruct { self: ThriftStruct =>
}

//   Copyright 2015 Commonwealth Bank of Australia
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

package au.com.cba.omnia.humbug.macros.test

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary

import org.specs2.Specification
import org.specs2.matcher.{TerminationMatchers, ThrownExpectations}

import au.com.cba.omnia.humbug.macros.ArbitraryThriftSupport

object ArbitraryThriftMacroSpec
  extends Specification
  with ArbitraryThriftSupport { def is = s2"""
    Generate arbitrary thrift struct: Humbug  $arbitraryHumbugTest
    Generate arbitrary thrift struct: Scrooge $arbitraryScroogeTest
"""

  def arbitraryHumbugTest = {

    def isCustomerHumbug[T](v: T) = v match {
      case Some(_:CustomerHumbug) => true
      case _                      => false
    }

    val genCustomer = arbitrary[CustomerHumbug]
    val result = isCustomerHumbug(genCustomer.sample)

    result must_== true
  }

  def arbitraryScroogeTest = {

    def isCustomerScrooge[T](v: T) = v match {
      case Some(_:CustomerScrooge) => true
      case _                       => false
    }

    val genCustomer = arbitrary[CustomerScrooge]
    val result = isCustomerScrooge(genCustomer.sample)

    result must_== true
  }
}

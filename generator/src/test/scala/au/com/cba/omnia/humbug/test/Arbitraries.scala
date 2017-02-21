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

package au.com.cba.omnia.humbug.test

import scalaz._, Scalaz._

import scalaz.scalacheck.ScalazArbitrary._
import scalaz.scalacheck.ScalaCheckBinding._

import org.scalacheck._, Arbitrary._

object Arbitraries {
  implicit def TypesArbitrary: Arbitrary[Types] = Arbitrary((
    arbitrary[String]         |@|
    arbitrary[Boolean]        |@|
    arbitrary[Short]          |@|
    arbitrary[Int]            |@|
    arbitrary[Long]           |@|
    arbitrary[Double]         |@|
    arbitrary[Byte]           |@|
    arbitrary[Option[String]] |@|
    arbitrary[Option[Double]]
  ){ case (string, boolean, short, int, long, double, byte, optString, optDouble) =>
      val types = new Types()
      types.stringField    = string
      types.booleanField   = boolean
      types.shortField     = short
      types.intField       = int
      types.longField      = long
      types.doubleField    = double
      types.byteField      = byte
      types.optStringField = optString
      types.optDoubleField = optDouble

      types
  })

  implicit def ListishArbitrary: Arbitrary[Listish] = Arbitrary((
    arbitrary[Short] |@| arbitrary[List[String]]) { case (s, l) =>
      val listish = new Listish()
      listish.short = s
      listish.listy  = l

      listish
    })

  implicit def MapishArbitrary: Arbitrary[Mapish] = Arbitrary((
    arbitrary[Int] |@| arbitrary[Map[String, Int]]) { case (i, m) =>
      val mapish = new Mapish()
      mapish.int = i
      mapish.mapy = m

      mapish
  })

  implicit def NestedArbitrary: Arbitrary[Nested] = Arbitrary(
    arbitrary[Map[String, List[Int]]].map { l =>
      val nested = new Nested
      nested.mapy = l

      nested
    })
}

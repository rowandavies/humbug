#   Copyright 2014 Commonwealth Bank of Australia
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

#@namespace scala au.com.cba.omnia.humbug.test

struct Types {
  1: string  stringField
  2: bool    booleanField
  3: i16     shortField
  4: i32     intField
  5: i64     longField
  6: double  doubleField
  7: byte    byteField
  8: optional string optStringField
  9: optional double optDoubleField
}

struct Listish {
  1: i16 short
  2: list<string> listy
}

struct Mapish {
  1: i32 int
  2: map<string, i32> mapy
}

struct Nested {
  1: map<string, list<i32>> mapy
}

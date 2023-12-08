/*
 * Copyright 2023 record4s authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tarao.record4s
package ujson

import ReadWriter.{writer, reader}

// import upickle.default.ReadWriter.join

class CodecSpec extends helper.UnitSpec {
  describe("ArrayRecord") {
    // ArrayRecord can be encoded/decoded without any special codec

    describe("encoding") {
      it("should encode an array record to json") {
        val r = ArrayRecord(name = "tarao", age = 3)
        val json = upickle.default.write(r)(writer)
        json shouldBe """{"name":"tarao","age":3}"""
      }
    }

    describe("decoding") {
      it("should decode json to an array record") {
        val json = """{"name":"tarao","age":3}"""
        val jsonObj = _root_.ujson.read(json)
        val record =
          upickle.default.read[ArrayRecord[(("name", String), ("age", Int))]](jsonObj)(reader)
        record.name shouldBe "tarao"
        record.age shouldBe 3
      }
    }
  }

  //     it("should encode a nested array record to json") {
  //       val r = ArrayRecord(
  //         name  = "tarao",
  //         age   = 3,
  //         email = ArrayRecord(user = "tarao", domain = "example.com"),
  //       )
  //       val json = r.asJson.noSpaces
  //       json shouldBe """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //     }
  //   }

  //   describe("decoding") {
  //     it("should decode json to an array record") {
  //       val json = """{"name":"tarao","age":3}"""
  //       val ShouldBeRight(jsonObj) = parse(json)
  //       val ShouldBeRight(record) =
  //         jsonObj.as[ArrayRecord[(("name", String), ("age", Int))]]
  //       record.name shouldBe "tarao"
  //       record.age shouldBe 3
  //     }

  //     it("should decode json to a nested array record") {
  //       val json =
  //         """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //       val ShouldBeRight(jsonObj) = parse(json)
  //       val ShouldBeRight(record) = jsonObj.as[ArrayRecord[
  //         (
  //           ("name", String),
  //           ("age", Int),
  //           ("email", ArrayRecord[(("user", String), ("domain", String))]),
  //         ),
  //       ]]
  //       record.name shouldBe "tarao"
  //       record.age shouldBe 3
  //       record.email.user shouldBe "tarao"
  //       record.email.domain shouldBe "example.com"
  //     }

  //     it("can decode partially") {
  //       locally {
  //         val json = """{"name":"tarao","age":3}"""
  //         val ShouldBeRight(jsonObj) = parse(json)
  //         val ShouldBeRight(record) =
  //           jsonObj.as[ArrayRecord[("name", String) *: EmptyTuple]]
  //         record.name shouldBe "tarao"
  //         "record.age" shouldNot typeCheck
  //       }

  //       locally {
  //         val json =
  //           """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //         val ShouldBeRight(jsonObj) = parse(json)
  //         val ShouldBeRight(record) = jsonObj.as[ArrayRecord[
  //           ("email", ArrayRecord[("domain", String) *: EmptyTuple]) *:
  //             EmptyTuple,
  //         ]]
  //         "record.name" shouldNot typeCheck
  //         "record.age" shouldNot typeCheck
  //         "record.email.user" shouldNot typeCheck
  //         record.email.domain shouldBe "example.com"
  //       }
  //     }
  //   }
  // }

  // describe("%") {
  //   import Codec.{decoder, encoder}

  //   describe("encoder") {
  //     it("should encode a record to json") {
  //       val r = %(name = "tarao", age = 3)
  //       val json = r.asJson.noSpaces
  //       json shouldBe """{"name":"tarao","age":3}"""
  //     }

  //     it("should encode a nested record to json") {
  //       val r = %(
  //         name  = "tarao",
  //         age   = 3,
  //         email = %(user = "tarao", domain = "example.com"),
  //       )
  //       val json = r.asJson.noSpaces
  //       json shouldBe """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //     }
  //   }

  //   describe("decoder") {
  //     it("should decode json to a record") {
  //       val json = """{"name":"tarao","age":3}"""
  //       val ShouldBeRight(jsonObj) = parse(json)
  //       val ShouldBeRight(record) =
  //         jsonObj.as[% { val name: String; val age: Int }]
  //       record.name shouldBe "tarao"
  //       record.age shouldBe 3
  //     }

  //     it("should decode json to a nested record") {
  //       val json =
  //         """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //       val ShouldBeRight(jsonObj) = parse(json)
  //       val ShouldBeRight(record) = jsonObj.as[
  //         % {
  //           val name: String; val age: Int;
  //           val email: % { val user: String; val domain: String }
  //         },
  //       ]
  //       record.name shouldBe "tarao"
  //       record.age shouldBe 3
  //       record.email.user shouldBe "tarao"
  //       record.email.domain shouldBe "example.com"
  //     }

  //     it("can decode partially") {
  //       locally {
  //         val json = """{"name":"tarao","age":3}"""
  //         val ShouldBeRight(jsonObj) = parse(json)
  //         val ShouldBeRight(record) = jsonObj.as[% { val name: String }]
  //         record.name shouldBe "tarao"
  //         "record.age" shouldNot typeCheck
  //       }

  //       locally {
  //         val json =
  //           """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
  //         val ShouldBeRight(jsonObj) = parse(json)
  //         val ShouldBeRight(record) =
  //           jsonObj.as[% { val email: % { val domain: String } }]
  //         "record.name" shouldNot typeCheck
  //         "record.age" shouldNot typeCheck
  //         "record.email.user" shouldNot typeCheck
  //         record.email.domain shouldBe "example.com"
  //       }
  //     }
  //   }
  // }
}

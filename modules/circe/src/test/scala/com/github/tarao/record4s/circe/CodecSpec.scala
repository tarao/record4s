/*
 * Copyright (c) 2023 record4s authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.tarao.record4s
package circe

import io.circe.generic.auto.*
import io.circe.parser.parse
import io.circe.syntax.*

class CodecSpec extends helper.UnitSpec {
  describe("ArrayRecord") {
    // ArrayRecord can be encoded/decoded without any special codec

    describe("encoding") {
      it("should encode an array record to json") {
        val r = ArrayRecord(name = "tarao", age = 3)
        val json = r.asJson.noSpaces
        json shouldBe """{"name":"tarao","age":3}"""
      }

      it("should encode a nested array record to json") {
        val r = ArrayRecord(
          name  = "tarao",
          age   = 3,
          email = ArrayRecord(user = "tarao", domain = "example.com"),
        )
        val json = r.asJson.noSpaces
        json shouldBe """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
      }
    }

    describe("decoding") {
      it("should decode json to an array record") {
        val json = """{"name":"tarao","age":3}"""
        val ShouldBeRight(jsonObj) = parse(json)
        val ShouldBeRight(record) =
          jsonObj.as[ArrayRecord[(("name", String), ("age", Int))]]
        record.name shouldBe "tarao"
        record.age shouldBe 3
      }

      it("should decode json to a nested array record") {
        val json =
          """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
        val ShouldBeRight(jsonObj) = parse(json)
        val ShouldBeRight(record) = jsonObj.as[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("user", String), ("domain", String))]),
          ),
        ]]
        record.name shouldBe "tarao"
        record.age shouldBe 3
        record.email.user shouldBe "tarao"
        record.email.domain shouldBe "example.com"
      }

      it("can decode partially") {
        locally {
          val json = """{"name":"tarao","age":3}"""
          val ShouldBeRight(jsonObj) = parse(json)
          val ShouldBeRight(record) =
            jsonObj.as[ArrayRecord[("name", String) *: EmptyTuple]]
          record.name shouldBe "tarao"
          "record.age" shouldNot typeCheck
        }

        locally {
          val json =
            """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
          val ShouldBeRight(jsonObj) = parse(json)
          val ShouldBeRight(record) = jsonObj.as[ArrayRecord[
            ("email", ArrayRecord[("domain", String) *: EmptyTuple]) *:
              EmptyTuple,
          ]]
          "record.name" shouldNot typeCheck
          "record.age" shouldNot typeCheck
          "record.email.user" shouldNot typeCheck
          record.email.domain shouldBe "example.com"
        }
      }
    }
  }

  describe("%") {
    import Codec.{decoder, encoder}

    describe("encoder") {
      it("should encode a record to json") {
        val r = %(name = "tarao", age = 3)
        val json = r.asJson.noSpaces
        json shouldBe """{"name":"tarao","age":3}"""
      }

      it("should encode a nested record to json") {
        val r = %(
          name  = "tarao",
          age   = 3,
          email = %(user = "tarao", domain = "example.com"),
        )
        val json = r.asJson.noSpaces
        json shouldBe """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
      }
    }

    describe("decoder") {
      it("should decode json to a record") {
        val json = """{"name":"tarao","age":3}"""
        val ShouldBeRight(jsonObj) = parse(json)
        val ShouldBeRight(record) =
          jsonObj.as[% { val name: String; val age: Int }]
        record.name shouldBe "tarao"
        record.age shouldBe 3
      }

      it("should decode json to a nested record") {
        val json =
          """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
        val ShouldBeRight(jsonObj) = parse(json)
        val ShouldBeRight(record) = jsonObj.as[
          % {
            val name: String; val age: Int;
            val email: % { val user: String; val domain: String }
          },
        ]
        record.name shouldBe "tarao"
        record.age shouldBe 3
        record.email.user shouldBe "tarao"
        record.email.domain shouldBe "example.com"
      }

      it("can decode partially") {
        locally {
          val json = """{"name":"tarao","age":3}"""
          val ShouldBeRight(jsonObj) = parse(json)
          val ShouldBeRight(record) = jsonObj.as[% { val name: String }]
          record.name shouldBe "tarao"
          "record.age" shouldNot typeCheck
        }

        locally {
          val json =
            """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
          val ShouldBeRight(jsonObj) = parse(json)
          val ShouldBeRight(record) =
            jsonObj.as[% { val email: % { val domain: String } }]
          "record.name" shouldNot typeCheck
          "record.age" shouldNot typeCheck
          "record.email.user" shouldNot typeCheck
          record.email.domain shouldBe "example.com"
        }
      }
    }
  }
}

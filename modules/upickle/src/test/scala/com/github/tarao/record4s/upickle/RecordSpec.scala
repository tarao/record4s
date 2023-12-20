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
package upickle

class RecordSpec extends helper.UnitSpec {
  describe("Record.readWriter") {
    import upickle.Record.readWriter

    describe("write") {
      import _root_.upickle.default.{write, writeJs}

      it("should write a record") {
        val r = %(name = "tarao", age = 3)
        write(r) shouldBe """{"name":"tarao","age":3}"""
        writeJs(r) shouldBe ujson.Obj(
          "name" -> ujson.Str("tarao"),
          "age"  -> ujson.Num(3),
        )
      }

      it("should write a nested record") {
        val r = %(
          name  = "tarao",
          age   = 3,
          email = %(user = "tarao", domain = "example.com"),
        )
        write(
          r,
        ) shouldBe """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
        writeJs(r) shouldBe ujson.Obj(
          "name" -> ujson.Str("tarao"),
          "age"  -> ujson.Num(3),
          "email" -> ujson.Obj(
            "user"   -> ujson.Str("tarao"),
            "domain" -> ujson.Str("example.com"),
          ),
        )
      }
    }

    describe("read") {
      import _root_.upickle.default.read

      it("should read a record") {
        val json = """{"name":"tarao","age":3}"""
        val r = read[% { val name: String; val age: Int }](json)
        r shouldStaticallyBe a[% { val name: String; val age: Int }]
        r.name shouldBe "tarao"
        r.age shouldBe 3
      }

      it("should read a nested record") {
        val json =
          """{"name":"tarao","age":3,"email":{"user":"tarao","domain":"example.com"}}"""
        val r = read[
          % {
            val name: String; val age: Int;
            val email: % { val user: String; val domain: String }
          },
        ](json)
        r shouldStaticallyBe a[
          % {
            val name: String; val age: Int;
            val email: % { val user: String; val domain: String }
          },
        ]
        r.name shouldBe "tarao"
        r.age shouldBe 3
        r.email.user shouldBe "tarao"
        r.email.domain shouldBe "example.com"
      }
    }
  }
}

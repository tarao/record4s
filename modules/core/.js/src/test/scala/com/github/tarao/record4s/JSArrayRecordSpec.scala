package com.github.tarao.record4s

import scala.scalajs.js

class JSArrayRecordSpec extends helper.UnitSpec {
  describe("ArrayRecord in JS") {
    describe("fromJS") {
      it("should convert js.Any to ArrayRecord") {
        val obj: js.Any = js.Dynamic.literal(name = "tarao", age = 3)
        val r = ArrayRecord.fromJS[(("name", String), ("age", Int))](obj)
        r shouldStaticallyBe a[ArrayRecord[(("name", String), ("age", Int))]]
        r.name shouldBe "tarao"
        r.age shouldBe 3
      }

      it("should convert js.Any to nested ArrayRecord") {
        val obj: js.Any = js
          .Dynamic
          .literal(
            name = "tarao",
            age  = 3,
            email = js
              .Dynamic
              .literal(
                local  = "tarao",
                domain = "example.com",
              ),
          )
        val r = ArrayRecord.fromJS[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("local", String), ("domain", String))]),
          ),
        ](obj)
        r shouldStaticallyBe a[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("local", String), ("domain", String))]),
          ),
        ]]
        r.name shouldBe "tarao"
        r.age shouldBe 3
        r.email.local shouldBe "tarao"
        r.email.domain shouldBe "example.com"
      }

      it("should ignore extra fields") {
        val obj: js.Any = js.Dynamic.literal(name = "tarao", age = 3)
        val r = ArrayRecord.fromJS[("name", String) *: EmptyTuple](obj)
        r shouldStaticallyBe a[ArrayRecord[("name", String) *: EmptyTuple]]
        r.name shouldBe "tarao"
        "r.age" shouldNot typeCheck
      }

      it("should fill nulls for missing fields") {
        val obj: js.Any = js.Dynamic.literal()
        val r = ArrayRecord.fromJS[(("name", String), ("age", Int))](obj)
        r shouldStaticallyBe a[ArrayRecord[(("name", String), ("age", Int))]]
        r.name shouldBe null
        r.age shouldBe 0
      }

      it("should throw if an object field is missing") {
        val obj: js.Any = js.Dynamic.literal(name = "tarao", age = 3)
        a[java.lang.RuntimeException] should be thrownBy
          ArrayRecord.fromJS[
            (
              ("name", String),
              ("age", Int),
              ("email", ArrayRecord[(("local", String), ("domain", String))]),
            ),
          ](obj)
      }
    }

    describe("fromJSON") {
      it("should convert JSON String to ArrayRecord") {
        val json = """{"name":"tarao","age":3}"""
        val r = ArrayRecord.fromJSON[(("name", String), ("age", Int))](json)
        r shouldStaticallyBe a[ArrayRecord[(("name", String), ("age", Int))]]
        r.name shouldBe "tarao"
        r.age shouldBe 3
      }

      it("should convert JSON String to nested ArrayRecord") {
        val json =
          """{"name":"tarao","age":3,"email":{"local":"tarao","domain":"example.com"}}"""
        val r = ArrayRecord.fromJSON[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("local", String), ("domain", String))]),
          ),
        ](json)
        r shouldStaticallyBe a[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("local", String), ("domain", String))]),
          ),
        ]]
        r.name shouldBe "tarao"
        r.age shouldBe 3
        r.email.local shouldBe "tarao"
        r.email.domain shouldBe "example.com"
      }

      it("should ignore extra fields") {
        val json = """{"name":"tarao","age":3}"""
        val r = ArrayRecord.fromJSON[("name", String) *: EmptyTuple](json)
        r shouldStaticallyBe a[ArrayRecord[("name", String) *: EmptyTuple]]
        r.name shouldBe "tarao"
        "r.age" shouldNot typeCheck
      }

      it("should fill nulls for missing fields") {
        val json = """{}"""
        val r = ArrayRecord.fromJSON[(("name", String), ("age", Int))](json)
        r shouldStaticallyBe a[ArrayRecord[(("name", String), ("age", Int))]]
        r.name shouldBe null
        r.age shouldBe 0
      }

      it("should throw if an object field is missing") {
        val json = """{"name":"tarao","age":3}"""
        a[java.lang.RuntimeException] should be thrownBy
          ArrayRecord.fromJSON[
            (
              ("name", String),
              ("age", Int),
              ("email", ArrayRecord[(("local", String), ("domain", String))]),
            ),
          ](json)
      }
    }

    describe("toJS") {
      it("should convert ArrayRecord to js.Any") {
        val r = ArrayRecord(name = "tarao", age = 3)
        val obj = r.toJS
        obj shouldStaticallyBe a[js.Any]
        js.JSON.stringify(obj) shouldBe """{"name":"tarao","age":3}"""
      }

      it("should convert nested ArrayRecord to js.Any") {
        val r = ArrayRecord(
          name = "tarao",
          age  = 3,
          email = ArrayRecord(
            local  = "tarao",
            domain = "example.com",
          ),
        )
        val obj = r.toJS
        obj shouldStaticallyBe a[js.Any]
        js.JSON.stringify(obj) shouldBe """{"name":"tarao","age":3,"email":{"local":"tarao","domain":"example.com"}}"""
      }
    }

    describe("toJSON") {
      it("should convert ArrayRecord to JSON String") {
        val r = ArrayRecord(name = "tarao", age = 3)
        val json = r.toJSON
        json shouldBe """{"name":"tarao","age":3}"""
      }

      it("should convert nested ArrayRecord to JSON String") {
        val r = ArrayRecord(
          name = "tarao",
          age  = 3,
          email = ArrayRecord(
            local  = "tarao",
            domain = "example.com",
          ),
        )
        val json = r.toJSON
        json shouldBe """{"name":"tarao","age":3,"email":{"local":"tarao","domain":"example.com"}}"""
      }
    }
  }
}

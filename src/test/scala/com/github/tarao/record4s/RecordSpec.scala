package com.github.tarao.record4s

class RecordSpec extends helper.UnitSpec {
  describe("Record") {
    it("can be created as an empty record") {
      val r = %()
    }

    it("can be created with multiple fields") {
      val r1 = %(name = "tarao", age = 3)
      r1.name shouldBe "tarao"
      r1.age shouldBe 3

      val r2 = %(("name", "tarao"), ("age", 3))
      r2.name shouldBe "tarao"
      r2.age shouldBe 3

      val r3 = %("name" -> "tarao", "age" -> 3)
      r3.name shouldBe "tarao"
      r3.age shouldBe 3
    }

    it("can be created by adding fields") {
      val r1 = %(name = "tarao")
      r1.name shouldBe "tarao"

      val r2 = r1 + (age = 3)
      r2.name shouldBe "tarao"
      r2.age shouldBe 3

      val r3 = r1 + (age = 3) + (email = "tarao@example.com")
      r3.name shouldBe "tarao"
      r3.age shouldBe 3
      r3.email shouldBe "tarao@example.com"
    }

    it("should allow replacing existing fields") {
      val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r1 + (age = r1.age + 1, email = "tarao2@example.com")
      r2.name shouldBe "tarao"
      r2.age shouldBe 4
      r2.email shouldBe "tarao2@example.com"

      val r3 = r2 + (email = %(user = "tarao", domain = "example.com"))
      r3.name shouldBe "tarao"
      r3.age shouldBe 4
      r3.email.user shouldBe "tarao"
      r3.email.domain shouldBe "example.com"
    }

    it("should not allow non-literal labels") {
      val label = "name"
      """%((label, "tarao"))""" shouldNot compile

      """%(("age", 3), (label, "tarao"))""" shouldNot compile

      """%("age" -> 3, label -> "tarao")""" shouldNot compile
    }

    it("should not allow non-vararg construction") {
      val args = Seq("name" -> "tarao")
      "%(args: _*)" shouldNot compile
    }

    it("should not allow accessing non-existing fields") {
      val r1 = %(name = "tarao", age = 3)
      "r1.email" shouldNot compile

      val r2: Record { val name: String } = r1
      "r2.age" shouldNot compile
    }

    it("should not allow nothing except named construction by `apply`") {
      """%.foo(name = "tarao")""" shouldNot compile

      """%("tarao")""" shouldNot compile
    }

    describe("typeOf[Record]") {
      it("should have simple type for an empty record") {
        val r = Record.empty
        helper.showTypeOf(r) shouldBe "%"
      }

      it("should have refinement type") {
        val r = %(name = "tarao")
        helper.showTypeOf(r) shouldBe """% {
                                        |  val name: String
                                        |}""".stripMargin
      }

      it("should combine refinement type for added fields") {
        val r1 = %(name = "tarao")
        val r2 = r1 + (age = 3) + (email = "tarao@example.com")
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}""".stripMargin
      }

      it("should take the last field type of the same name") {
        val r1 = %(name = "tarao")
        val r2 = r1 + (age  = 3) + (email = "tarao@example.com")
        val r3 = r2 + (name = "ikura") + (email =
          %(user = "ikura", domain = "example.com"),
        )
        helper.showTypeOf(r3) shouldBe """% {
                                         |  val age: Int
                                         |  val name: String
                                         |  val email: % {
                                         |    val user: String
                                         |    val domain: String
                                         |  }
                                         |}""".stripMargin
      }
    }

    describe(".toString()") {
      it("can express empty Record") {
        Record.empty.toString() shouldBe "%()"
      }

      it("can express Record with a field") {
        val r = %(foo = 42)
        r.toString() shouldBe "%(foo = 42)"
      }

      it("can express Record with fields") {
        val r = %(foo = 42, bar = "yay!")
        // TODO: Should we sort fields?
        r.toString() shouldBe "%(foo = 42, bar = yay!)"
      }

      it("can express nested Record") {
        val r0 = %(foo = 42, bar = "yay!")
        val rr1 = %(buzz = true)
        val r1 = r0 + (child = rr1)

        r1.toString shouldBe "%(foo = 42, bar = yay!, child = %(buzz = true))"
      }
    }
  }
}

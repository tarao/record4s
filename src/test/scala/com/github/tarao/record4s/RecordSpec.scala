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

    describe("++") {
      it("should allow concatenating two records") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(email = "tarao@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "tarao@example.com"

        val r4 = %(email = "tarao@example.com", occupation = "engineer")
        val r5 = r1 ++ r4
        r5.name shouldBe "tarao"
        r5.age shouldBe 3
        r5.email shouldBe "tarao@example.com"
        r5.occupation shouldBe "engineer"
      }

      it("should take the last value for duplicated fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val r2 = %(name = "ikura", email = "ikura@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "ikura"
        r3.age shouldBe 3
        r3.email shouldBe "ikura@example.com"
      }

      it("should not take type-hidden value") {
        val r1 = %(name = "tarao", age = 3)
        val r2: % { val name: String } =
          %(name = "ikura", age = 1, email = "ikura@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "ikura"
        r3.age shouldBe 3
        "r3.email" shouldNot compile
      }
    }

    describe("|+|") {
      it("should allow concatenating two disjoint records") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(email = "tarao@example.com")
        val r3 = r1 |+| r2
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "tarao@example.com"

        val r4 = %(occupation = "engineer")
        val r5 = r1 |+| r4
        r5.name shouldBe "tarao"
        r5.age shouldBe 3
        r5.occupation shouldBe "engineer"
      }

      it("should reject duplicated fields") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(name = "ikura", email = "ikura@example.com")
        "r1 |+| r2" shouldNot compile
      }

      it("should not break other operations") {
        val r1 = %(name = "tarao") |+| %(age = 3)
        val r2 = %(email = "tarao@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "tarao@example.com"

        val r4 = r1 + (email = "tarao@example.com")
        r4.name shouldBe "tarao"
        r4.age shouldBe 3
        r4.email shouldBe "tarao@example.com"
      }

      it("should not affected by type-hidden values") {
        val r1 = %(name = "tarao", age = 3)
        val r2: % { val email: String } =
          %(name = "ikura", age = 1, email = "ikura@example.com")
        val r3 = r1 |+| r2
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "ikura@example.com"
      }
    }

    describe("as[]") {
      it("should allow returning the same type") {
        val r = %(name = "tarao", age = 3)
        helper.showTypeOf(r.as) shouldBe """% {
                                           |  val name: String
                                           |  val age: Int
                                           |}""".stripMargin
      }

      it("should allow upcast") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = r1.as[% { val name: String }]
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val name: String
                                         |}""".stripMargin
      }

      it("should reject downcast") {
        val r1 = %(name = "tarao")
        "val r2 = r1.as[% { val name: String; val age: Int }]" shouldNot compile
      }

      it("should reject unrelated types") {
        val r1 = %(name = "tarao", age = 3)
        "val r2 = r1.as[% { val name: String; val email: String }]" shouldNot compile
      }

      it("should strip away statically invisible fields") {
        val r1 = %(name = "tarao", age = 3)
        val r2: % { val name: String } = r1
        val r3 = r1.as[% { val name: String }]

        r2.as.toString() shouldBe "%(name = tarao)"
        r3.toString() shouldBe "%(name = tarao)"
      }
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

      """%("tarao", age = 3)""" shouldNot compile
    }

    describe("==") {
      it("should test equality of two records") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(name = "tarao", age = 3)
        val r3 = %(name = "ikura", age = 1)
        val r4 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val r5: % { val name: String; val age: Int } = r4
        val r6 = %(age = 3) |+| %(name = "tarao")
        val r7 = r1 ++ r6

        (r1 == r1) shouldBe true
        (r1 == r2) shouldBe true
        (r2 == r1) shouldBe true
        (r1 == r3) shouldBe false
        (r3 == r1) shouldBe false
        (r1 == r4) shouldBe false
        (r4 == r1) shouldBe false
        (r1 == r5) shouldBe false
        (r5 == r1) shouldBe false
        (r1 == r5.as) shouldBe true
        (r5.as == r1) shouldBe true
        (r1 == r6) shouldBe true
        (r6 == r1) shouldBe true
        (r1 == r7) shouldBe true
        (r7 == r1) shouldBe true
      }

      it("should statically reject equality test of different types") {
        val r1 = %(name = "tarao", age = 3)
        case class Person(name: String, age: Int)
        val r2 = Person("tarao", 3)

        "r1 == r2" shouldNot compile
      }
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

      it("should combine refinement type of concatenated records") {
        val r1 = %(name = "tarao")
        val r2 = %(age = 3, email = "tarao@example.com")
        helper.showTypeOf(r1 ++ r2) shouldBe """% {
                                               |  val name: String
                                               |  val age: Int
                                               |  val email: String
                                               |}""".stripMargin
        val r3 = r1 ++ r2
        helper.showTypeOf(r3) shouldBe """% {
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

        val r4 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val r5 = %(name = "ikura") + (email =
          %(user = "ikura", domain = "example.com"),
        )
        helper.showTypeOf(r4 ++ r5) shouldBe """% {
                                               |  val age: Int
                                               |  val name: String
                                               |  val email: % {
                                               |    val user: String
                                               |    val domain: String
                                               |  }
                                               |}""".stripMargin
      }

      it("should combine refinement type of directly concatenated records") {
        val r1 = %(name = "tarao")
        val r2 = %(age = 3, email = "tarao@example.com")
        helper.showTypeOf(r1 |+| r2) shouldBe """% {
                                                |  val name: String
                                                |} & % {
                                                |  val age: Int
                                                |  val email: String
                                                |}""".stripMargin
        val r3 = r1 |+| r2
        helper.showTypeOf(r3) shouldBe """% {
                                         |  val name: String
                                         |} & % {
                                         |  val age: Int
                                         |  val email: String
                                         |}""".stripMargin
      }

      it("should flatten field types after ++ or +") {
        val r1 = %(name = "tarao")
        val r2 = %(age = 3)
        val r3 = %(email = "tarao@example.com")
        helper.showTypeOf((r1 |+| r2) ++ r3) shouldBe """% {
                                                        |  val name: String
                                                        |  val age: Int
                                                        |  val email: String
                                                        |}""".stripMargin
        val r4 = (r1 |+| r2) + (email = "tarao@example.com")
        helper.showTypeOf(r4) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
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

package com.github.tarao.record4s

class RecordSpec extends helper.UnitSpec {
  describe("Record") {
    describe("Construction") {
      it("can create an empty record") {
        val r = %()
      }

      it("can create records with multiple fields") {
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

      it("can create records by adding fields") {
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

      it("should replace existing fields") {
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

      it("should reject non-literal labels") {
        val label = "name"
        """%((label, "tarao"))""" shouldNot compile

        """%(("age", 3), (label, "tarao"))""" shouldNot compile

        """%("age" -> 3, label -> "tarao")""" shouldNot compile
      }

      it("should not allow '$' in labels") {
        "%($value = 3)" shouldNot compile
        "%(value$ = 3)" shouldNot compile
        "%($minusfoobar = 3)" shouldNot compile
        "%(foo$minusbar = 3)" shouldNot compile
        "%(foobar$minus = 3)" shouldNot compile

        case class Cell($value: Int)
        "Record.from(Cell(3))" shouldNot compile
      }

      it("should allow other signs in labels") {
        val r = %(`foo-bar` = 3)
        r.`foo-bar` shouldBe 3
      }

      it("should reject non-vararg construction") {
        val args = Seq("name" -> "tarao")
        "%(args: _*)" shouldNot compile
      }

      it("should reject accessing non-existing fields") {
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
    }

    describe("Type of %") {
      it("should be a simple type for an empty record") {
        val r = Record.empty
        helper.showTypeOf(r) shouldBe "%"
      }

      it("should be refinement type") {
        val r = %(name = "tarao")
        helper.showTypeOf(r) shouldBe """% {
                                        |  val name: String
                                        |}""".stripMargin
      }

      it("should be combined refinement type of added fields") {
        val r1 = %(name = "tarao")
        val r2 = r1 + (age = 3) + (email = "tarao@example.com")
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}""".stripMargin
      }

      it("should be combined refinement type of concatenated records") {
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

      it("should have flattened field types after ++ or +") {
        val r1 = %(name = "tarao")
        val r2 = %(age = 3)
        val r3 = %(email = "tarao@example.com")
        helper.showTypeOf((r1 ++ r2) ++ r3) shouldBe """% {
                                                       |  val name: String
                                                       |  val age: Int
                                                       |  val email: String
                                                       |}""".stripMargin
        val r4 = (r1 ++ r2) + (email = "tarao@example.com")
        helper.showTypeOf(r4) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}""".stripMargin
      }
    }

    describe("Lookup") {
      it("should return a value by a string key name") {
        val r = %(name = "tarao", age = 3)
        Record.lookup(r, "name") shouldBe a[String]
        Record.lookup(r, "name") shouldBe "tarao"
        Record.lookup(r, "age") shouldBe an[Int]
        Record.lookup(r, "age") shouldBe 3
      }

      it("should reject non-literal key names") {
        val r = %(name = "tarao", age = 3)
        val key = "name"
        "Record.lookup(r, key)" shouldNot compile
      }

      it("should allow shadowed field to be extracted") {
        val r = %(toString = 10)
        r.toString shouldBe a[String]
        r.toString shouldBe "%(toString = 10)"
        Record.lookup(r, "toString") shouldBe an[Int]
        Record.lookup(r, "toString") shouldBe 10
      }
    }

    describe("++") {
      it("should allow concatenation of two records") {
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

    describe(".tag[]") {
      it("should give a tag") {
        trait MyType
        trait AnotherType

        val r0 = %(name = "tarao", age = 3)
        "val t0: Tag[MyType]] = r0" shouldNot compile

        val r1 = r0.tag[MyType]
        r1 shouldBe a[Tag[MyType]]
        val t1: Tag[MyType] = r1

        val r2 = r1.tag[AnotherType]
        r2 shouldBe a[Tag[MyType]]
        r2 shouldBe a[Tag[AnotherType]]
        val t2: Tag[MyType] = r2
        val t3: Tag[AnotherType] = r2
      }

      it("should be a target of extension method defined in a tagged type") {
        trait Person
        object Person {
          extension (p: % { val name: String } & Tag[Person]) {
            def firstName: String = p.name.split(" ").head
          }
        }

        val r0 = %(name = "tarao fuguta", age = 3)
        "r0.firstName" shouldNot compile

        val r1 = r0.tag[Person]
        r1.firstName shouldBe "tarao"

        val r2 = %(age = 3).tag[Person]
        "r2.firstName" shouldNot compile
      }

      it("should preserve tags after concatenation") {
        trait MyType
        trait AnotherType

        val r1 = %(name = "tarao", age = 3).tag[MyType]

        val r2 = r1 ++ %(email = "tarao@example.com")
        r2 shouldBe a[Tag[MyType]]
        val t2: Tag[MyType] = r2

        val r3 = r1 + (email = "tarao@example.com")
        r3 shouldBe a[Tag[MyType]]
        val t3: Tag[MyType] = r3

        val r4 = r1.tag[AnotherType]

        val r5 = r4 ++ %(email = "tarao@example.com")
        r5 shouldBe a[Tag[MyType]]
        r5 shouldBe a[Tag[AnotherType]]
        val t4: Tag[MyType] = r5
        val t5: Tag[AnotherType] = r5

        val r6 = r4 + (email1 = "tarao@example.com") + (occupation = "engineer")
        r6 shouldBe a[Tag[MyType]]
        r6 shouldBe a[Tag[AnotherType]]
        val t6: Tag[MyType] = r6
        val t7: Tag[AnotherType] = r6
      }

      it("should join tags from multiple records") {
        trait MyType
        trait YourType
        trait AnotherType
        trait YetAnotherType

        val r1 = %(name = "tarao").tag[MyType]
        val r2 = %(age = 3).tag[YourType]

        val r3 = r1 ++ r2
        r3 shouldBe a[Tag[MyType]]
        r3 shouldBe a[Tag[YourType]]
        val t3: Tag[MyType] = r3
        val t4: Tag[YourType] = r3

        val r4 = r1.tag[AnotherType]
        val r5 = r2.tag[YetAnotherType]

        val r6 = r4 ++ r5
        r6 shouldBe a[Tag[MyType]]
        r6 shouldBe a[Tag[AnotherType]]
        r6 shouldBe a[Tag[YourType]]
        r6 shouldBe a[Tag[YetAnotherType]]
        val t6: Tag[MyType] = r6
        val t7: Tag[AnotherType] = r6
        val t8: Tag[YourType] = r6
        val t9: Tag[YetAnotherType] = r6
      }

      it("should preserve tags after upcast") {
        trait MyType
        trait AnotherType

        val r1 = %(name = "tarao", age = 3).tag[MyType]

        val r2 = r1.as
        r2 shouldBe a[Tag[MyType]]
        val t2: Tag[MyType] = r2

        val r3 = r1.as[% { val name: String } & Tag[MyType]]
        r3 shouldBe a[Tag[MyType]]
        val t3: Tag[MyType] = r3

        val r4 = r1.tag[AnotherType]

        val r5 = r4.as
        r5 shouldBe a[Tag[MyType]]
        r5 shouldBe a[Tag[AnotherType]]
        val t4: Tag[MyType] = r5
        val t5: Tag[AnotherType] = r5

        val r6 = r4.as[% { val name: String } & Tag[MyType] & Tag[AnotherType]]
        r6 shouldBe a[Tag[MyType]]
        r6 shouldBe a[Tag[AnotherType]]
        val t6: Tag[MyType] = r6
        val t7: Tag[AnotherType] = r6
      }

      it("should not allow to give a tag by .as[]") {
        trait MyType

        val r1 = %(name = "tarao", age = 3)
        "r1.as[%{val name: String; val age: Int} & Tag[MyType]]" shouldNot compile
      }
    }

    describe(".as[]") {
      it("should return the same type if no type is specified") {
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

    describe(".to[]") {
      it("should convert a record to a Product") {
        case class Empty()
        val r0 = %()
        val e = r0.to[Empty]
        e shouldBe an[Empty]

        case class Cell(value: Int)
        val r1 = %(value = 10)
        val c = r1.to[Cell]
        c shouldBe a[Cell]
        c.value shouldBe 10

        case class Person(name: String, age: Int)
        val r2 = %(name = "tarao", age = 3)
        val p = r2.to[Person]
        p shouldBe a[Person]
        p.name shouldBe "tarao"
        p.age shouldBe 3
      }

      it("should not convert a record to a different shape of Product") {
        case class Person(name: String, age: Int)
        val r = %(value = 10)
        "r.to[Person]" shouldNot compile
      }

      it(
        "should not convert a record to a Product with incompatible field type",
      ) {
        case class Cell(value: Int)
        val r = %(value = "foo")
        "r.to[Cell]" shouldNot compile
      }
    }

    describe(".toTuple") {
      val r1 = %(name = "tarao", age = 3)
      val t1 = r1.toTuple
      t1 shouldBe a[("name", String) *: ("age", Int) *: EmptyTuple]
      t1._1._1 shouldBe "name"
      t1._1._2 shouldBe "tarao"
      t1._2._1 shouldBe "age"
      t1._2._2 shouldBe 3

      val r2: % { val age: Int } = r1
      val t2 = r2.toTuple
      t2 shouldBe a[("age", Int) *: EmptyTuple]
      t2._1._1 shouldBe "age"
      t2._1._2 shouldBe 3

      val r3: % { val age: Int; val name: String } = r1
      val t3 = r3.toTuple
      t3 shouldBe a[("age", Int) *: ("name", String) *: EmptyTuple]
      t3._1._1 shouldBe "age"
      t3._1._2 shouldBe 3
      t3._2._1 shouldBe "name"
      t3._2._2 shouldBe "tarao"
    }

    describe("Product support") {
      it("should convert a Product to a record") {
        case class Person(name: String, age: Int)
        val p = Person("tarao", 3)
        val r1 = Record.from(p)

        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.toString() shouldBe "%(name = tarao, age = 3)"
        helper.showTypeOf(r1) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |}""".stripMargin

        val t1: ("foo", Int) *: EmptyTuple = ("foo", 1) *: EmptyTuple
        val r2 = Record.from(t1)
        r2.foo shouldBe 1
        r2.toString() shouldBe "%(foo = 1)"
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val foo: Int
                                         |}""".stripMargin

        val t2: ("foo", Int) *: Int *: EmptyTuple = ("foo", 1) *: 3 *: EmptyTuple
        val r3 = Record.from(t2)
        r3.foo shouldBe 1
        r3.toString() shouldBe "%(foo = 1)"
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val foo: Int
                                         |}""".stripMargin
      }

      it("should allow Products to be concatenated to a record") {
        val r1 = %(email = "tarao@example.com")

        case class Person(name: String, age: Int)
        val p = Person("tarao", 3)

        val r2 = r1 ++ p
        r2.name shouldBe "tarao"
        r2.age shouldBe 3
        r2.email shouldBe "tarao@example.com"

        val t: ("foo", Int) *: EmptyTuple = ("foo", 1) *: EmptyTuple
        val r3 = r1 ++ p ++ t
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "tarao@example.com"
        r3.foo shouldBe 1

        val t2: ("foo", Int) *: Int *: EmptyTuple = ("foo", 1) *: 3 *: EmptyTuple
        val r4 = r1 ++ p ++ t
        r4.name shouldBe "tarao"
        r4.age shouldBe 3
        r4.email shouldBe "tarao@example.com"
        r4.foo shouldBe 1
      }
    }

    describe("==") {
      it("should test equality of two records") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(name = "tarao", age = 3)
        val r3 = %(name = "ikura", age = 1)
        val r4 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val r5: % { val name: String; val age: Int } = r4

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
      }

      it("should statically reject equality test of different types") {
        val r1 = %(name = "tarao", age = 3)
        case class Person(name: String, age: Int)
        val r2 = Person("tarao", 3)

        "r1 == r2" shouldNot compile
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

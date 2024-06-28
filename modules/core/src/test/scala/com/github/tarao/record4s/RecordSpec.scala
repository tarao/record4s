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

      it("can create records from identifiers") {
        val name = "tarao"
        val age = 3
        val r1 = %(name, age)
        r1.name shouldBe "tarao"
        r1.age shouldBe 3

        val r2 = %(name, age = 3)
        r2.name shouldBe "tarao"
        r2.age shouldBe 3

        val r3 = %(name = "tarao", age)
        r3.name shouldBe "tarao"
        r3.age shouldBe 3

        class A {
          val name = "tarao"
          def record = %(name)
        }
        val r4 = new A().record
        r4.name shouldBe "tarao"

        case class Person(name: String)
        val p = Person("tarao")
        val r5 = %(p.name)
        r5.name shouldBe "tarao"
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
        """%((label, "tarao"))""" shouldNot typeCheck

        """%(("age", 3), (label, "tarao"))""" shouldNot typeCheck

        """%("age" -> 3, label -> "tarao")""" shouldNot typeCheck
      }

      it("should not allow '$' in labels") {
        "%($value = 3)" shouldNot typeCheck
        "%(value$ = 3)" shouldNot typeCheck
        "%($minusfoobar = 3)" shouldNot typeCheck
        "%(foo$minusbar = 3)" shouldNot typeCheck
        "%(foobar$minus = 3)" shouldNot typeCheck

        case class Cell($value: Int)
        "Record.from(Cell(3))" shouldNot typeCheck
      }

      it("should allow other signs in labels") {
        val r = %(`foo-bar` = 3)
        r.`foo-bar` shouldBe 3
      }

      it("should reject non-vararg construction") {
        val args = Seq("name" -> "tarao")
        "%(args*)" shouldNot typeCheck
      }

      it("should reject accessing non-existing fields") {
        val r1 = %(name = "tarao", age = 3)
        "r1.email" shouldNot typeCheck

        val r2: Record { val name: String } = r1
        "r2.age" shouldNot typeCheck
      }

      it("should not allow nothing except named construction by `apply`") {
        """%.foo(name = "tarao")""" shouldNot typeCheck

        """%("tarao")""" shouldNot typeCheck

        """%("tarao", age = 3)""" shouldNot typeCheck
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
                                         |  val name: String
                                         |  val age: Int
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
                                               |  val name: String
                                               |  val age: Int
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

      it("should preserve opaque alias of field type") {
        import RecordSpec.Name

        locally {
          val r1 = %(name = Name("tarao"), age = 3)
          helper.showTypeOf(r1) shouldBe """% {
                                           |  val name: Name
                                           |  val age: Int
                                           |}""".stripMargin

          trait MyType

          val r2 = r1.tag[MyType]
          helper.showTypeOf(r2) shouldBe """% {
                                           |  val name: Name
                                           |  val age: Int
                                           |} & Tag[MyType]""".stripMargin

          val r3 = r2 + (email = "tarao@example.com")
          helper.showTypeOf(r3) shouldBe """% {
                                           |  val name: Name
                                           |  val age: Int
                                           |  val email: String
                                           |} & Tag[MyType]""".stripMargin
        }

        locally {
          val r1 = %(name = (Name("tarao"), Name("fuguta")), age = 3)
          helper.showTypeOf(r1) shouldBe """% {
                                           |  val name: Tuple2[Name, Name]
                                           |  val age: Int
                                           |}""".stripMargin

          trait MyType

          val r2 = r1.tag[MyType]
          helper.showTypeOf(r2) shouldBe """% {
                                           |  val name: Tuple2[Name, Name]
                                           |  val age: Int
                                           |} & Tag[MyType]""".stripMargin

          val r3 = r2 + (email = "tarao@example.com")
          helper.showTypeOf(r3) shouldBe """% {
                                           |  val name: Tuple2[Name, Name]
                                           |  val age: Int
                                           |  val email: String
                                           |} & Tag[MyType]""".stripMargin
        }
      }
    }

    describe("Record.lookup") {
      it("should return a value by a string key name") {
        val r = %(name = "tarao", age = 3)
        Record.lookup(r, "name") shouldStaticallyBe a[String]
        Record.lookup(r, "name") shouldBe "tarao"
        Record.lookup(r, "age") shouldStaticallyBe an[Int]
        Record.lookup(r, "age") shouldBe 3
      }

      it("should reject non-literal key names") {
        val r = %(name = "tarao", age = 3)
        val key = "name"
        "Record.lookup(r, key)" shouldNot typeCheck
      }

      it("should reject statically key names not in the record") {
        val r = %(name = "tarao", age = 3)
        """Record.lookup(r, "email")""" shouldNot typeCheck
      }

      it("should allow shadowed field to be extracted") {
        val r = %(toString = 10)
        r.toString shouldStaticallyBe a[String]
        r.toString shouldBe "%(toString = 10)"
        Record.lookup(r, "toString") shouldStaticallyBe an[Int]
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
        "r3.email" shouldNot typeCheck
      }
    }

    describe(".apply(select)") {
      it("should create a new record with selected fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name.age)
        r2.name shouldBe "tarao"
        r2.age shouldBe 3
        "r2.email" shouldNot typeCheck

        val r3 = r1(select)
        r3.toString shouldBe "%()"
      }

      it("should preserve the order of selection") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name.age)
        helper.showTypeOf(r2) shouldBe """% {
                                         |  val name: String
                                         |  val age: Int
                                         |}""".stripMargin

        val r3 = r1(select.age.name)
        helper.showTypeOf(r3) shouldBe """% {
                                         |  val age: Int
                                         |  val name: String
                                         |}""".stripMargin
      }

      it("should allow to rename fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name("nickname").age)
        r2.nickname shouldBe "tarao"
        r2.age shouldBe 3
        "r2.name" shouldNot typeCheck

        val r3 = r1(select.name(rename = "nickname").age.name)
        r3.nickname shouldBe "tarao"
        r3.age shouldBe 3
        r3.name shouldBe "tarao"
        helper.showTypeOf(r3) shouldBe """% {
                                         |  val nickname: String
                                         |  val age: Int
                                         |  val name: String
                                         |}""".stripMargin
      }

      it("should reject giving a new name by non-literal string") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val newName = "nickname"
        "r1(select.name(newName).age)" shouldNot typeCheck
      }

      it("should reject selecting missing fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")
        "r1(select.nickname)" shouldNot typeCheck
      }

      it("should take the last one for duplicated fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name("value").age("value"))
        r2.value shouldBe 3
        "r2.name" shouldNot typeCheck
        "r2.age" shouldNot typeCheck
      }
    }

    describe(".apply(unselect)") {
      it("should create a new record without unselected fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(unselect.email)
        r2.name shouldBe "tarao"
        r2.age shouldBe 3
        "r2.email" shouldNot typeCheck

        val r3 = r1(unselect)
        r3 shouldBe r1
      }

      it("should do nothing for missing fields") {
        val r1 = %(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(unselect.nickname)
        r2 shouldBe r1

        val r3 = r1(unselect.email.age.nickname)
        r3.name shouldBe "tarao"
        "r3.age" shouldNot typeCheck
        "r3.email" shouldNot typeCheck
      }
    }

    describe(".tag[]") {
      it("should give a tag") {
        trait MyType
        trait AnotherType

        val r0 = %(name = "tarao", age = 3)
        "val t0: Tag[MyType] = r0" shouldNot typeCheck

        val r1 = r0.tag[MyType]
        r1 shouldStaticallyBe a[Tag[MyType]]
        val t1: Tag[MyType] = r1

        val r2 = r1.tag[AnotherType]
        r2 shouldStaticallyBe a[Tag[MyType]]
        r2 shouldStaticallyBe a[Tag[AnotherType]]
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
        "r0.firstName" shouldNot typeCheck

        val r1 = r0.tag[Person]
        r1.firstName shouldBe "tarao"

        val r2 = %(age = 3).tag[Person]
        "r2.firstName" shouldNot typeCheck
      }

      it("should preserve tags after concatenation") {
        trait MyType
        trait AnotherType

        val r1 = %(name = "tarao", age = 3).tag[MyType]

        val r2 = r1 ++ %(email = "tarao@example.com")
        r2 shouldStaticallyBe a[Tag[MyType]]
        val t2: Tag[MyType] = r2

        val r3 = r1 + (email = "tarao@example.com")
        r3 shouldStaticallyBe a[Tag[MyType]]
        val t3: Tag[MyType] = r3

        val r4 = r1.tag[AnotherType]

        val r5 = r4 ++ %(email = "tarao@example.com")
        r5 shouldStaticallyBe a[Tag[MyType]]
        r5 shouldStaticallyBe a[Tag[AnotherType]]
        val t4: Tag[MyType] = r5
        val t5: Tag[AnotherType] = r5

        val r6 = r4 + (email1 = "tarao@example.com") + (occupation = "engineer")
        r6 shouldStaticallyBe a[Tag[MyType]]
        r6 shouldStaticallyBe a[Tag[AnotherType]]
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
        r3 shouldStaticallyBe a[Tag[MyType]]
        r3 shouldStaticallyBe a[Tag[YourType]]
        val t3: Tag[MyType] = r3
        val t4: Tag[YourType] = r3

        val r4 = r1.tag[AnotherType]
        val r5 = r2.tag[YetAnotherType]

        val r6 = r4 ++ r5
        r6 shouldStaticallyBe a[Tag[MyType]]
        r6 shouldStaticallyBe a[Tag[AnotherType]]
        r6 shouldStaticallyBe a[Tag[YourType]]
        r6 shouldStaticallyBe a[Tag[YetAnotherType]]
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
        r2 shouldStaticallyBe a[Tag[MyType]]
        val t2: Tag[MyType] = r2

        val r3 = r1.as[% { val name: String } & Tag[MyType]]
        r3 shouldStaticallyBe a[Tag[MyType]]
        val t3: Tag[MyType] = r3

        val r4 = r1.tag[AnotherType]

        val r5 = r4.as
        r5 shouldStaticallyBe a[Tag[MyType]]
        r5 shouldStaticallyBe a[Tag[AnotherType]]
        val t4: Tag[MyType] = r5
        val t5: Tag[AnotherType] = r5

        val r6 = r4.as[% { val name: String } & Tag[MyType] & Tag[AnotherType]]
        r6 shouldStaticallyBe a[Tag[MyType]]
        r6 shouldStaticallyBe a[Tag[AnotherType]]
        val t6: Tag[MyType] = r6
        val t7: Tag[AnotherType] = r6
      }

      it("should not allow to give a tag by .as[]") {
        trait MyType

        val r1 = %(name = "tarao", age = 3)
        "r1.as[%{val name: String; val age: Int} & Tag[MyType]]" shouldNot typeCheck
      }
    }

    describe(".values") {
      it("should extract values of records") {
        val r1 = %(name = "tarao", age = 3)
        val t1 = r1.values
        t1 shouldStaticallyBe a[(String, Int)]
        t1._1 shouldBe "tarao"
        t1._2 shouldBe 3

        val r2: % { val age: Int } = r1
        val t2 = r2.values
        t2 shouldStaticallyBe a[Int *: EmptyTuple]
        t2._1 shouldBe 3
      }

      it("should preserve the order of static type of fields") {
        val r1 = %(name = "tarao", age = 3)
        val r2: % { val age: Int; val name: String } = r1
        val t2 = r2.values
        t2 shouldStaticallyBe a[(Int, String)]
        t2._1 shouldBe 3
        t2._2 shouldBe "tarao"
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
        "val r2 = r1.as[% { val name: String; val age: Int }]" shouldNot typeCheck
      }

      it("should reject unrelated types") {
        val r1 = %(name = "tarao", age = 3)
        "val r2 = r1.as[% { val name: String; val email: String }]" shouldNot typeCheck
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
        e shouldStaticallyBe an[Empty]

        case class Cell(value: Int)
        val r1 = %(value = 10)
        val c = r1.to[Cell]
        c shouldStaticallyBe a[Cell]
        c.value shouldBe 10

        case class Person(name: String, age: Int)
        val r2 = %(name = "tarao", age = 3)
        val p = r2.to[Person]
        p shouldStaticallyBe a[Person]
        p.name shouldBe "tarao"
        p.age shouldBe 3
      }

      it("should not convert a record to a different shape of Product") {
        case class Person(name: String, age: Int)
        val r = %(value = 10)
        "r.to[Person]" shouldNot typeCheck
      }

      it(
        "should not convert a record to a Product with incompatible field type",
      ) {
        case class Cell(value: Int)
        val r = %(value = "foo")
        "r.to[Cell]" shouldNot typeCheck
      }

      it("should convert a record to a Tuple") {
        val r1 = %(name = "tarao", age = 3)
        val t1 = r1.to[("name", String) *: ("age", Int) *: EmptyTuple]
        t1._1._1 shouldBe "name"
        t1._1._2 shouldBe "tarao"
        t1._2._1 shouldBe "age"
        t1._2._2 shouldBe 3

        val r2: % { val age: Int } = r1
        val t2 = r2.to[("age", Int) *: EmptyTuple]
        t2._1._1 shouldBe "age"
        t2._1._2 shouldBe 3

        val r3: % { val age: Int; val name: String } = r1
        val t3 = r3.to[("age", Int) *: ("name", String) *: EmptyTuple]
        t3._1._1 shouldBe "age"
        t3._1._2 shouldBe 3
        t3._2._1 shouldBe "name"
        t3._2._2 shouldBe "tarao"
      }
    }

    describe(".toTuple") {
      it("should convert records to tuples") {
        val r1 = %(name = "tarao", age = 3)
        val t1 = r1.toTuple
        t1 shouldStaticallyBe a[("name", String) *: ("age", Int) *: EmptyTuple]
        t1._1._1 shouldBe "name"
        t1._1._2 shouldBe "tarao"
        t1._2._1 shouldBe "age"
        t1._2._2 shouldBe 3

        val r2: % { val age: Int } = r1
        val t2 = r2.toTuple
        t2 shouldStaticallyBe a[("age", Int) *: EmptyTuple]
        t2._1._1 shouldBe "age"
        t2._1._2 shouldBe 3

        val r3: % { val age: Int; val name: String } = r1
        val t3 = r3.toTuple
        t3 shouldStaticallyBe a[("age", Int) *: ("name", String) *: EmptyTuple]
        t3._1._1 shouldBe "age"
        t3._1._2 shouldBe 3
        t3._2._1 shouldBe "name"
        t3._2._2 shouldBe "tarao"
      }
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

        val t2: ("foo", Int) *: Int *: EmptyTuple =
          ("foo", 1) *: 3 *: EmptyTuple
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

        val t2: ("foo", Int) *: Int *: EmptyTuple =
          ("foo", 1) *: 3 *: EmptyTuple
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

        "r1 == r2" shouldNot typeCheck
      }
    }

    describe(".hashCode()") {
      it("should return the same hash code for equal records") {
        val r1 = %(name = "tarao", age = 3)
        val r2 = %(name = "tarao", age = 3)
        val r3 = %(name = "ikura", age = 1)
        val r4 = %(name = "tarao", age = 3, email = "tarao@example.com")
        val r5: % { val name: String; val age: Int } = r4

        (r1.hashCode() == r1.hashCode()) shouldBe true
        (r1.hashCode() == r2.hashCode()) shouldBe true
        (r2.hashCode() == r1.hashCode()) shouldBe true
        (r1.hashCode() == r5.as.hashCode()) shouldBe true
        (r5.as.hashCode() == r1.hashCode()) shouldBe true
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

object RecordSpec {
  opaque type Name = String
  object Name {
    def apply(name: String): Name = name
  }
}

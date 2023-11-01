package com.github.tarao.record4s

class ArrayRecordSpec extends helper.UnitSpec {
  describe("Record") {
    describe("Construction") {
      it("can create an empty record") {
        val r = ArrayRecord()
      }

      it("can create records with multiple fields") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        r1.name shouldBe "tarao"
        r1.age shouldBe 3

        val r2 = ArrayRecord(("name", "tarao"), ("age", 3))
        r2.name shouldBe "tarao"
        r2.age shouldBe 3

        val r3 = ArrayRecord("name" -> "tarao", "age" -> 3)
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
      }

      it("can create records by adding fields") {
        val r1 = ArrayRecord(name = "tarao")
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
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"

        val r2 = r1 + (age = r1.age + 1, email = "tarao2@example.com")
        r2.name shouldBe "tarao"
        r2.age shouldBe 4
        r2.email shouldBe "tarao2@example.com"

        val r3 =
          r2 + (email = ArrayRecord(user = "tarao", domain = "example.com"))
        r3.name shouldBe "tarao"
        r3.age shouldBe 4
        r3.email.user shouldBe "tarao"
        r3.email.domain shouldBe "example.com"
      }

      it("should reject non-literal labels") {
        val label = "name"
        """ArrayRecord((label, "tarao"))""" shouldNot typeCheck

        """ArrayRecord(("age", 3), (label, "tarao"))""" shouldNot typeCheck

        """ArrayRecord("age" -> 3, label -> "tarao")""" shouldNot typeCheck
      }

      it("should not allow '$' in labels") {
        "ArrayRecord($value = 3)" shouldNot typeCheck
        "ArrayRecord(value$ = 3)" shouldNot typeCheck
        "ArrayRecord($minusfoobar = 3)" shouldNot typeCheck
        "ArrayRecord(foo$minusbar = 3)" shouldNot typeCheck
        "ArrayRecord(foobar$minus = 3)" shouldNot typeCheck

        case class Cell($value: Int)
        "Record.from(Cell(3))" shouldNot typeCheck
      }

      it("should allow other signs in labels") {
        val r = ArrayRecord(`foo-bar` = 3)
        r.`foo-bar` shouldBe 3
      }

      it("should reject non-vararg construction") {
        val args = Seq("name" -> "tarao")
        "ArrayRecord(args: _*)" shouldNot typeCheck
      }

      it("should reject accessing non-existing fields") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        "r1.email" shouldNot typeCheck
      }

      it("should not allow nothing except named construction by `apply`") {
        """ArrayRecord.foo(name = "tarao")""" shouldNot typeCheck

        """ArrayRecord("tarao")""" shouldNot typeCheck

        """ArrayRecord("tarao", age = 3)""" shouldNot typeCheck
      }
    }

    describe("Type of ArrayRecord") {
      it("should be a simple type for an empty record") {
        val r = ArrayRecord.empty
        helper.showTypeOf(r) shouldBe "ArrayRecord[%]"
      }

      it("should be refinement type") {
        val r = ArrayRecord(name = "tarao")
        helper.showTypeOf(r) shouldBe """ArrayRecord[% {
                                        |  val name: String
                                        |}]""".stripMargin
      }

      it("should be combined refinement type of added fields") {
        val r1 = ArrayRecord(name = "tarao")
        val r2 = r1 + (age = 3) + (email = "tarao@example.com")
        helper.showTypeOf(r2) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}]""".stripMargin
      }

      it("should be combined refinement type of concatenated records") {
        val r1 = ArrayRecord(name = "tarao")
        val r2 = ArrayRecord(age = 3, email = "tarao@example.com")
        helper.showTypeOf(r1 ++ r2) shouldBe """ArrayRecord[% {
                                               |  val name: String
                                               |  val age: Int
                                               |  val email: String
                                               |}]""".stripMargin
        val r3 = r1 ++ r2
        helper.showTypeOf(r3) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}]""".stripMargin
      }

      it("should take the last field type of the same name") {
        val r1 = ArrayRecord(name = "tarao")
        val r2 = r1 + (age  = 3) + (email = "tarao@example.com")
        val r3 = r2 + (name = "ikura") + (email =
          ArrayRecord(user = "ikura", domain = "example.com"),
        )
        helper.showTypeOf(r3) shouldBe """ArrayRecord[% {
                                         |  val age: Int
                                         |  val name: String
                                         |  val email: ArrayRecord[% {
                                         |    val user: String
                                         |    val domain: String
                                         |  }]
                                         |}]""".stripMargin

        val r4 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r5 = ArrayRecord(name = "ikura") + (email =
          ArrayRecord(user = "ikura", domain = "example.com"),
        )
        helper.showTypeOf(r4 ++ r5) shouldBe """ArrayRecord[% {
                                               |  val age: Int
                                               |  val name: String
                                               |  val email: ArrayRecord[% {
                                               |    val user: String
                                               |    val domain: String
                                               |  }]
                                               |}]""".stripMargin
      }

      it("should have flattened field types after ++ or +") {
        val r1 = ArrayRecord(name = "tarao")
        val r2 = ArrayRecord(age = 3)
        val r3 = ArrayRecord(email = "tarao@example.com")
        helper.showTypeOf((r1 ++ r2) ++ r3) shouldBe """ArrayRecord[% {
                                                       |  val name: String
                                                       |  val age: Int
                                                       |  val email: String
                                                       |}]""".stripMargin
        val r4 = (r1 ++ r2) + (email = "tarao@example.com")
        helper.showTypeOf(r4) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |  val age: Int
                                         |  val email: String
                                         |}]""".stripMargin
      }
    }

    describe("As a Product") {
      it("should be a Product") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        r1 shouldBe a[Product]
        r1.productArity shouldBe 2
        r1.productElement(0) shouldBe "tarao"
        r1.productElement(1) shouldBe 3
        r1.productElementName(0) shouldBe "name"
        r1.productElementName(1) shouldBe "age"
      }

      it("can be converted from a non-array record") {
        val r1 = ArrayRecord.from(%(name = "tarao", age = 3))
        r1 shouldBe an[ArrayRecord[% { val name: String; val age: Int }]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
      }

      it("can be mirrored") {
        import scala.deriving.Mirror

        type ElemLabels = "name" *: "age" *: EmptyTuple
        type ElemTypes = String *: Int *: EmptyTuple
        type PersonFields = % {
          val name: String
          val age: Int
        }
        type PersonRecord = ArrayRecord[PersonFields]

        case class Person(name: String, age: Int)
        case class NonPerson(name: String)

        val m1 = summon[Mirror.ProductOf[PersonRecord]]
        summon[m1.MirroredMonoType =:= PersonRecord]
        summon[m1.MirroredType =:= PersonRecord]
        summon[m1.MirroredElemTypes =:= (String, Int)]
        summon[m1.MirroredElemLabels =:= ("name", "age")]

        val p1 = m1.fromProduct(("tarao", 3))
        p1 shouldBe an[ArrayRecord[PersonFields]]
        p1 shouldBe a[Product]
        p1.productElement(0) shouldBe "tarao"
        p1.productElement(1) shouldBe 3

        val p2 = m1.fromProduct(Person("tarao", 3))
        p2 shouldBe an[ArrayRecord[PersonFields]]
        p2 shouldBe a[Product]
        p2.productElement(0) shouldBe "tarao"
        p2.productElement(1) shouldBe 3

        val p3 = m1.fromProductTyped(("tarao", 3))
        p3 shouldBe an[ArrayRecord[PersonFields]]
        p3 shouldBe a[Product]
        p3.productElement(0) shouldBe "tarao"
        p3.productElement(1) shouldBe 3

        val p4 = m1.fromProductTyped(Person("tarao", 3))
        p4 shouldBe an[ArrayRecord[PersonFields]]
        p4 shouldBe a[Product]
        p4.productElement(0) shouldBe "tarao"
        p4.productElement(1) shouldBe 3

        """m1.fromProductTyped((3, "tarao"))""" shouldNot typeCheck
        """m1.fromProductTyped(NonPerson("tarao"))""" shouldNot typeCheck

        val m2 = summon[Mirror.ProductOf[ArrayRecord[PersonFields & Tag[Person]]]]
        summon[m2.MirroredMonoType =:= (ArrayRecord[PersonFields & Tag[Person]])]
        summon[m2.MirroredType =:= (ArrayRecord[PersonFields & Tag[Person]])]
        summon[m2.MirroredElemTypes =:= (String, Int)]
        summon[m2.MirroredElemLabels =:= ("name", "age")]

        """m2.fromProductTyped((3, "tarao"))""" shouldNot typeCheck
        """m2.fromProductTyped(NonPerson("tarao"))""" shouldNot typeCheck

        val p5 = m2.fromProduct(("tarao", 3))
        p5 shouldBe an[ArrayRecord[PersonFields & Tag[Person]]]
        p5 shouldBe a[Product]
        p5.productElement(0) shouldBe "tarao"
        p5.productElement(1) shouldBe 3

        val p6 = m2.fromProduct(Person("tarao", 3))
        p6 shouldBe an[ArrayRecord[PersonFields & Tag[Person]]]
        p6 shouldBe a[Product]
        p6.productElement(0) shouldBe "tarao"
        p6.productElement(1) shouldBe 3

        val p7 = m2.fromProductTyped(("tarao", 3))
        p7 shouldBe an[ArrayRecord[PersonFields & Tag[Person]]]
        p7 shouldBe a[Product]
        p7.productElement(0) shouldBe "tarao"
        p7.productElement(1) shouldBe 3

        val p8 = m2.fromProductTyped(Person("tarao", 3))
        p8 shouldBe an[ArrayRecord[PersonFields & Tag[Person]]]
        p8 shouldBe a[Product]
        p8.productElement(0) shouldBe "tarao"
        p8.productElement(1) shouldBe 3
      }
    }

    describe("ArrayRecord.lookup") {
      it("should return a value by a string key name") {
        val r = ArrayRecord(name = "tarao", age = 3)
        ArrayRecord.lookup(r, "name") shouldBe a[String]
        ArrayRecord.lookup(r, "name") shouldBe "tarao"
        ArrayRecord.lookup(r, "age") shouldBe an[Int]
        ArrayRecord.lookup(r, "age") shouldBe 3
      }

      it("should reject non-literal key names") {
        val r = ArrayRecord(name = "tarao", age = 3)
        val key = "name"
        "ArrayRecord.lookup(r, key)" shouldNot typeCheck
      }

      it("should reject statically key names not in the record") {
        val r = ArrayRecord(name = "tarao", age = 3)
        """ArrayRecord.lookup(r, "email")""" shouldNot typeCheck
      }

      it("should allow shadowed field to be extracted") {
        val r = ArrayRecord(toString = 10)
        r.toString shouldBe a[String]
        r.toString shouldBe "ArrayRecord(toString = 10)"
        ArrayRecord.lookup(r, "toString") shouldBe an[Int]
        ArrayRecord.lookup(r, "toString") shouldBe 10
      }
    }

    describe("++") {
      it("should allow concatenation of two records") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 = ArrayRecord(email = "tarao@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "tarao"
        r3.age shouldBe 3
        r3.email shouldBe "tarao@example.com"

        val r4 =
          ArrayRecord(email = "tarao@example.com", occupation = "engineer")
        val r5 = r1 ++ r4
        r5.name shouldBe "tarao"
        r5.age shouldBe 3
        r5.email shouldBe "tarao@example.com"
        r5.occupation shouldBe "engineer"
      }

      it("should take the last value for duplicated fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r2 = ArrayRecord(name = "ikura", email = "ikura@example.com")
        val r3 = r1 ++ r2
        r3.name shouldBe "ikura"
        r3.age shouldBe 3
        r3.email shouldBe "ikura@example.com"
      }

      it("should allow to concatenate non-array records") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r2 = %(name = "ikura", email = "ikura@example.com")
        val r3 = r1 ++ r2
        helper.showTypeOf(r3) shouldBe """ArrayRecord[% {
                                         |  val age: Int
                                         |  val name: String
                                         |  val email: String
                                         |}]""".stripMargin
        r3.name shouldBe "ikura"
        r3.age shouldBe 3
        r3.email shouldBe "ikura@example.com"
      }

      it("should not take type-hidden value") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
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
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name.age)
        r2.name shouldBe "tarao"
        r2.age shouldBe 3
        "r2.email" shouldNot typeCheck

        val r3 = r1(select)
        r3.toString shouldBe "ArrayRecord()"
      }

      it("should preserve the order of selection") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name.age)
        helper.showTypeOf(r2) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |  val age: Int
                                         |}]""".stripMargin

        val r3 = r1(select.age.name)
        helper.showTypeOf(r3) shouldBe """ArrayRecord[% {
                                         |  val age: Int
                                         |  val name: String
                                         |}]""".stripMargin
      }

      it("should allow to rename fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name("nickname").age)
        r2.nickname shouldBe "tarao"
        r2.age shouldBe 3
        "r2.name" shouldNot typeCheck

        val r3 = r1(select.name(rename = "nickname").age.name)
        r3.nickname shouldBe "tarao"
        r3.age shouldBe 3
        r3.name shouldBe "tarao"
        helper.showTypeOf(r3) shouldBe """ArrayRecord[% {
                                         |  val nickname: String
                                         |  val age: Int
                                         |  val name: String
                                         |}]""".stripMargin
      }

      it("should reject giving a new name by non-literal string") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val newName = "nickname"
        "r1(select.name(newName).age)" shouldNot typeCheck
      }

      it("should reject selecting missing fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        "r1(select.nickname)" shouldNot typeCheck
      }

      it("should take the last one for duplicated fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(select.name("value").age("value"))
        r2.value shouldBe 3
        "r2.name" shouldNot typeCheck
        "r2.age" shouldNot typeCheck
      }
    }

    describe(".apply(unselect)") {
      it("should create a new record without unselected fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

        val r2 = r1(unselect.email)
        r2.name shouldBe "tarao"
        r2.age shouldBe 3
        "r2.email" shouldNot typeCheck

        val r3 = r1(unselect)
        r3 shouldBe r1
      }

      it("should do nothing for missing fields") {
        val r1 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")

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

        val r0 = ArrayRecord(name = "tarao", age = 3)
        "val t0: Tag[MyType] = r0" shouldNot typeCheck

        val r1 = r0.tag[MyType]
        r1 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType],
        ]]

        val r2 = r1.tag[AnotherType]
        r2 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType] & Tag[AnotherType],
        ]]
      }

      it("should be a target of extension method defined in a tagged type") {
        trait Person
        object Person {
          extension (p: ArrayRecord[% { val name: String } & Tag[Person]]) {
            def firstName: String = p.name.split(" ").head
          }
        }

        val r0 = ArrayRecord(name = "tarao fuguta")
        "r0.firstName" shouldNot typeCheck

        val r1 = r0.tag[Person]
        r1.firstName shouldBe "tarao"

        val r2 = ArrayRecord(age = 3).tag[Person]
        "r2.firstName" shouldNot typeCheck
      }

      it("should preserve tags after concatenation") {
        trait MyType
        trait AnotherType

        val r1 = ArrayRecord(name = "tarao", age = 3).tag[MyType]

        val r2 = r1 ++ ArrayRecord(email = "tarao@example.com")
        r2 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int; val email: String } & Tag[MyType],
        ]]

        val r3 = r1 + (email = "tarao@example.com")
        r3 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int; val email: String } & Tag[MyType],
        ]]

        val r4 = r1.tag[AnotherType]

        val r5 = r4 ++ ArrayRecord(email = "tarao@example.com")
        r5 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int; val email: String } &
            Tag[MyType] & Tag[AnotherType],
        ]]

        val r6 = r4 + (email1 = "tarao@example.com") + (occupation = "engineer")
        r6 shouldBe a[Tag[MyType]]
        r6 shouldBe a[Tag[AnotherType]]
        r6 shouldBe an[ArrayRecord[
          % {
            val name: String; val age: Int; val email: String;
            val occupation: String
          } & Tag[MyType] & Tag[AnotherType],
        ]]
      }

      it("should join tags from multiple records") {
        trait MyType
        trait YourType
        trait AnotherType
        trait YetAnotherType

        val r1 = ArrayRecord(name = "tarao").tag[MyType]
        val r2 = ArrayRecord(age = 3).tag[YourType]

        val r3 = r1 ++ r2
        r3 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType] & Tag[YourType],
        ]]

        val r4 = r1.tag[AnotherType]
        val r5 = r2.tag[YetAnotherType]

        val r6 = r4 ++ r5
        r6 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType] &
            Tag[AnotherType] & Tag[YourType] & Tag[YetAnotherType],
        ]]
      }

      it("should preserve tags after upcast") {
        trait MyType
        trait AnotherType

        val r1 = ArrayRecord(name = "tarao", age = 3).tag[MyType]

        val r2 = r1.upcast
        r2 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType],
        ]]

        val r3 = r1.upcast[% { val name: String } & Tag[MyType]]
        r3 shouldBe an[ArrayRecord[% { val name: String } & Tag[MyType]]]

        val r4 = r1.tag[AnotherType]

        val r5 = r4.upcast
        r5 shouldBe an[ArrayRecord[
          % { val name: String; val age: Int } & Tag[MyType] & Tag[AnotherType],
        ]]

        val r6 =
          r4.upcast[% { val name: String } & Tag[MyType] & Tag[AnotherType]]
        r6 shouldBe an[ArrayRecord[
          % { val name: String } & Tag[MyType] & Tag[AnotherType],
        ]]
      }

      it("should not allow to give a tag by .upcast[]") {
        trait MyType

        val r1 = ArrayRecord(name = "tarao", age = 3)
        "r1.upcast[% {val name: String; val age: Int} & Tag[MyType]]" shouldNot typeCheck
      }
    }

    describe(".values") {
      it("should extract values of records") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val t1 = r1.values
        t1 shouldBe a[(String, Int)]
        t1._1 shouldBe "tarao"
        t1._2 shouldBe 3

        val r2 = r1.upcast[% { val age: Int }]
        val t2 = r2.values
        t2 shouldBe a[Int *: EmptyTuple]
        t2._1 shouldBe 3
      }

      it("should preserve the order of static type of fields") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 = r1.upcast[% { val age: Int; val name: String }]
        val t2 = r2.values
        t2 shouldBe a[(Int, String)]
        t2._1 shouldBe 3
        t2._2 shouldBe "tarao"
      }
    }

    describe(".upcast[]") {
      it("should return the same type if no type is specified") {
        val r = ArrayRecord(name = "tarao", age = 3)
        helper.showTypeOf(r.upcast) shouldBe """ArrayRecord[% {
                                               |  val name: String
                                               |  val age: Int
                                               |}]""".stripMargin
      }

      it("should allow upcast") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 = r1.upcast[% { val name: String }]
        helper.showTypeOf(r2) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |}]""".stripMargin
      }

      it("should reject downcast") {
        val r1 = ArrayRecord(name = "tarao")
        "val r2 = r1.upcast[% { val name: String; val age: Int }]" shouldNot typeCheck
      }

      it("should reject unrelated types") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        "val r2 = r1.upcast[% { val name: String; val email: String }]" shouldNot typeCheck
      }

      it("should strip away statically invisible fields") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 = r1.upcast[% { val name: String }]

        r2.toString() shouldBe "ArrayRecord(name = tarao)"
      }
    }

    describe(".to[]") {
      it("should convert a record to a Product") {
        case class Empty()
        val r0 = ArrayRecord()
        val e = r0.to[Empty]
        e shouldBe an[Empty]

        case class Cell(value: Int)
        val r1 = ArrayRecord(value = 10)
        val c = r1.to[Cell]
        c shouldBe a[Cell]
        c.value shouldBe 10

        case class Person(name: String, age: Int)
        val r2 = ArrayRecord(name = "tarao", age = 3)
        val p = r2.to[Person]
        p shouldBe a[Person]
        p.name shouldBe "tarao"
        p.age shouldBe 3
      }

      it("should not convert a record to a different shape of Product") {
        case class Person(name: String, age: Int)
        val r1 = ArrayRecord(value = 10)
        "r1.to[Person]" shouldNot typeCheck

        val r2 = ArrayRecord(age = 3, name = "tarao")
        "r2.to[Person]" shouldNot typeCheck
      }

      it(
        "should not convert a record to a Product with incompatible field type",
      ) {
        case class Cell(value: Int)
        val r = ArrayRecord(value = "foo")
        "r.to[Cell]" shouldNot typeCheck
      }

      it("should convert to a Product of different label names") {
        case class KeyValue(key: String, value: Int)
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val kv = r1.to[KeyValue]
        kv shouldBe a[KeyValue]
        kv.key shouldBe "tarao"
        kv.value shouldBe 3
      }
    }

    describe(".toTuple") {
      it("should convert records to tuples") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val t1 = r1.toTuple
        t1 shouldBe a[("name", String) *: ("age", Int) *: EmptyTuple]
        t1._1._1 shouldBe "name"
        t1._1._2 shouldBe "tarao"
        t1._2._1 shouldBe "age"
        t1._2._2 shouldBe 3

        val r2 = r1.upcast[% { val age: Int }]
        val t2 = r2.toTuple
        t2 shouldBe a[("age", Int) *: EmptyTuple]
        t2._1._1 shouldBe "age"
        t2._1._2 shouldBe 3

        val r3 = r1.upcast[% { val age: Int; val name: String }]
        val t3 = r3.toTuple
        t3 shouldBe a[("age", Int) *: ("name", String) *: EmptyTuple]
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
        val r1 = ArrayRecord.from(p)

        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.toString() shouldBe "ArrayRecord(name = tarao, age = 3)"
        helper.showTypeOf(r1) shouldBe """ArrayRecord[% {
                                         |  val name: String
                                         |  val age: Int
                                         |}]""".stripMargin

        val t1: ("foo", Int) *: EmptyTuple = ("foo", 1) *: EmptyTuple
        val r2 = ArrayRecord.from(t1)
        r2.foo shouldBe 1
        r2.toString() shouldBe "ArrayRecord(foo = 1)"
        helper.showTypeOf(r2) shouldBe """ArrayRecord[% {
                                         |  val foo: Int
                                         |}]""".stripMargin

        val t2: ("foo", Int) *: Int *: EmptyTuple =
          ("foo", 1) *: 3 *: EmptyTuple
        val r3 = ArrayRecord.from(t2)
        r3.foo shouldBe 1
        r3.toString() shouldBe "ArrayRecord(foo = 1)"
        helper.showTypeOf(r2) shouldBe """ArrayRecord[% {
                                         |  val foo: Int
                                         |}]""".stripMargin
      }

      it("should allow Products to be concatenated to a record") {
        val r1 = ArrayRecord(email = "tarao@example.com")

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
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 = ArrayRecord(name = "tarao", age = 3)
        val r3 = ArrayRecord(name = "ikura", age = 1)

        (r1 == r1) shouldBe true
        (r1 == r2) shouldBe true
        (r2 == r1) shouldBe true
        (r1 == r3) shouldBe false
        (r3 == r1) shouldBe false
      }

      it("should statically reject equality test of different types") {
        val r1 = ArrayRecord(name = "tarao", age = 3)
        val r2 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        case class Person(name: String, age: Int)
        val r3 = Person("tarao", 3)

        "r1 == r2" shouldNot typeCheck
        "r2 == r1" shouldNot typeCheck
        "r1 == r3" shouldNot typeCheck
        "r3 == r1" shouldNot typeCheck
      }
    }

    describe(".toString()") {
      it("can express empty Record") {
        ArrayRecord.empty.toString() shouldBe "ArrayRecord()"
      }

      it("can express Record with a field") {
        val r = ArrayRecord(foo = 42)
        r.toString() shouldBe "ArrayRecord(foo = 42)"
      }

      it("can express Record with fields") {
        val r = ArrayRecord(foo = 42, bar = "yay!")
        r.toString() shouldBe "ArrayRecord(foo = 42, bar = yay!)"
      }

      it("can express nested Record") {
        val r0 = ArrayRecord(foo = 42, bar = "yay!")
        val rr1 = ArrayRecord(buzz = true)
        val r1 = r0 + (child = rr1)

        r1.toString shouldBe "ArrayRecord(foo = 42, bar = yay!, child = ArrayRecord(buzz = true))"
      }
    }
  }
}

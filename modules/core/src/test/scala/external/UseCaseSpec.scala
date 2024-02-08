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

package external

class UseCaseSpec extends helper.UnitSpec {
  describe("Generic record lookup") {
    import com.github.tarao.record4s.{%, Record}
    import com.github.tarao.record4s.typing.syntax.{:=, in}

    it("can be done by using typing.syntax") {
      def getEmail[R <: %, V](record: R)(using
        V := ("email" in R),
      ): V = Record.lookup(record, "email")

      val r0 = %(name = "tarao", age = 3)
      val r1 = r0 + (email = "tarao@example.com")
      getEmail(r1) shouldBe "tarao@example.com"
      "getEmail(r0)" shouldNot typeCheck
    }
  }

  describe("Generic record extension with ++") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.syntax.{++, :=}

    it("can be done by using typing.syntax") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        RR := R ++ % { val email: String },
      ): RR = record ++ %(email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can replace existing field") {
      def addEmail[R <: %, T, RR <: %](record: R, email: T)(using
        RR := R ++ % { val email: T },
      ): RR = record ++ %(email = email)

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com")
      val r1 = addEmail(r0, %(user = "tarao", domain = "example.com"))
      r1 shouldStaticallyBe a[
        % {
          val name: String; val age: Int;
          val email: % { val user: String; val domain: String }
        },
      ]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email.user shouldBe "tarao"
      r1.email.domain shouldBe "example.com"
    }

    it("preserves a tag") {
      trait Person
      object Person {
        extension [R <: % { val name: String }](p: R & Tag[Person]) {
          def firstName: String = p.name.split(" ").head

          def withEmail[RR <: %](email: String)(using
            RR := (R & Tag[Person]) ++ % { val email: String },
          ): RR = p ++ %(email = email)
        }
      }

      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        RR := R ++ % { val email: String },
      ): RR = record ++ %(email = email)

      val r0 = %(name = "tarao fuguta", age = 3).tag[Person]
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r2.name shouldBe "tarao fuguta"
      r2.firstName shouldBe "tarao"
      r2.age shouldBe 3
      r2.email shouldBe "tarao@example.com"
    }

    it("should reject generic record concatenation without using ++ type") {
      """
      def addEmail[R <: %](record: R, email: String): Any =
        record ++ %(email = email)
      """ shouldNot typeCheck
    }
  }

  describe("Generic record extension with +") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.syntax.{+, ++, :=}

    it("can be done by using typing.syntax") {
      locally {
        def addEmail[R <: %, RR <: %](record: R, email: String)(using
          RR := R ++ % { val email: String },
        ): RR = record + (email = email)

        val r0 = %(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[
          % { val name: String; val age: Int; val email: String },
        ]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }

      locally {
        def addEmail[R <: %, RR <: %](record: R, email: String)(using
          RR := R ++ ("email", String) *: EmptyTuple,
        ): RR = record + (email = email)

        val r0 = %(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[
          % { val name: String; val age: Int; val email: String },
        ]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }

      locally {
        def addEmail[R <: %, RR <: %](record: R, email: String)(using
          RR := R + ("email", String),
        ): RR = record + (email = email)

        val r0 = %(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[
          % { val name: String; val age: Int; val email: String },
        ]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }
    }

    it("can replace existing field") {
      def addEmail[R <: %, T, RR <: %](record: R, email: T)(using
        RR := R ++ % { val email: T },
      ): RR = record + (email = email)

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com")
      val r1 = addEmail(r0, %(user = "tarao", domain = "example.com"))
      r1 shouldStaticallyBe a[
        % {
          val name: String; val age: Int;
          val email: % { val user: String; val domain: String }
        },
      ]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email.user shouldBe "tarao"
      r1.email.domain shouldBe "example.com"
    }

    it("preserves a tag") {
      trait Person
      object Person {
        extension [R <: % { val name: String }](p: R & Tag[Person]) {
          def firstName: String = p.name.split(" ").head

          def withEmail[RR <: %](email: String)(using
            RR := (R & Tag[Person]) ++ % { val email: String },
          ): RR = p + (email = email)
        }
      }

      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        RR := R ++ % { val email: String },
      ): RR = record + (email = email)

      val r0 = %(name = "tarao fuguta", age = 3).tag[Person]
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r2.name shouldBe "tarao fuguta"
      r2.firstName shouldBe "tarao"
      r2.age shouldBe 3
      r2.email shouldBe "tarao@example.com"
    }

    it("should reject generic record extension without using ++ type") {
      """
      def addEmail[R <: %](record: R, email: String): Any =
        record + (email = email)
      """ shouldNot typeCheck
    }
  }

  describe("Generic record upcast") {
    import com.github.tarao.record4s.{%, RecordLike, Tag}
    import com.github.tarao.record4s.typing.syntax.{-, --, :=}

    it("can be done by using --") {
      def withoutAge[R <: %, RR <: %](record: R)(using
        RR := R -- "age" *: EmptyTuple,
        R <:< RR,
      ): RR = record

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com")
      val r1 = withoutAge(r0)
      r1.name shouldBe "tarao"
      r1.email shouldBe "tarao@example.com"
      "r1.age" shouldNot typeCheck
    }

    it("can be done by using -") {
      def withoutAge[R <: %, RR <: %](record: R)(using
        RR := R - "age",
        R <:< RR,
      ): RR = record

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com")
      val r1 = withoutAge(r0)
      r1.name shouldBe "tarao"
      r1.email shouldBe "tarao@example.com"
      "r1.age" shouldNot typeCheck
    }

    it("preserves a tag") {
      def withoutAge[R <: %, RR <: %](record: R)(using
        RR := R -- "age" *: EmptyTuple,
        R <:< RR,
      ): RR = record

      trait Person

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com").tag[Person]
      val r1 = withoutAge(r0)
      r1.name shouldBe "tarao"
      r1.email shouldBe "tarao@example.com"
      "r1.age" shouldNot typeCheck
      r1 shouldStaticallyBe a[Tag[Person]]
    }
  }

  describe("Generic array record lookup") {
    import com.github.tarao.record4s.ArrayRecord
    import com.github.tarao.record4s.typing.syntax.{:=, by, in}

    it("can be done by using Lookup") {
      inline def getEmail[R, V, I <: Int](record: ArrayRecord[R])(using
        V := ("email" in R) by I,
      ): V = ArrayRecord.lookup(record, "email")

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = r0 + (email = "tarao@example.com")
      getEmail(r1) shouldBe "tarao@example.com"
      "getEmail(r0)" shouldNot typeCheck
    }
  }

  describe("Generic array record extension with ++") {
    import com.github.tarao.record4s.{ArrayRecord, ProductRecord, Tag}
    import com.github.tarao.record4s.typing.syntax.{++, :=}

    it("can be done by using typing.syntax") {
      def addEmail[R <: Tuple, RR <: ProductRecord](
        record: ArrayRecord[R],
        email: String,
      )(using RR := R ++ ArrayRecord[("email", String) *: EmptyTuple]): RR =
        record ++ ArrayRecord(email = email)

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)),
      ]]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can replace existing field") {
      locally {
        def addEmail[R <: Tuple, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: T,
        )(using RR := R ++ ArrayRecord[("email", T) *: EmptyTuple]): RR =
          record ++ ArrayRecord(email = email)

        val r0 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r1 =
          addEmail(r0, ArrayRecord(user = "tarao", domain = "example.com"))
        r1 shouldStaticallyBe an[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("user", String), ("domain", String))]),
          ),
        ]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email.user shouldBe "tarao"
        r1.email.domain shouldBe "example.com"
      }

      locally {
        def rename[R <: Tuple, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          value: T,
        )(using
          RR := R ++ ArrayRecord[("name", T) *: EmptyTuple],
        ): RR = record ++ ArrayRecord(name = value)

        val r0 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r1 = rename(r0, "ikura")
        r1 shouldStaticallyBe an[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", String),
          ),
        ]]
        r1.name shouldBe "ikura"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }
    }

    it("preserves a tag") {
      trait Person
      type PersonRecord[T <: Tuple] =
        ArrayRecord[(("name", String) *: T) & Tag[Person]]

      object Person {
        extension [T <: Tuple](p: PersonRecord[T]) {
          def firstName: String = p.name.split(" ").head

          def withEmail[RR <: ProductRecord](email: String)(using
            RR := ((("name", String) *: T) & Tag[Person]) ++
              ArrayRecord[("email", String) *: EmptyTuple],
          ): RR = p ++ ArrayRecord(email = email)
        }
      }

      def addEmail[R <: Tuple, T, RR <: ProductRecord](
        record: ArrayRecord[R],
        email: T,
      )(using
        RR := R ++ ArrayRecord[("email", T) *: EmptyTuple],
      ): RR = record ++ ArrayRecord(email = email)

      val r0 = ArrayRecord(name = "tarao fuguta", age = 3).tag[Person]
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)) & Tag[Person],
      ]]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)) & Tag[Person],
      ]]
      r2.name shouldBe "tarao fuguta"
      r2.firstName shouldBe "tarao"
      r2.age shouldBe 3
      r2.email shouldBe "tarao@example.com"
    }

    it("should reject concatenation without concrete field types") {
      """
        def addEmail[R](record: ArrayRecord[R], email: String): Any =
          record ++ ArrayRecord(email = email)
      """ shouldNot typeCheck
    }
  }

  describe("Generic array record extension with +") {
    import com.github.tarao.record4s.{%, ArrayRecord, ProductRecord, Tag}
    import com.github.tarao.record4s.typing.syntax.{+, ++, :=}

    it("can be done by using typing.syntax") {
      locally {
        def addEmail[R <: Tuple, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: String,
        )(using RR := R ++ ("email", String) *: EmptyTuple): RR =
          record + (email = email)

        val r0 = ArrayRecord(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[ArrayRecord[
          (("name", String), ("age", Int), ("email", String)),
        ]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }

      locally {
        def addEmail[R <: Tuple, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: String,
        )(using RR := R ++ % { val email: String }): RR =
          record + (email = email)

        val r0 = ArrayRecord(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[ArrayRecord[
          (("name", String), ("age", Int), ("email", String)),
        ]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }

      locally {
        def addEmail[R <: Tuple, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: String,
        )(using RR := R + ("email", String)): RR =
          record + (email = email)

        val r0 = ArrayRecord(name = "tarao", age = 3)
        val r1 = addEmail(r0, "tarao@example.com")
        r1 shouldStaticallyBe a[ArrayRecord[
          (("name", String), ("age", Int), ("email", String)),
        ]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }
    }

    it("can replace existing field") {
      locally {
        def addEmail[R <: Tuple, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: T,
        )(using RR := R ++ ("email", T) *: EmptyTuple): RR =
          record + (email = email)

        val r0 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r1 =
          addEmail(r0, ArrayRecord(user = "tarao", domain = "example.com"))
        r1 shouldStaticallyBe a[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", ArrayRecord[(("user", String), ("domain", String))]),
          ),
        ]]
        r1.name shouldBe "tarao"
        r1.age shouldBe 3
        r1.email.user shouldBe "tarao"
        r1.email.domain shouldBe "example.com"
      }

      locally {
        def rename[R <: Tuple, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          value: T,
        )(using
          RR := R ++ ("name", T) *: EmptyTuple,
        ): RR = record + (name = value)

        val r0 =
          ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
        val r1 = rename(r0, "ikura")
        r1 shouldStaticallyBe an[ArrayRecord[
          (
            ("name", String),
            ("age", Int),
            ("email", String),
          ),
        ]]
        r1.name shouldBe "ikura"
        r1.age shouldBe 3
        r1.email shouldBe "tarao@example.com"
      }
    }

    it("preserves a tag") {
      trait Person
      type PersonRecord[T <: Tuple] =
        ArrayRecord[(("name", String) *: T) & Tag[Person]]

      object Person {
        extension [T <: Tuple](p: PersonRecord[T]) {
          def firstName: String = p.name.split(" ").head

          def withEmail[RR <: ProductRecord](email: String)(using
            RR := ((("name", String) *: T) & Tag[Person]) ++
              ("email", String) *: EmptyTuple,
          ): RR = p + (email = email)
        }
      }

      def addEmail[R <: Tuple, T, RR <: ProductRecord](
        record: ArrayRecord[R],
        email: T,
      )(using
        RR := R ++ ("email", T) *: EmptyTuple,
      ): RR = record + (email = email)

      val r0 = ArrayRecord(name = "tarao fuguta", age = 3).tag[Person]
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)) & Tag[Person],
      ]]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)) & Tag[Person],
      ]]
      r2.name shouldBe "tarao fuguta"
      r2.firstName shouldBe "tarao"
      r2.age shouldBe 3
      r2.email shouldBe "tarao@example.com"
    }

    it("should reject concatenation without concrete field types") {
      """
        def addEmail[R](record: ArrayRecord[R], email: String): Any =
          record + (email = email)
      """ shouldNot typeCheck
    }
  }

  describe("Pattern matching") {
    import com.github.tarao.record4s.{%, ArrayRecord, select}

    it("should be possible to match records by `select` and `values`") {
      val r = %(name = "tarao", age = 3, email = "tarao@example.com")

      r match {
        case select.name.age(name, age) =>
          name shouldBe "tarao"
          age shouldBe 3
      }

      val pattern = "(.*)@(.*)".r
      r match {
        case select.name.email(name, pattern(user, domain)) =>
          name shouldBe "tarao"
          user shouldBe "tarao"
          domain shouldBe "example.com"
        case _ =>
          fail()
      }

      val ar = ArrayRecord(name = "tarao", age = 3, email = "tarao@example.com")
      r match {
        case select.age.name(age, name) =>
          name shouldBe "tarao"
          age shouldBe 3
      }

      case class Person(name: String, age: Int)
      val p = Person("tarao", 3)
      p match {
        case select.age(age) =>
          age shouldBe 3
        case _ =>
          fail()
      }
    }
  }
}

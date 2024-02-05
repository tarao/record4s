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
    import com.github.tarao.record4s.typing.Record.Lookup

    it("can be done by using Lookup") {
      def getEmail[R <: %](record: R)(using
        hasEmail: Lookup[R, "email"],
      ): hasEmail.Out = Record.lookup(record, "email")

      val r0 = %(name = "tarao", age = 3)
      val r1 = r0 + (email = "tarao@example.com")
      getEmail(r1) shouldBe "tarao@example.com"
      "getEmail(r0)" shouldNot typeCheck
    }
  }

  describe("Generic record extension with ++") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.Record.Concat

    it("can be done by using Concat") {
      def addEmail[R <: %](record: R, email: String)(using
        concat: Concat[R, % { val email: String }],
      ): concat.Out = record ++ %(email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can be done by using Concat.Aux") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Concat.Aux[R, % { val email: String }, RR],
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
        Concat.Aux[R, % { val email: T }, RR],
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
            Concat.Aux[R & Tag[Person], % { val email: String }, RR],
          ): RR = p ++ %(email = email)
        }
      }

      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Concat.Aux[R, % { val email: String }, RR],
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

    it("should lose record type information without using Concat") {
      def addEmail[R <: %](record: R, email: String) =
        record ++ %(email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      "r1.name" shouldNot typeCheck
      "r1.age" shouldNot typeCheck
      r1.email shouldBe "tarao@example.com"
      r1 shouldStaticallyBe a[% { val email: String }]

      def addEmail2[R <: % { val name: String }](record: R, email: String) =
        record ++ %(email = email)
      val r2 = addEmail(r0, "tarao@example.com")
      "r2.name" shouldNot typeCheck
      "r2.age" shouldNot typeCheck
      r2.email shouldBe "tarao@example.com"
      r2 shouldStaticallyBe a[% { val email: String }]
    }
  }

  describe("Generic record extension with +") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.Record.Append

    it("can be done by using Append") {
      def addEmail[R <: %](record: R, email: String)(using
        append: Append[R, % { val email: String }],
      ): append.Out = record + (email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[
        % { val name: String; val age: Int; val email: String },
      ]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can be done by using Append.Aux") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Append.Aux[R, % { val email: String }, RR],
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

    it("can replace existing field") {
      def addEmail[R <: %, T, RR <: %](record: R, email: T)(using
        Append.Aux[R, % { val email: T }, RR],
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
            Append.Aux[R & Tag[Person], % { val email: String }, RR],
          ): RR = p + (email = email)
        }
      }

      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Append.Aux[R, % { val email: String }, RR],
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

    it("should lose record type information without using Append") {
      def addEmail[R <: %](record: R, email: String) =
        record + (email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      "r1.name" shouldNot typeCheck
      "r1.age" shouldNot typeCheck
      r1.email shouldBe "tarao@example.com"
      r1 shouldStaticallyBe a[% { val email: String }]

      def addEmail2[R <: % { val name: String }](record: R, email: String) =
        record + (email = email)
      val r2 = addEmail2(r0, "tarao@example.com")
      "r2.name" shouldNot typeCheck
      "r2.age" shouldNot typeCheck
      r2.email shouldBe "tarao@example.com"
      r2 shouldStaticallyBe a[% { val email: String }]
    }
  }

  describe("Generic array record lookup") {
    import com.github.tarao.record4s.ArrayRecord
    import com.github.tarao.record4s.typing.ArrayRecord.Lookup

    it("can be done by using Lookup") {
      inline def getEmail[R](record: ArrayRecord[R])(using
        hasEmail: Lookup[R, "email"],
      ): hasEmail.Out = ArrayRecord.lookup(record, "email")

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = r0 + (email = "tarao@example.com")
      getEmail(r1) shouldBe "tarao@example.com"
      "getEmail(r0)" shouldNot typeCheck
    }
  }

  describe("Generic array record extension with ++") {
    import com.github.tarao.record4s.{ArrayRecord, ProductRecord, Tag}
    import com.github.tarao.record4s.typing.ArrayRecord.Concat

    it("can be done by using Concat") {
      def addEmail[R](record: ArrayRecord[R], email: String)(using
        concat: Concat[R, ArrayRecord[("email", String) *: EmptyTuple]],
      ): concat.Out = record ++ ArrayRecord(email = email)

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe an[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)),
      ]]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can be done by using Concat.Aux") {
      def addEmail[R, RR <: ProductRecord](
        record: ArrayRecord[R],
        email: String,
      )(using
        Concat.Aux[R, ArrayRecord[("email", String) *: EmptyTuple], RR],
      ): RR = record ++ ArrayRecord(email = email)

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
        def addEmail[R, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: T,
        )(using
          Concat.Aux[R, ArrayRecord[("email", T) *: EmptyTuple], RR],
        ): RR = record ++ ArrayRecord(email = email)

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
        def rename[R, T, RR <: ProductRecord](record: ArrayRecord[R], value: T)(
          using Concat.Aux[R, ArrayRecord[("name", T) *: EmptyTuple], RR],
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
            Concat.Aux[
              (("name", String) *: T) & Tag[Person],
              ArrayRecord[("email", String) *: EmptyTuple],
              RR,
            ],
          ): RR = p ++ ArrayRecord(email = email)
        }
      }

      def addEmail[R, T, RR <: ProductRecord](record: ArrayRecord[R], email: T)(
        using Concat.Aux[R, ArrayRecord[("email", T) *: EmptyTuple], RR],
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
        def addEmail[R](record: ArrayRecord[R], email: String) =
          record ++ ArrayRecord(email = email)
      """ shouldNot typeCheck
    }
  }

  describe("Generic array record extension with +") {
    import com.github.tarao.record4s.{ArrayRecord, ProductRecord, Tag}
    import com.github.tarao.record4s.typing.ArrayRecord.Append

    it("can be done by using Append") {
      def addEmail[R](record: ArrayRecord[R], email: String)(using
        append: Append[R, ("email", String) *: EmptyTuple],
      ): append.Out = record + (email = email)

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)),
      ]]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can be done by using Append.Aux") {
      def addEmail[R, RR <: ProductRecord](
        record: ArrayRecord[R],
        email: String,
      )(using
        Append.Aux[R, ("email", String) *: EmptyTuple, RR],
      ): RR = record + (email = email)

      val r0 = ArrayRecord(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldStaticallyBe a[ArrayRecord[
        (("name", String), ("age", Int), ("email", String)),
      ]]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can replace existing field") {
      locally {
        def addEmail[R, T, RR <: ProductRecord](
          record: ArrayRecord[R],
          email: T,
        )(using
          Append.Aux[R, ("email", T) *: EmptyTuple, RR],
        ): RR = record + (email = email)

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
        def rename[R, T, RR <: ProductRecord](record: ArrayRecord[R], value: T)(
          using Append.Aux[R, ("name", T) *: EmptyTuple, RR],
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
            Append.Aux[
              (("name", String) *: T) & Tag[Person],
              ("email", String) *: EmptyTuple,
              RR,
            ],
          ): RR = p + (email = email)
        }
      }

      def addEmail[R, T, RR <: ProductRecord](record: ArrayRecord[R], email: T)(
        using Append.Aux[R, ("email", T) *: EmptyTuple, RR],
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
        def addEmail[R](record: ArrayRecord[R], email: String) =
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

package external

class UseCaseSpec extends helper.UnitSpec {
  describe("Generic record extension with ++") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.Concat

    it("can be done by using Concat") {
      def addEmail[R <: %](record: R, email: String)(using
        concat: Concat[R, % { val email: String }],
      ): concat.Out = record ++ %(email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
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
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
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
      r1 shouldBe a[
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
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldBe a[% { val name: String; val age: Int; val email: String }]
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
      r1 shouldBe a[% { val email: String }]

      def addEmail2[R <: % { val name: String }](record: R, email: String) =
        record ++ %(email = email)
      val r2 = addEmail(r0, "tarao@example.com")
      "r2.name" shouldNot typeCheck
      "r2.age" shouldNot typeCheck
      r2.email shouldBe "tarao@example.com"
      r2 shouldBe a[% { val email: String }]
    }
  }

  describe("Generic record extension with +") {
    import com.github.tarao.record4s.{%, Tag}
    import com.github.tarao.record4s.typing.Append

    it("can be done by using Append") {
      def addEmail[R <: %](record: R, email: String)(using
        append: Append[R, ("email", String) *: EmptyTuple],
      ): append.Out = record + (email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can be done by using Append.Aux") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Append.Aux[R, ("email", String) *: EmptyTuple, RR],
      ): RR = record + (email = email)

      val r0 = %(name = "tarao", age = 3)
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
      r1.name shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"
    }

    it("can replace existing field") {
      def addEmail[R <: %, T, RR <: %](record: R, email: T)(using
        Append.Aux[R, ("email", T) *: EmptyTuple, RR],
      ): RR = record + (email = email)

      val r0 = %(name = "tarao", age = 3, email = "tarao@example.com")
      val r1 = addEmail(r0, %(user = "tarao", domain = "example.com"))
      r1 shouldBe a[
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
            Append.Aux[R & Tag[Person], ("email", String) *: EmptyTuple, RR],
          ): RR = p + (email = email)
        }
      }

      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Append.Aux[R, ("email", String) *: EmptyTuple, RR],
      ): RR = record + (email = email)

      val r0 = %(name = "tarao fuguta", age = 3).tag[Person]
      val r1 = addEmail(r0, "tarao@example.com")
      r1 shouldBe a[% { val name: String; val age: Int; val email: String }]
      r1.name shouldBe "tarao fuguta"
      r1.firstName shouldBe "tarao"
      r1.age shouldBe 3
      r1.email shouldBe "tarao@example.com"

      val r2 = r0.withEmail("tarao@example.com")
      r2 shouldBe a[% { val name: String; val age: Int; val email: String }]
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
      r1 shouldBe a[% { val email: String }]

      def addEmail2[R <: % { val name: String }](record: R, email: String) =
        record + (email = email)
      val r2 = addEmail(r0, "tarao@example.com")
      "r2.name" shouldNot typeCheck
      "r2.age" shouldNot typeCheck
      r2.email shouldBe "tarao@example.com"
      r2 shouldBe a[% { val email: String }]
    }
  }

  describe("Pattern matching") {
    import com.github.tarao.record4s.{%, select}

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

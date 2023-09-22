package external

class UseCaseSpec extends helper.UnitSpec {
  describe("Generic record extension with ++") {
    import com.github.tarao.record4s.*

    it("can be done by using Concat.Aux") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Typing.Concat.Aux[R, % { val email: String }, RR],
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
        Typing.Concat.Aux[R, % { val email: T }, RR],
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
  }

  describe("Generic record extension with +") {
    import com.github.tarao.record4s.*

    it("can be done by using Append.Aux") {
      def addEmail[R <: %, RR <: %](record: R, email: String)(using
        Typing.Append.Aux[R, ("email", String) *: EmptyTuple, RR],
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
        Typing.Append.Aux[R, ("email", T) *: EmptyTuple, RR],
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
  }
}

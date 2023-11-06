package com.github.tarao.record4s

import scala.compiletime.testing.{typeCheckErrors, Error, ErrorKind}

class TypeErrorSpec extends helper.UnitSpec {
  describe("Typing %") {
    describe("Concat") {
      it("should detect invalid characters in field labels") {
        def checkErrors(errs: List[Error]): Unit = {
          errs should not be empty
          errs.head.kind shouldBe ErrorKind.Typer
          val _ = errs.head.message should startWith(
            "'$' cannot be used in a field label",
          )
        }

        case class Cell($value: Int)

        checkErrors(typeCheckErrors("""%(name = "tarao") ++ Cell(3)"""))
        checkErrors(typeCheckErrors("""Record.from(Cell(3))"""))
      }

      it("should detect wrong usage") {
        """
          def addEmail[R <: %, RR <: %](record: R, email: String)(using
            typing.Record.Concat.Aux[R, Nothing, RR],
          ): RR = record ++ %(email = email)
        """ shouldNot typeCheck

        """
          def addEmail[R <: %, RR <: %](record: R, email: String)(using
            typing.Record.Concat.Aux[R, % { val name: String }, RR],
          ): RR = record ++ %(email = email)
        """ shouldNot typeCheck
      }
    }

    describe("Lookup") {
      it("should detect illegal member access") {
        val r = %(name = "tarao")
        val errs = typeCheckErrors("""Record.lookup(r, "email")""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith("""Value '("email" : String)' is not a member of""")
      }

      it("should detect invalid key type") {
        val r = %(name = "tarao")
        val key = "email"
        val errs = typeCheckErrors("""Record.lookup(r, key)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith("""Value '(key : String)' is not a member of""")
      }
    }

    describe("Select") {
      it("should detect missing key") {
        def checkErrors(errs: List[Error]): Unit = {
          errs should not be empty
          errs.head.kind shouldBe ErrorKind.Typer
          val _ =
            errs.head.message should include regex "Missing key \\\\?'age\\\\?'"
        }

        val r = %(name = "tarao")
        checkErrors(typeCheckErrors("""r(select.age)"""))

        case class Person(name: String, age: Int)
        checkErrors(typeCheckErrors("""r.to[Person]"""))
      }

      it("should detect invalid selector type") {
        val r = %(name = "tarao")
        val key = "name"
        val s = new Selector[key.type *: EmptyTuple]
        val errs = typeCheckErrors("""r(s)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith(
          "Selector type element must be a literal",
        )
      }
    }

    describe("Unselect") {
      it("should detect invalid unselector type") {
        val r = %(name = "tarao")
        val key = "name"
        val u = new Unselector[key.type *: EmptyTuple]
        val errs = typeCheckErrors("""r(u)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        // the error message is about proving R <:< RR
      }
    }

    describe("RecordLike") {
      it("should detect non-literal label type") {
        val errs =
          typeCheckErrors("""%(name = "tarao") ++ (("age", 3) *: EmptyTuple)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith(
          "Types of field labels must be literal string types",
        )
      }
    }
  }

  describe("Typing ArrayRecord") {
    describe("Concat") {
      it("should detect invalid characters in field labels") {
        def checkErrors(errs: List[Error]): Unit = {
          errs should not be empty
          errs.head.kind shouldBe ErrorKind.Typer
          val _ = errs.head.message should startWith(
            "'$' cannot be used in a field label",
          )
        }

        case class Cell($value: Int)

        checkErrors(typeCheckErrors("""ArrayRecord(name = "tarao") ++ Cell(3)"""))
        checkErrors(typeCheckErrors("""ArrayRecord.from(Cell(3))"""))
      }

      it("should detect wrong usage") {
        """
          def addEmail[R, RR <: %](record: ArrayRecord[R], email: String)(using
            typing.ArrayRecord.Concat.Aux[R, Nothing, RR],
          ): RR = record ++ ArrayRecord(email = email)
        """ shouldNot typeCheck

        """
          def addEmail[R, RR <: %](record: ArrayRecord[R], email: String)(using
            typing.ArrayRecord.Concat.Aux[R, ArrayRecord[("name", String) *: EmptyTuple], RR],
          ): RR = record ++ ArrayRecord(email = email)
        """ shouldNot typeCheck
      }
    }

    describe("Lookup") {
      it("should detect illegal member access") {
        val r = ArrayRecord(name = "tarao")
        val errs = typeCheckErrors("""ArrayRecord.lookup(r, "email")""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith("""Value '("email" : String)' is not a member of""")
      }

      it("should detect invalid key type") {
        val r = ArrayRecord(name = "tarao")
        val key = "email"
        val errs = typeCheckErrors("""ArrayRecord.lookup(r, key)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith("""Value '(key : String)' is not a member of""")
      }
    }

    describe("Select") {
      it("should detect missing key") {
        def checkErrors(errs: List[Error]): Unit = {
          errs should not be empty
          errs.head.kind shouldBe ErrorKind.Typer
          val _ =
            errs.head.message should include regex "Missing key \\\\?'age\\\\?'"
        }

        val r = ArrayRecord(name = "tarao")
        checkErrors(typeCheckErrors("""r(select.age)"""))
      }

      it("should detect invalid selector type") {
        val r = ArrayRecord(name = "tarao")
        val key = "name"
        val s = new Selector[key.type *: EmptyTuple]
        val errs = typeCheckErrors("""r(s)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith(
          "Selector type element must be a literal",
        )
      }
    }

    describe("Unselect") {
      it("should detect invalid unselector type") {
        val r = ArrayRecord(name = "tarao")
        val key = "name"
        val u = new Unselector[key.type *: EmptyTuple]
        val errs = typeCheckErrors("""r(u)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        // the error message is about proving R <:< RR
      }
    }

    describe("RecordLike") {
      it("should detect non-literal label type") {
        val errs =
          typeCheckErrors("""ArrayRecord(name = "tarao") ++ (("age", 3) *: EmptyTuple)""")
        errs should not be empty
        errs.head.kind shouldBe ErrorKind.Typer
        errs.head.message should startWith(
          "Types of field labels must be literal string types",
        )
      }
    }
  }
}

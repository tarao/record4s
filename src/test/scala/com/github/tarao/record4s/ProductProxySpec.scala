package com.github.tarao.record4s

class ProductProxySpec extends helper.UnitSpec {
  describe("ProductProxy") {
    it("can be constructed from a record") {
      val r1 = %(name = "tarao", age = 3)
      val p1 = ProductProxy.from(r1)

      p1 shouldBe a[%]
      p1 shouldBe a[Product]
      p1.productElement(0) shouldBe "tarao"
      p1.productElement(1) shouldBe 3

      p1.name shouldBe "tarao"
      p1.age shouldBe 3

      helper.showTypeOf(p1) shouldBe """ProductProxy {
                                       |  val name: String
                                       |  val age: Int
                                       |}""".stripMargin

      trait Person
      val r2 = %(name = "tarao", age = 3).tag[Person]
      val p2 = ProductProxy.from(r2)

      p2 shouldBe a[%]
      p2 shouldBe a[Tag[Person]]
      p2 shouldBe a[Product]
      p2.productElement(0) shouldBe "tarao"
      p2.productElement(1) shouldBe 3

      p2.name shouldBe "tarao"
      p2.age shouldBe 3

      helper.showTypeOf(p2) shouldBe """ProductProxy {
                                       |  val name: String
                                       |  val age: Int
                                       |} & Tag[Person]""".stripMargin
    }

    it("can be mirrored") {
      import scala.deriving.Mirror

      type PersonRecord = ProductProxy {
        val name: String
        val age: Int
      }

      case class Person(name: String, age: Int)

      val m1 = summon[Mirror.ProductOf[PersonRecord]]
      summon[m1.MirroredMonoType =:= PersonRecord]
      summon[m1.MirroredType =:= PersonRecord]
      summon[m1.MirroredElemTypes =:= (String, Int)]
      summon[m1.MirroredElemLabels =:= ("name", "age")]

      val p1 = m1.fromProduct(("tarao", 3))
      p1 shouldBe a[%]
      p1 shouldBe a[Product]
      p1.productElement(0) shouldBe "tarao"
      p1.productElement(1) shouldBe 3

      val p2 = m1.fromProduct(Person("tarao", 3))
      p2 shouldBe a[%]
      p2 shouldBe a[Product]
      p2.productElement(0) shouldBe "tarao"
      p2.productElement(1) shouldBe 3

      val m2 = summon[Mirror.ProductOf[PersonRecord & Tag[Person]]]
      summon[m2.MirroredMonoType =:= (PersonRecord & Tag[Person])]
      summon[m2.MirroredType =:= (PersonRecord & Tag[Person])]
      summon[m2.MirroredElemTypes =:= (String, Int)]
      summon[m2.MirroredElemLabels =:= ("name", "age")]

      val p3 = m2.fromProduct(("tarao", 3))
      p3 shouldBe a[%]
      p3 shouldBe a[Product]
      p3.productElement(0) shouldBe "tarao"
      p3.productElement(1) shouldBe 3

      val p4 = m2.fromProduct(Person("tarao", 3))
      p4 shouldBe a[%]
      p4 shouldBe a[Product]
      p4.productElement(0) shouldBe "tarao"
      p4.productElement(1) shouldBe 3
    }
  }
}

package helper

import scala.compiletime.summonInline
import org.scalatest.matchers.dsl.{ResultOfATypeInvocation, ResultOfAnTypeInvocation}

trait StaticTypeMatcher {
  extension [T1](anything: T1) {
    inline def shouldStaticallyBe[T2](r: ResultOfATypeInvocation[T2]): Unit = {
      val _ = summonInline[T1 <:< T2]
    }

    inline def shouldStaticallyBe[T2](r: ResultOfAnTypeInvocation[T2]): Unit = {
      val _ = summonInline[T1 <:< T2]
    }
  }
}

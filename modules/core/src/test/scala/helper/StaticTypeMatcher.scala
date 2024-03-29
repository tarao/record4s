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

package helper

import org.scalatest.matchers.dsl.{
  ResultOfATypeInvocation,
  ResultOfAnTypeInvocation,
}

import scala.compiletime.summonInline

trait StaticTypeMatcher {
  extension [T1](anything: T1) {
    inline infix def shouldStaticallyBe[T2](
      r: ResultOfATypeInvocation[T2],
    ): Unit = {
      val _ = summonInline[T1 <:< T2]
    }

    inline infix def shouldStaticallyBe[T2](
      r: ResultOfAnTypeInvocation[T2],
    ): Unit = {
      val _ = summonInline[T1 <:< T2]
    }
  }
}

/*
 * Copyright (c) 2023 record4s authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.tarao.record4s
package typing

/** Base trait typing given instances.
  *
  * Typing given instances infers result type as `Out`. If it is impossible to
  * infer the type then, `Out` is `Nothing` and `Msg` is a string literal type
  * of a message describing violation of the typing rule. `Out` is `Nothing` if
  * the type is successfully inferred.
  *
  * To show the typing failure, use `withPotentialTypingError { ... }`.
  */
trait MaybeError {
  type Out
  type Msg <: String
}

private inline def showTypingError(using err: typing.MaybeError): Unit = {
  import scala.compiletime.{constValue, erasedValue, error}

  inline erasedValue[err.Out] match {
    case _: Nothing => error(constValue[err.Msg])
    case _          => // no error
  }
}

inline def withPotentialTypingError[T](
  inline block: => T,
)(using err: typing.MaybeError): T = {
  showTypingError
  block
}

// A dummy class to carry type information of `T`.  Without this context bound, `Concat`
// somehow drops tag types in `T`.  It can be removed if all tests pass since it does
// nothing semantically.
final class Context[T] private ()
object Context {
  private val instance = new Context[Nothing]

  given [T]: Context[T] = instance.asInstanceOf[Context[T]]
}

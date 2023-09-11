package com.github.tarao.record4s

import scala.language.dynamics

class Extensible[R](private val record: R) extends AnyVal with Dynamic {
  transparent inline def applyDynamic(method: String)(
    inline fields: (String, Any)*,
  ) =
    ${ Macros.applyImpl('record, 'method, 'fields) }

  transparent inline def applyDynamicNamed(method: String)(
    inline fields: (String, Any)*,
  ) =
    ${ Macros.applyImpl('record, 'method, 'fields) }
}

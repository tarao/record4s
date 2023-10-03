package helper

import org.scalatest.Inside
import org.scalatest.Inspectors
import org.scalatest.OptionValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers

abstract class UnitSpec
    extends AnyFunSpec
    with matchers.should.Matchers
    with OptionValues
    with Inside
    with Inspectors

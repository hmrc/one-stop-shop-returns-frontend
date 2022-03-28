/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.mappings

import models.{Index, VatRate}
import play.api.data.validation.{Constraint, Invalid, Valid}
import utils.CurrencyFormatter

import java.time.LocalDate

trait Constraints {

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, minimum)
        }
    }

  protected def minimumValue(minimum: BigDecimal, errorKey: String)(implicit ev: Ordering[BigDecimal]): Constraint[BigDecimal] =
    Constraint {
      input =>
        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, CurrencyFormatter.currencyFormat(minimum))
        }
    }

  protected def maximumValue[A](maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, maximum)
        }
    }

  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum && input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, minimum, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _ =>
        Invalid(errorKey, regex)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def nonEmptySet(errorKey: String): Constraint[Set[_]] =
    Constraint {
      case set if set.nonEmpty =>
        Valid
      case _ =>
        Invalid(errorKey)
    }

  protected def nonEmptySeq(errorKey: String): Constraint[Seq[_]] =
    Constraint {
      case seq if seq.nonEmpty =>
        Valid
      case _ =>
        Invalid(errorKey)
    }

  protected def validVatRates(vatRates: Seq[VatRate], errorKey: String): Constraint[List[String]] =
    Constraint {
      case seq if seq.forall(vatRates.map(_.rate.toString).contains) =>
        Valid
      case _ =>
        Invalid(errorKey)
    }

  protected def notADuplicate[A](index: Index, existingAnswers: Seq[A], errorKey: String, args: Any*): Constraint[A] = {

    val indexedAnswers = existingAnswers.zipWithIndex
    val filteredAnswers = indexedAnswers.filter(_._2 != index.position)

    Constraint {
      case answer if filteredAnswers.map(_._1) contains answer =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }
  }
}

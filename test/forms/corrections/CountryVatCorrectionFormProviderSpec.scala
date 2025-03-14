/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.corrections
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import forms.behaviours.DecimalFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import utils.CurrencyFormatter

import scala.math.BigDecimal.RoundingMode

class CountryVatCorrectionFormProviderSpec extends DecimalFieldBehaviours {

  private val country = "Country"

  val minimum = minCurrencyAmount
  val maximum = maxCurrencyAmount

  val form = new CountryVatCorrectionFormProvider()(country, minimum - 0.02, false)

  ".value" - {

    val fieldName = "value"

    val validDataGeneratorForPositive =
      Gen.choose[BigDecimal](BigDecimal(0.01), maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    val validDataGeneratorForNegative =
      Gen.choose[BigDecimal](minimum, BigDecimal(-0.01))
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    "bind valid data positive" in {

      forAll(validDataGeneratorForPositive -> "validDataItem") {
        (dataItem: String) =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe empty
      }
    }

    "bind valid data negative" in {
      forAll(validDataGeneratorForNegative -> "validDataItem") {
        (dataItem: String) =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe empty
      }
    }

    behave like decimalField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "countryVatCorrection.error.nonNumeric", Seq(country)),
      invalidNumericError = FormError(fieldName, "countryVatCorrection.error.wholeNumber", Seq(country))
    )

    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "countryVatCorrection.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "countryVatCorrection.error.required", Seq(country))
    )

    "show correct error" in {
      val form = new CountryVatCorrectionFormProvider()(country, minimum - 0.02, true)

      val result = form.bind(Map(fieldName -> (maximum + 0.01).toString)).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "countryVatCorrection.error.outOfRange.undeclared", Seq(minimum, maximum)))
    }

    "fail when value is below minimum allowed correction" in {

      val minimumAllowedCorrection = 0.0
      val form = new CountryVatCorrectionFormProvider()(country, minimumAllowedCorrection, false)

      forAll(validDataGeneratorForNegative -> "validDataItem") {
        (dataItem: String) =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe List(FormError(fieldName, "countryVatCorrection.error.negative", Seq(CurrencyFormatter.currencyFormat(minimumAllowedCorrection))))
      }
    }
  }
}

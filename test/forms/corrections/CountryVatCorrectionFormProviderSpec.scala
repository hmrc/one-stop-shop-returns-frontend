/*
 * Copyright 2021 HM Revenue & Customs
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
import config.Constants.maxCurrencyAmount
import forms.behaviours.{DecimalFieldBehaviours, IntFieldBehaviours}
import org.scalacheck.Gen
import pages.corrections.CorrectionCountryPage
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class CountryVatCorrectionFormProviderSpec extends DecimalFieldBehaviours {

  private val country = "Country"

  val form = new CountryVatCorrectionFormProvider()(country)

  ".value" - {

    val fieldName = "value"

    val minimum = BigDecimal(-1000000000)
    val maximum = maxCurrencyAmount

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
        dataItem: String =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe empty
      }
    }

    "bind valid data negative" in {

      forAll(validDataGeneratorForNegative -> "validDataItem") {
        dataItem: String =>
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
  }
}

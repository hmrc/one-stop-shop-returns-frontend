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

package forms

import config.Constants.maxCurrencyAmount
import forms.behaviours.DecimalFieldBehaviours
import models.VatRate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.FormError
import services.VatRateService

import scala.math.BigDecimal.RoundingMode

class NetValueOfSalesFromEuFormProviderSpec extends DecimalFieldBehaviours {

  private val mockVatRateService = mock[VatRateService]

  private val vatRate = arbitrary[VatRate].sample.value
  val form = new NetValueOfSalesFromEuFormProvider(mockVatRateService)(vatRate)

  ".value" - {

    val fieldName = "value"

    val minimum = BigDecimal(0.01)
    val maximum = maxCurrencyAmount

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_EVEN))
        .map(_.toString)

    when(mockVatRateService.standardVatOnSales(any(), any())) thenReturn BigDecimal(1)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like decimalField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "netValueOfSalesFromEu.error.nonNumeric", Seq(vatRate.rateForDisplay)),
      invalidNumericError = FormError(fieldName, "netValueOfSalesFromEu.error.wholeNumber", Seq(vatRate.rateForDisplay))
    )

    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "netValueOfSalesFromEu.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "netValueOfSalesFromEu.error.required", Seq(vatRate.rateForDisplay))
    )

    "must not bind when calculated vat amount is less than or equal to zero" in {

      when(mockVatRateService.standardVatOnSales(any(), any())) thenReturn BigDecimal(0)

      val result = form.bind(Map(
        fieldName -> "0.02"
      ))

      result.errors must contain only FormError(fieldName, "netValueOfSalesFromEu.error.calculatedVatRateOutOfRange")
    }
  }
}

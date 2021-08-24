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

package forms

import forms.behaviours.DecimalFieldBehaviours
import models.VatRate
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

class SalesAtVatRateFromNiFormProviderSpec extends DecimalFieldBehaviours {

  private val vatRate = arbitrary[VatRate].sample.value
  private val form = new SalesAtVatRateFromNiFormProvider()(vatRate)

  ".netValueOfSales" - {

    val fieldName = "netValueOfSales"

    val minimum = 0
    val maximum = 1000000

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like decimalField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "netValueOfSalesFromNi.error.nonNumeric", Seq(vatRate.rateForDisplay)),
      invalidNumericError = FormError(fieldName, "netValueOfSalesFromNi.error.wholeNumber", Seq(vatRate.rateForDisplay))
    )

    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "netValueOfSalesFromNi.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "netValueOfSalesFromNi.error.required", Seq(vatRate.rateForDisplay))
    )
  }


  ".vatOnSales" - {

    val fieldName = "vatOnSales"

    val minimum: BigDecimal = 0
    val maximum: BigDecimal = 1000000

    val validDataGenerator = decimalInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like decimalField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "vatOnSalesFromNi.error.nonNumeric", Seq(vatRate.rateForDisplay)),
      invalidNumericError  = FormError(fieldName, "vatOnSalesFromNi.error.wholeNumber", Seq(vatRate.rateForDisplay))
    )

    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum       = minimum,
      maximum       = maximum,
      expectedError = FormError(fieldName, "vatOnSalesFromNi.error.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "vatOnSalesFromNi.error.required", Seq(vatRate.rateForDisplay))
    )
  }
}

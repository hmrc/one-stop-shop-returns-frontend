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

import config.Constants.maxCurrencyAmount
import forms.behaviours.DecimalFieldBehaviours
import play.api.data.FormError

class SalesAtVatRateFromEuFormProviderSpec extends DecimalFieldBehaviours {

  val form = new SalesAtVatRateFromEuFormProvider()()

  ".netValueOfSales" - {

    val fieldName = "netValueOfSales"
    val requiredError = "salesAtVatRateFromEu.error.netValueOfSales.required"
    val nonNumericError = "salesAtVatRateFromEu.error.netValueOfSales.nonNumeric"
    val invalidNumericError = "salesAtVatRateFromEu.error.netValueOfSales.invalidNumeric"
    val outOfRangeError = "salesAtVatRateFromEu.error.netValueOfSales.outOfRange"

    behave like decimalField(
      form,
      fieldName,
      FormError(fieldName, nonNumericError),
      FormError(fieldName, invalidNumericError)
    )
    
    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum = 0,
      maximum = maxCurrencyAmount,
      expectedError = FormError(fieldName, outOfRangeError, Seq(0, maxCurrencyAmount))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredError)
    )
  }

  ".vatOnSales" - {

    val fieldName = "vatOnSales"
    val requiredError = "salesAtVatRateFromEu.error.vatOnSales.required"
    val nonNumericError = "salesAtVatRateFromEu.error.vatOnSales.nonNumeric"
    val invalidNumericError = "salesAtVatRateFromEu.error.vatOnSales.invalidNumeric"
    val outOfRangeError = "salesAtVatRateFromEu.error.vatOnSales.outOfRange"

    behave like decimalField(
      form,
      fieldName,
      FormError(fieldName, nonNumericError),
      FormError(fieldName, invalidNumericError)
    )

    behave like decimalFieldWithRange(
      form,
      fieldName,
      minimum = 0,
      maximum = maxCurrencyAmount,
      expectedError = FormError(fieldName, outOfRangeError, Seq(0, maxCurrencyAmount))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredError)
    )
  }
}

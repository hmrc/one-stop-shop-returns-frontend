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

package forms

import config.Constants.maxCurrencyAmount
import forms.mappings.Mappings
import models.VatRate
import play.api.data.Form
import services.VatRateService

import javax.inject.Inject

class NetValueOfSalesFromEuFormProvider @Inject()(vatRateService: VatRateService) extends Mappings {

  def apply(vatRate: VatRate): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "netValueOfSalesFromEu.error.required",
        "netValueOfSalesFromEu.error.wholeNumber",
        "netValueOfSalesFromEu.error.nonNumeric",
        args = Seq(vatRate.rateForDisplay))
          .verifying(inRange[BigDecimal](0.01, maxCurrencyAmount, "netValueOfSalesFromEu.error.outOfRange"))
        .verifying("netValueOfSalesFromEu.error.calculatedVatRateOutOfRange", value => {
          vatRateService.standardVatOnSales(value, vatRate) > BigDecimal(0)
        })
    )
}

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
import models.VatOnSalesChoice.{NonStandard, Standard}
import models.{VatOnSales, VatOnSalesChoice, VatRate}
import play.api.data.Form
import play.api.data.Forms.mapping
import services.VatRateService
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import javax.inject.Inject

class VatOnSalesFromNiFormProvider @Inject()(vatRateService: VatRateService) extends Mappings {

  def apply(vatRate: VatRate, netSales: BigDecimal): Form[VatOnSales] =
    Form(
      mapping(
        "choice" -> enumerable[VatOnSalesChoice]("vatOnSalesFromNi.choice.error.required"),
        "amount" -> mandatoryIfEqual("choice", VatOnSalesChoice.NonStandard.toString, currency(
          "vatOnSalesFromNi.amount.error.required",
          "vatOnSalesFromNi.amount.error.decimalFormat",
          "vatOnSalesFromNi.amount.error.nonNumeric"
        )
        .verifying(inRange[BigDecimal](0.01, maxCurrencyAmount, "vatOnSalesFromNi.amount.error.calculatedVatRateOutOfRange"))
        )
      )(a(vatRate, netSales))(u)
    )

  private def a( vatRate: VatRate, netSales: BigDecimal)
               (choice: VatOnSalesChoice, amount: Option[BigDecimal])
               : VatOnSales =
    (choice, amount) match {
      case (Standard, _)               => VatOnSales(Standard, vatRateService.standardVatOnSales(netSales, vatRate))
      case (NonStandard, Some(amount)) => VatOnSales(NonStandard, amount)
      case (NonStandard, None)         => throw new IllegalArgumentException("Tried to bind a form for an other amount, but no amount was supplied")
    }

  private def u(vatOnSales: VatOnSales): Option[(VatOnSalesChoice, Option[BigDecimal])] =
    vatOnSales.choice match {
      case Standard    => Some((Standard, None))
      case NonStandard => Some((NonStandard, Some(vatOnSales.amount)))
    }
}

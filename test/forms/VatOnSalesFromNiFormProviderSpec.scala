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
import models.VatOnSalesChoice._
import models.{VatOnSales, VatRate, VatRateType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.FormError
import services.VatRateService

import java.time.LocalDate

class VatOnSalesFromNiFormProviderSpec extends DecimalFieldBehaviours {

  private val vatRate            = VatRate(1, VatRateType.Standard, LocalDate.now)
  private val netSales           = BigDecimal(1)
  private val standardVatOnSales = BigDecimal(1)

  private val mockVatRateService = mock[VatRateService]
  when(mockVatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales

  private val form = new VatOnSalesFromNiFormProvider(mockVatRateService)(vatRate, netSales)

  "form" - {

    "when Standard is selected" - {

      "must bind" in {

        val result = form.bind(Map("choice" -> Standard.toString))
        result.value.value mustEqual VatOnSales(Standard, standardVatOnSales)
        result.errors mustBe empty
      }
    }

    "when NonStandard is selected" - {

      "must bind when a valid amount is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> "1"
        ))
        result.value.value mustEqual VatOnSales(NonStandard, 1)
        result.errors mustBe empty
      }

      "must not bind when a negative amount is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> "-1"
        ))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.outOfRange", Seq(0.01, maxCurrencyAmount))
      }

      "must not bind when a zero amount is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> "0"
        ))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.outOfRange", Seq(0.01, maxCurrencyAmount))
      }

      "must not bind when an amount greater than 1,000,000,000 is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> (maxCurrencyAmount + 1).toString
        ))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.outOfRange", Seq(0.01, maxCurrencyAmount))
      }

      "must not bind when a non-numeric amount is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> "foo"
        ))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.nonNumeric")
      }

      "must not bind when a number with too many decimal places is supplied" in {

        val result = form.bind(Map(
          "choice" -> NonStandard.toString,
          "amount" -> "1.234"
        ))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.decimalFormat")
      }

      "must not bind when an amount is not supplied" in {

        val result = form.bind(Map("choice" -> NonStandard.toString))
        result.errors must contain only FormError("amount", "vatOnSalesFromNi.amount.error.required")
      }
    }

    "when no choice is selected" - {

      "must not bind" in {

        val result = form.bind(Map.empty[String, String])
        result.errors must contain only FormError("choice", "vatOnSalesFromNi.choice.error.required")
      }
    }
  }
}

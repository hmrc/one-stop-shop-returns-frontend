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

package models.financialdata

import base.SpecBase
import models.PaymentState.{NoneDue, Paid, PaymentDue}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class VatReturnWithFinancialDataSpec extends AnyFreeSpec
  with Matchers
  with SpecBase {
  val vatAmount = BigDecimal(1000)
  val outstandingCharge = Charge(completeVatReturn.period, vatAmount, vatAmount, BigDecimal(0))
  val payedCharge = Charge(completeVatReturn.period, vatAmount, BigDecimal(0), vatAmount)
  "showPayNow" - {

    "is true when" - {

      "the return is not nil and there is an outstanding charge" in {
        val returnWithData = VatReturnWithFinancialData(
          completeVatReturn, Some(outstandingCharge), vatAmount, None)
        returnWithData.showPayNow mustBe true
        returnWithData.paymentState mustBe PaymentDue
      }

      "the return is not nil and there is no charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, vatAmount, None)
        returnWithData.showPayNow mustBe true
        returnWithData.paymentState mustBe PaymentDue
      }

      "the return is not nil and there is an outstanding charge and a correction" in {
        val returnWithData = VatReturnWithFinancialData(
          completeVatReturn, Some(outstandingCharge), vatAmount, Some(emptyCorrectionPayload)
        )
        returnWithData.showPayNow mustBe true
        returnWithData.paymentState mustBe PaymentDue
      }
    }

    "is false when" - {

      "the return is nil and there is no charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, 0, None)
        returnWithData.showPayNow mustBe false
        returnWithData.paymentState mustBe NoneDue
      }

      "the return is not nil and there is zero outstanding charge" - {

        "and there is an initial charge" in {
          val returnWithData = VatReturnWithFinancialData(completeVatReturn, Some(payedCharge), 0, None)
          returnWithData.showPayNow mustBe false
          returnWithData.paymentState mustBe Paid
        }

        "and there is no initial charge" in {
          val noCharge = Charge(emptyVatReturn.period, BigDecimal(0), BigDecimal(0), BigDecimal(0))
          val returnWithData = VatReturnWithFinancialData(emptyVatReturn, Some(noCharge), 0, None)
          returnWithData.showPayNow mustBe false
          returnWithData.paymentState mustBe NoneDue
        }
      }

      "the vat owed is none and there is no outstanding charge and no correction" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, 0, None)
        returnWithData.showPayNow mustBe false
        returnWithData.paymentState mustBe NoneDue
      }
    }
  }

}

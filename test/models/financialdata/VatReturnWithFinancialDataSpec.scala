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

package models.financialdata

import base.SpecBase
import generators.Generators
import models.PaymentState.{NoneDue, Paid, PaymentDue}
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VatReturnWithFinancialDataSpec extends AnyFreeSpec
  with Matchers
  with SpecBase {
  val vatAmount = 1000L
  val outstandingCharge = Charge(completeVatReturn.period, BigDecimal(vatAmount), BigDecimal(vatAmount), BigDecimal(0))
  val payedCharge = Charge(completeVatReturn.period, BigDecimal(vatAmount), BigDecimal(0), BigDecimal(vatAmount))
  "showPayNow" - {

    "is true when" - {

      "the return is not nil and there is an outstanding charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, Some(outstandingCharge), Some(vatAmount))
        returnWithData.showPayNow mustBe true
        returnWithData.paymentState mustBe PaymentDue
      }

      "the return is not nil and there is no charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, Some(vatAmount))
        returnWithData.showPayNow mustBe true
        returnWithData.paymentState mustBe PaymentDue
      }
    }

    "is false when" - {

      "the return is nil and there is no charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, Some(0L))
        returnWithData.showPayNow mustBe false
        returnWithData.paymentState mustBe NoneDue
      }

      "the return is not nil and there is zero outstanding charge" - {

        "and there is an initial charge" in {
          val returnWithData = VatReturnWithFinancialData(completeVatReturn, Some(payedCharge), Some(0L))
          returnWithData.showPayNow mustBe false
          returnWithData.paymentState mustBe Paid
        }

        "and there is no initial charge" in {
          val payedCharge = Charge(completeVatReturn.period, BigDecimal(0), BigDecimal(0), BigDecimal(0))
          val returnWithData = VatReturnWithFinancialData(completeVatReturn, Some(payedCharge), Some(0L))
          returnWithData.showPayNow mustBe false
          returnWithData.paymentState mustBe NoneDue
        }
      }

      "the vat owed is none and there is no outstanding charge" in {
        val returnWithData = VatReturnWithFinancialData(completeVatReturn, None, None)
        returnWithData.showPayNow mustBe false
        returnWithData.paymentState mustBe NoneDue
      }
    }
  }

}

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

package services

import base.SpecBase
import models.domain.VatReturn
import models.financialdata.{Charge, VatReturnWithFinancialData}
import models.Period
import models.Quarter.Q3
import models.corrections.CorrectionPayload
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

class FinancialDataServiceSpec extends SpecBase with MockitoSugar {

  private val mockVatReturnSalesService = mock[VatReturnSalesService]
  private val financialDataService = new FinancialDataService(mockVatReturnSalesService)

  private val vatReturn = arbitrary[VatReturn].sample.value
  private val correctionPayload = arbitrary[CorrectionPayload].sample.value

  private val fullyPaidCharge = Charge(
    period = Period(2021, Q3),
    originalAmount = BigDecimal(1000),
    outstandingAmount = BigDecimal(0),
    clearedAmount = BigDecimal(1000)
  )
  private val notPaidCharge = Charge(
    period = Period(2021, Q3),
    originalAmount = BigDecimal(1000),
    outstandingAmount = BigDecimal(1000),
    clearedAmount = BigDecimal(0)
  )

  ".filterIfPaymentIsOutstanding" - {

    "when passing one vatReturnWithFinancialData" - {

      val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, None, None, None)
      val vatOnSales = BigDecimal(1000)

      "should return one vatReturnWithFinancialData" - {

        "when no charge exists and has vat owed with no correction" in {
          when(mockVatReturnSalesService.getTotalVatOnSales(vatReturn, None)).thenReturn(vatOnSales)

          val result = financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData))

          result mustBe Seq(vatReturnWithFinancialData)
          verify(mockVatReturnSalesService, times(1)).getTotalVatOnSales(vatReturn, None)
        }

        "when no charge exists and has vat owed with correction" in {
          when(mockVatReturnSalesService.getTotalVatOnSales(vatReturn, Some(correctionPayload)))
            .thenReturn(vatOnSales)

          val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, None, None, Some(correctionPayload))

          val result =
            financialDataService.filterIfPaymentIsOutstanding(
              Seq(vatReturnWithFinancialData)
            )

          result mustBe Seq(vatReturnWithFinancialData)
          verify(mockVatReturnSalesService, times(1))
            .getTotalVatOnSales(vatReturn, Some(correctionPayload))
        }

        "when charge exists with outstanding amount" in {
          val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(notPaidCharge), None, None)
          val result = financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData))

          result mustBe Seq(vatReturnWithFinancialData)
        }
      }
    }

    "when passing vatReturnWithFinancialDatas" - {

      "should return empty when no outstanding amounts" in {
        val vatReturn2 = arbitrary[VatReturn].sample.value
        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0L), None)
        val vatReturnWithFinancialData2 = VatReturnWithFinancialData(vatReturn2, Some(fullyPaidCharge), Some(0L), None)

        financialDataService.filterIfPaymentIsOutstanding(
          Seq(vatReturnWithFinancialData, vatReturnWithFinancialData2)
        ) mustBe Seq.empty
      }

      "should return all vatReturnWithFinancialDatas with outstanding amounts" in {
        val vatReturn2 = arbitrary[VatReturn].sample.value
        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(notPaidCharge), Some(1000L), None)
        val vatReturnWithFinancialData2 = VatReturnWithFinancialData(vatReturn2, Some(notPaidCharge), Some(1000L), None)

        financialDataService.filterIfPaymentIsOutstanding(
          Seq(vatReturnWithFinancialData, vatReturnWithFinancialData2)
        ) mustBe Seq(vatReturnWithFinancialData, vatReturnWithFinancialData2)
      }
    }

    "return empty when" - {

      "charge has been fully paid" in {
        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0), None)

        val result = financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData))

        result mustBe Seq.empty
      }

      "no charge exists and does not have vat owed" in {
        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0), None)

        val result = financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData))

        result mustBe Seq.empty
      }
    }
  }
}

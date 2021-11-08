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
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

class FinancialDataServiceTest extends SpecBase with MockitoSugar {

  private val mockVatReturnSalesService = mock[VatReturnSalesService]
  private val financialDataService = new FinancialDataService(mockVatReturnSalesService)

  private val vatReturn = arbitrary[VatReturn].sample.value
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

    "passing 1 and return 1 when" - {

      "no charge exists and has vat owed" in {

        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, None, None)

        when(mockVatReturnSalesService.getTotalVatOnSales(vatReturn)) thenReturn BigDecimal(1000)

        financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData)) mustBe Seq(vatReturnWithFinancialData)
      }

      "charge exists with outstanding amount" in {

        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(notPaidCharge), None)

        financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData)) mustBe Seq(vatReturnWithFinancialData)
      }

    }

    "passing 2 and return empty when" - {

      "both have been paid" in {

        val vatReturn2 = arbitrary[VatReturn].sample.value
        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0L))
        val vatReturnWithFinancialData2 = VatReturnWithFinancialData(vatReturn2, Some(fullyPaidCharge), Some(0L))

        financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData, vatReturnWithFinancialData2)) mustBe Seq.empty

      }

    }

    "return empty when" - {

      "charge has been fully paid" in {

        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0))

        financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData)) mustBe Seq.empty
      }

      "no charge exists and does not have vat owed" in {

        val vatReturnWithFinancialData = VatReturnWithFinancialData(vatReturn, Some(fullyPaidCharge), Some(0))

        financialDataService.filterIfPaymentIsOutstanding(Seq(vatReturnWithFinancialData)) mustBe Seq.empty
      }

    }

  }

}

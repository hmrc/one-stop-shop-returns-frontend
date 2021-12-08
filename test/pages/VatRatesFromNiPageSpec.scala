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

package pages

import controllers.routes
import models.VatOnSalesChoice.Standard
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, Country, Index, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class VatRatesFromNiPageSpec extends PageBehaviours {

  "VatRatesFromNiPage" - {
    val newVatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value
    val answersWithVatRate = emptyUserAnswers.set(SoldGoodsFromNiPage, true).success.value
      .set(CountryOfConsumptionFromNiPage(index), Country("Spain", "Spain")).success.value
      .set(VatRatesFromNiPage(index), newVatRates).success.value

    beRetrievable[List[VatRate]](VatRatesFromNiPage(index))

    beSettable[List[VatRate]](VatRatesFromNiPage(index))

    beRemovable[List[VatRate]](VatRatesFromNiPage(index))

    "must navigate in Normal mode" - {

      "to Net Value of Sales" in {

        VatRatesFromNiPage(index).navigate(NormalMode, answersWithVatRate)
          .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period, index, Index(0)))
      }
    }

    "must navigate in Check mode" - {

      "to Net Value of Sales" in {

        VatRatesFromNiPage(index).navigate(CheckMode, answersWithVatRate)
          .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, emptyUserAnswers.period, index, Index(0)))
      }
    }

    "must navigate in Check Loop mode" - {

        "to Net Value of Sales" in {

          VatRatesFromNiPage(index).navigate(CheckLoopMode, answersWithVatRate)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckLoopMode, emptyUserAnswers.period, index, Index(0)))
        }
      }

    "must navigate in Check Second Loop mode" - {

        "to Net Value of Sales" in {

          VatRatesFromNiPage(index).navigate(CheckSecondLoopMode, answersWithVatRate)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckSecondLoopMode, emptyUserAnswers.period, index, Index(0)))
        }
      }

    "cleanup" - {
      val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

      "must remove all Net Value of Sales and VAT on Sales answers for this country" in {
        val newVatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value

        val answers = emptyUserAnswers
          .set(VatRatesFromNiPage(index), vatRates).success.value
          .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(1)).success.value
          .set(VatOnSalesFromNiPage(index, index), VatOnSales(Standard, BigDecimal(1))).success.value
          .set(NetValueOfSalesFromNiPage(index, index + 1), BigDecimal(1)).success.value
          .set(VatOnSalesFromNiPage(index, index + 1), VatOnSales(Standard, BigDecimal(1))).success.value

        val expected = emptyUserAnswers
          .set(VatRatesFromNiPage(index), newVatRates).success.value

        val actual = answers.set(VatRatesFromNiPage(index), newVatRates).success.value

        actual mustEqual expected
      }
    }
  }
}

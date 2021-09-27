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
import models.{CheckLoopMode, CheckMode, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class NetValueOfSalesFromEuPageSpec extends PageBehaviours {

  "NetValueOfSalesFromEuPage" - {

    "must navigate in Normal mode" - {

      "to VAT on sales from EU" in {

        NetValueOfSalesFromEuPage(index, index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromEuController.onPageLoad(NormalMode, period, index, index, index))
      }
    }

    "must navigate in Check mode" - {

      "to VAT on sales from EU" in {

        NetValueOfSalesFromEuPage(index, index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromEuController.onPageLoad(CheckMode, period, index, index, index))
      }
    }

    "must navigate in Check Loop mode" - {

      "to VAT on sales from EU" in {

        NetValueOfSalesFromEuPage(index, index, index).navigate(CheckLoopMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromEuController.onPageLoad(CheckLoopMode, period, index, index, index))
      }
    }


    "must remove VAT on sales for the same index when set" in {

      val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value
      val answers =
        emptyUserAnswers
          .set(VatRatesFromEuPage(index, index), vatRates).success.value
          .set(VatOnSalesFromEuPage(index, index, index), VatOnSales(Standard, BigDecimal(1))).success.value
          .set(VatOnSalesFromEuPage(index, index, index + 1), VatOnSales(Standard, BigDecimal(2))).success.value

      val result = answers.set(NetValueOfSalesFromEuPage(index, index, index), BigDecimal(1)).success.value

      result.get(VatOnSalesFromEuPage(index, index, index)) must not be defined
      result.get(VatOnSalesFromEuPage(index, index, index + 1)).value mustEqual VatOnSales(Standard, BigDecimal(2))
    }
  }
}

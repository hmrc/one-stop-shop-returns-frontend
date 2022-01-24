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

package pages

import controllers.routes
import models.VatOnSalesChoice.Standard
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, NormalMode, VatOnSales, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class NetValueOfSalesFromNiPageSpec extends PageBehaviours {

  "NetValueOfSalesFromNiPage" - {

    "must navigate in Normal mode" - {

      "to VAT on sales" in {

        NetValueOfSalesFromNiPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(NormalMode, period, index, index))
      }
    }

    "must navigate in Check mode" - {

      "to VAT on sales" in {

        NetValueOfSalesFromNiPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckMode, period, index, index))
      }
    }

    "must navigate in Check Loop mode" - {

      "to VAT on sales" in {

        NetValueOfSalesFromNiPage(index, index).navigate(CheckLoopMode, emptyUserAnswers)
          .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckLoopMode, period, index, index))
      }
    }

    "must navigate in Check Second Loop mode" - {

        "to VAT on sales" in {

          NetValueOfSalesFromNiPage(index, index).navigate(CheckSecondLoopMode, emptyUserAnswers)
            .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckSecondLoopMode, period, index, index))
        }
      }

    "must navigate in Check Inner Loop mode" - {

        "to VAT on sales" in {

          NetValueOfSalesFromNiPage(index, index).navigate(CheckInnerLoopMode, emptyUserAnswers)
            .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckInnerLoopMode, period, index, index))
        }
      }

   "must navigate in Check Second Inner Loop mode" - {

        "to VAT on sales" in {

          NetValueOfSalesFromNiPage(index, index).navigate(CheckSecondInnerLoopMode, emptyUserAnswers)
            .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckSecondInnerLoopMode, period, index, index))
        }
      }

   "must navigate in Check Final Inner Loop mode" - {

        "to VAT on sales" in {

          NetValueOfSalesFromNiPage(index, index).navigate(CheckFinalInnerLoopMode, emptyUserAnswers)
            .mustEqual(routes.VatOnSalesFromNiController.onPageLoad(CheckFinalInnerLoopMode, period, index, index))
        }
      }

    "must remove VAT on sales for the same index when set" in {

      val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value
      val answers =
        emptyUserAnswers
          .set(VatRatesFromNiPage(index), vatRates).success.value
          .set(VatOnSalesFromNiPage(index, index), VatOnSales(Standard, BigDecimal(1))).success.value
          .set(VatOnSalesFromNiPage(index, index + 1), VatOnSales(Standard, BigDecimal(2))).success.value

      val result = answers.set(NetValueOfSalesFromNiPage(index, index), BigDecimal(1)).success.value

      result.get(VatOnSalesFromNiPage(index, index)) must not be defined
      result.get(VatOnSalesFromNiPage(index, index + 1)).value mustEqual VatOnSales(Standard, BigDecimal(2))
    }
  }
}

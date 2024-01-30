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

package pages

import controllers.routes
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, Index, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class VatOnSalesFromNiPageSpec extends PageBehaviours {

  "VatOnSalesFromNiPage" - {

    "must navigate in Normal Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), vatRates).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex))
        }
      }
    }

    "must navigate in Check Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), vatRates).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(CheckMode, answers.period, countryIndex))
        }
      }
    }

    "must navigate in Check Loop Mode" - {

      "when there are other vat rates" - {

        "it will navigate to the check sales from ni page" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), vatRates).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckLoopMode, answers)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckLoopMode, answers.period, countryIndex, Index(1)))

        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckLoopMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex))
        }
      }
    }

    "must navigate in Check Second Loop Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), vatRates).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckSecondLoopMode, answers)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(CheckSecondLoopMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckSecondLoopMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(CheckSecondLoopMode, answers.period, countryIndex))
        }
      }
    }

    "must navigate in Check Inner Loop Mode" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex))
        }

    }

    "must navigate in Check Second Inner Loop Mode" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckSecondInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(CheckSecondLoopMode, answers.period, countryIndex))
        }

    }

    "must navigate in Check Final Inner Loop Mode" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(CheckFinalInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(CheckMode, answers.period, countryIndex))
        }

    }
  }
}

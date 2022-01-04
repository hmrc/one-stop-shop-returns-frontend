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
import models.{CheckInnerLoopMode, CheckLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, CheckThirdInnerLoopMode, CheckThirdLoopMode, Index, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class VatOnSalesFromEuPageSpec extends PageBehaviours {

  private val countryFromIndex = Index(0)
  private val countryToIndex = Index(0)

  "VatOnSalesFromEuPage" - {

    "must navigate in Normal Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), List(vatRate)).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex))
        }
      }
    }

    "must navigate in Check Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), List(vatRate)).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex))
        }
      }
    }

    "must navigate in Check Loop Mode" - {

      "when there are no other vat rates" - {

        "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex))

        }
      }

      "when there are other vat rates" - {

        "it will navigate to the NetValueOfSalesFromEu page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckLoopMode, answers)
            .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckLoopMode, answers.period, countryFromIndex, countryToIndex, Index(1)))

        }
      }
    }

    "must navigate in Check Second Loop Mode" - {

      "when there are no other vat rates" - {

        "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckSecondLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex))

        }
      }

      "when there are other vat rates" - {

        "it will navigate to the NetValueOfSalesFromEu page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckSecondLoopMode, answers)
            .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex, Index(1)))

        }
      }
    }

    "must navigate in Check Third Loop Mode" - {

      "when there are no other vat rates" - {

        "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(1, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckThirdLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex))

        }
      }

      "when there are other vat rates" - {

        "it will navigate to the NetValueOfSalesFromEu page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckThirdLoopMode, answers)
            .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex, Index(1)))

        }
      }
    }

    "must navigate in Check Final Inner Loop Mode" - {

        "to Check Sales From NI" in {
          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), List(vatRate)).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckMode, answers.period, countryFromIndex, countryToIndex))
        }
      }


    "must navigate in Check Inner Loop Mode" - {

        "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryFromIndex, countryToIndex))

        }
      }


    "must navigate in Check Second Inner Loop Mode" - {

      "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckSecondInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckSecondLoopMode, answers.period, countryFromIndex, countryToIndex))

        }

    }

    "must navigate in Check Third Inner Loop Mode" - {

        "it will navigate to the check sales from ni page" in {

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryFromIndex, countryToIndex), vatRates).success.value

          VatOnSalesFromEuPage(countryFromIndex, countryToIndex, Index(0)).navigate(CheckThirdInnerLoopMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(CheckThirdLoopMode, answers.period, countryFromIndex, countryToIndex))

        }
      }

  }
}

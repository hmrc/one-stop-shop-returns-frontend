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
import models.{CheckMode, Index, NormalMode, SalesAtVatRate, VatRate}
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours
import org.scalacheck.Arbitrary.arbitrary

class SalesAtVatRateFromNiPageSpec extends PageBehaviours {

  "SalesAtVatRateFromNiPage" - {

    beRetrievable[SalesAtVatRate](SalesAtVatRateFromNiPage(index, index))

    beSettable[SalesAtVatRate](SalesAtVatRateFromNiPage(index, index))

    beRemovable[SalesAtVatRate](SalesAtVatRateFromNiPage(index, index))

    "must navigate in Normal Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), vatRates).success.value

          SalesAtVatRateFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.SalesAtVatRateFromNiController.onPageLoad(NormalMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          SalesAtVatRateFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
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

          SalesAtVatRateFromNiPage(countryIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.SalesAtVatRateFromNiController.onPageLoad(CheckMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(vatRate)).success.value

          SalesAtVatRateFromNiPage(countryIndex, Index(0)).navigate(CheckMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(CheckMode, answers.period, countryIndex))
        }
      }
    }
  }
}
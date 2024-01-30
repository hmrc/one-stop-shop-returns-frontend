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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Country, Index, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class VatRatesFromEuPageSpec extends PageBehaviours {

  "VatRatesFromEuPage" - {

    beRetrievable[List[VatRate]](VatRatesFromEuPage(index, index))

    beSettable[List[VatRate]](VatRatesFromEuPage(index, index))

    beRemovable[List[VatRate]](VatRatesFromEuPage(index, index))

    "must navigate in Normal mode" - {

      "to Sales Details from EU" in {

        val countryFrom  = arbitrary[Country].sample.value
        val countryTo    = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
            .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value

        VatRatesFromEuPage(index, index).navigate(NormalMode, answers)
          .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(NormalMode, answers.period, index, index, Index(0)))
      }
    }

    "must navigate in Check mode" - {

      "to Sales Details from EU" in {

        val countryFrom  = arbitrary[Country].sample.value
        val countryTo    = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
            .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value

        VatRatesFromEuPage(index, index).navigate(CheckMode, answers)
          .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, answers.period, index, index, Index(0)))
      }
    }

    "must navigate in Check Loop mode" - {

      "to Sales Details from EU" in {

        val countryFrom  = arbitrary[Country].sample.value
        val countryTo    = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
            .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value

        VatRatesFromEuPage(index, index).navigate(CheckLoopMode, answers)
          .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckLoopMode, answers.period, index, index, Index(0)))
      }
    }

    "must navigate in Check Second Loop mode" - {

      "to Sales Details from EU" in {

        val countryFrom  = arbitrary[Country].sample.value
        val countryTo    = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
            .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value

        VatRatesFromEuPage(index, index).navigate(CheckSecondLoopMode, answers)
          .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckSecondLoopMode, answers.period, index, index, Index(0)))
      }
    }

    "must navigate in Check Third Loop mode" - {

      "to Sales Details from EU" in {

        val countryFrom  = arbitrary[Country].sample.value
        val countryTo    = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
            .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value

        VatRatesFromEuPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(routes.NetValueOfSalesFromEuController.onPageLoad(CheckThirdLoopMode, answers.period, index, index, Index(0)))
      }
    }
  }
}

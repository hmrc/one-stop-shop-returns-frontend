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
import models.{CheckMode, Country, Index, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours


class CountryOfConsumptionFromNiPageSpec extends PageBehaviours {

  "CountryOfConsumptionFromNiPage" - {

    beRetrievable[Country](CountryOfConsumptionFromNiPage(index))

    beSettable[Country](CountryOfConsumptionFromNiPage(index))

    beRemovable[Country](CountryOfConsumptionFromNiPage(index))

    "must navigate in Normal mode" - {

      "to VAT Rates from NI" in {

        CountryOfConsumptionFromNiPage(index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.VatRatesFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period, index))
      }
    }

    "must navigate in Check mode" - {

      "to VAT Rates from NI" in {

        CountryOfConsumptionFromNiPage(index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.VatRatesFromNiController.onPageLoad(CheckMode, emptyUserAnswers.period, index))
      }
    }

    "cleanup" - {

      "must remove values when answer changes" in {
        val country: Country = arbitrary[Country].sample.value
        val vatRate: VatRate = arbitrary[VatRate].sample.value

        val answers = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(index), country).success.value
          .set(VatRatesFromNiPage(index), List(vatRate)).success.value
          .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(0)).success.value
          .set(VatOnSalesFromNiPage(index, index), BigDecimal(0)).success.value

        val expected = emptyUserAnswers
          .set(SoldGoodsFromNiPage, true).success.value
          .set(CountryOfConsumptionFromNiPage(index), country).success.value
        val result = CountryOfConsumptionFromNiPage(index).cleanup(Some(country), answers).success.value

        result mustEqual expected
      }
    }
  }
}

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
import models.{CheckMode, Country, NormalMode, SalesAtVatRate, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class DeleteSalesFromNiPageSpec extends PageBehaviours {

  "DeleteSalesFromNiPage" - {

    beRetrievable[Boolean](DeleteSalesFromNiPage(index))

    beSettable[Boolean](DeleteSalesFromNiPage(index))

    beRemovable[Boolean](DeleteSalesFromNiPage(index))

    "must navigate in Normal mode" - {

      "when there are sales from NI for at least one country in user answers" - {

        "to Sales from NI list" in {

          val country = arbitrary[Country].sample.value
          val vatRate = arbitrary[VatRate].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfConsumptionFromNiPage(index), country).success.value
              .set(VatRatesFromNiPage(index), List(vatRate)).success.value
              .set(SalesAtVatRateFromNiPage(index, index), arbitrary[SalesAtVatRate].sample.value).success.value

          DeleteSalesFromNiPage(index).navigate(NormalMode, answers)
            .mustEqual(routes.SalesFromNiListController.onPageLoad(NormalMode, answers.period))
        }
      }

      "when there are no sales from NI in user answers" - {

        "to Sold Goods From NI" in {

          DeleteSalesFromNiPage(index).navigate(NormalMode, emptyUserAnswers)
            .mustEqual(routes.SoldGoodsFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period))
        }
      }
    }

    "must navigate in Check mode" - {

      "when there are sales from NI for at least one country in user answers" - {

        "to Sales from NI list" in {

          val country = arbitrary[Country].sample.value
          val vatRate = arbitrary[VatRate].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfConsumptionFromNiPage(index), country).success.value
              .set(VatRatesFromNiPage(index), List(vatRate)).success.value
              .set(SalesAtVatRateFromNiPage(index, index), arbitrary[SalesAtVatRate].sample.value).success.value

          DeleteSalesFromNiPage(index).navigate(CheckMode, answers)
            .mustEqual(routes.SalesFromNiListController.onPageLoad(CheckMode, answers.period))
        }
      }

      "when there are no sales from NI in user answers" - {

        "to Sold Goods From NI" in {

          DeleteSalesFromNiPage(index).navigate(CheckMode, emptyUserAnswers)
            .mustEqual(routes.SoldGoodsFromNiController.onPageLoad(CheckMode, emptyUserAnswers.period))
        }
      }
    }
  }
}

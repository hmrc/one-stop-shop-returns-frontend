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
import models.{Country, NormalMode, SalesAtVatRate, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class DeleteSalesFromEuPageSpec extends PageBehaviours {

  "DeleteSalesFromEuPage" - {

    beRetrievable[Boolean](DeleteSalesFromEuPage(index))

    beSettable[Boolean](DeleteSalesFromEuPage(index))

    beRemovable[Boolean](DeleteSalesFromEuPage(index))

    "must navigate in Normal mode" - {

      "when there are sales from at least one EU country in user answers" - {

        "to Sales from EU list" in {

          val countryFrom  = arbitrary[Country].sample.value
          val countryTo    = arbitrary[Country].sample.value
          val vatRate      = arbitrary[VatRate].sample.value
          val salesDetails = arbitrary[SalesAtVatRate].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
              .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
              .set(VatRatesFromEuPage(index, index), List(vatRate)).success.value
              .set(SalesAtVatRateFromEuPage(index, index, index), salesDetails).success.value

          DeleteSalesFromEuPage(index).navigate(NormalMode, answers)
            .mustEqual(routes.SalesFromEuListController.onPageLoad(NormalMode, answers.period))
        }
      }

      "when there aer no sales from EU in user answers" - {

        "to Sold Goods from EU" in {

          DeleteSalesFromEuPage(index).navigate(NormalMode, emptyUserAnswers)
            .mustEqual(routes.SoldGoodsFromEuController.onPageLoad(NormalMode, emptyUserAnswers.period))
        }
      }
    }
  }
}

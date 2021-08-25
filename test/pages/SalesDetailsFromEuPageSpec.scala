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
import models.{Index, NormalMode, SalesAtVatRate, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.behaviours.PageBehaviours

class SalesDetailsFromEuPageSpec extends PageBehaviours {

  "SalesDetailsFromEuPage" - {

    beRetrievable[SalesAtVatRate](SalesDetailsFromEuPage(index, index, index))

    beSettable[SalesAtVatRate](SalesDetailsFromEuPage(index, index, index))

    beRemovable[SalesAtVatRate](SalesDetailsFromEuPage(index, index, index))

    "must navigate in Normal mode" - {

      "when there are more VAT rates to get answers for" - {

        "to Sales Details from EU for the next index" in {

          val countryIndex = Index(0)

          val vatRates = Gen.listOfN(2, arbitrary[VatRate]).sample.value

          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryIndex, countryIndex), vatRates).success.value

          SalesDetailsFromEuPage(countryIndex, countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.SalesDetailsFromEuController.onPageLoad(NormalMode, answers.period, countryIndex, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to get answers for" - {

        "to Check Sales to EU" in {

          val countryIndex = Index(0)

          val vatRate = arbitrary[VatRate].sample.value
          val answers =
            emptyUserAnswers
              .set(VatRatesFromEuPage(countryIndex, countryIndex), List(vatRate)).success.value

          SalesDetailsFromEuPage(countryIndex, countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.CheckSalesToEuController.onPageLoad(NormalMode, answers.period, countryIndex, countryIndex))
        }
      }
    }
  }
}

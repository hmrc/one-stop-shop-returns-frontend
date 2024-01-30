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
import models.{CheckMode, CheckThirdLoopMode, Country, Index, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class SalesToEuListPageSpec extends PageBehaviours {

  "SalesToEuList page" - {

    "must navigate in Normal mode" - {

      "when the answer is Yes" - {

        "to Country of Consumption with an index equal to the number of countries we have details for" in {

          val countryFrom = arbitrary[Country].sample.value
          val countryTo   = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
              .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

          SalesToEuListPage(index).navigate(answers, NormalMode, addAnother = true)
            .mustEqual(routes.CountryOfConsumptionFromEuController.onPageLoad(NormalMode, answers.period, index, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Sales from EU List" in {

          SalesToEuListPage(index).navigate(emptyUserAnswers, NormalMode, addAnother = false)
            .mustEqual(routes.SalesFromEuListController.onPageLoad(NormalMode, emptyUserAnswers.period))
        }
      }
    }

    "must navigate in Check mode" - {

      "when the answer is Yes" - {

        "to Country of Consumption with an index equal to the number of countries we have details for" in {

          val countryFrom = arbitrary[Country].sample.value
          val countryTo   = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
              .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

          SalesToEuListPage(index).navigate(answers, CheckMode, addAnother = true)
            .mustEqual(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, answers.period, index, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Sales from EU List" in {

          SalesToEuListPage(index).navigate(emptyUserAnswers, CheckMode, addAnother = false)
            .mustEqual(routes.SalesFromEuListController.onPageLoad(CheckMode, emptyUserAnswers.period))
        }
      }
    }

    "must navigate in Check Third Loop mode" - {

    "when the answer is Yes" - {

      "to Country of Consumption with an index equal to the number of countries we have details for" in {

        val countryFrom = arbitrary[Country].sample.value
        val countryTo   = arbitrary[Country].sample.value

        val answers =
          emptyUserAnswers
            .set(CountryOfSaleFromEuPage(index), countryFrom).success.value
            .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value

        SalesToEuListPage(index).navigate(answers, CheckThirdLoopMode, addAnother = true)
          .mustEqual(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckThirdLoopMode, answers.period, index, Index(1)))
      }
    }

    "when the answer is no" - {

      "to Sales from EU List" in {

        SalesToEuListPage(index).navigate(emptyUserAnswers, CheckThirdLoopMode, addAnother = false)
          .mustEqual(routes.SalesFromEuListController.onPageLoad(NormalMode, emptyUserAnswers.period))
      }
    }
  }
  }
}

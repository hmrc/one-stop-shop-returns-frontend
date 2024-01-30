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
import models.{CheckMode, Country, Index, NormalMode}
import pages.behaviours.PageBehaviours


class CountryOfSaleFromEuPageSpec extends PageBehaviours {

  "CountryOfSaleFromEuPage" - {

    beRetrievable[Country](CountryOfSaleFromEuPage(index))

    beSettable[Country](CountryOfSaleFromEuPage(index))

    beRemovable[Country](CountryOfSaleFromEuPage(index))

    "must navigate in Normal mode" - {

      "to Country of Consumption from EU" in {

        CountryOfSaleFromEuPage(Index(0)).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.CountryOfConsumptionFromEuController.onPageLoad(NormalMode, emptyUserAnswers.period, Index(0), Index(0)))
      }
    }

    "must navigate in Check mode" - {

      "to Country of Consumption from EU" in {

        CountryOfSaleFromEuPage(Index(0)).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, emptyUserAnswers.period, Index(0), Index(0)))
      }
    }
  }
}

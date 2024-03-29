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
import pages.behaviours.PageBehaviours


class CountryOfConsumptionFromEuPageSpec extends PageBehaviours {

  "CountryOfConsumptionFromEuPage" - {

    beRetrievable[Country](CountryOfConsumptionFromEuPage(index, index))

    beSettable[Country](CountryOfConsumptionFromEuPage(index, index))

    beRemovable[Country](CountryOfConsumptionFromEuPage(index, index))

    "must navigate in Normal mode" - {

      "to Vat Rates from EU" in {

        CountryOfConsumptionFromEuPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.VatRatesFromEuController.onPageLoad(NormalMode, emptyUserAnswers.period, Index(0), Index(0)))
      }
    }

    "must navigate in Check mode" - {

      "to Vat Rates from EU" in {

        CountryOfConsumptionFromEuPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.VatRatesFromEuController.onPageLoad(CheckMode, emptyUserAnswers.period, Index(0), Index(0)))
      }
    }

    "must navigate in Check Third Loop mode" - {

      "to Vat Rates from EU" in {

        CountryOfConsumptionFromEuPage(index, index).navigate(CheckThirdLoopMode, emptyUserAnswers)
          .mustEqual(routes.VatRatesFromEuController.onPageLoad(CheckThirdLoopMode, emptyUserAnswers.period, Index(0), Index(0)))
      }
    }
  }
}

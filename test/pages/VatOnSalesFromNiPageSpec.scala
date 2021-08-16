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
import models.{Index, NormalMode}
import models.VatRatesFromNi.{Option1, Option2}
import pages.behaviours.PageBehaviours

class VatOnSalesFromNiPageSpec extends PageBehaviours {

  "VatOnSalesFromNiPage" - {

    beRetrievable[Int](VatOnSalesFromNiPage(index, index))

    beSettable[Int](VatOnSalesFromNiPage(index, index))

    beRemovable[Int](VatOnSalesFromNiPage(index, index))

    "must navigate in Normal Mode" - {

      "when there is another VAT rate to collect answers for" - {

        "to Net Value of Sales for the next index" in {

          val countryIndex = Index(0)

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(Option1, Option2)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.NetValueOfSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex, Index(1)))
        }
      }

      "when there are no more VAT rates to collect answers for" - {

        "to Check Sales From NI" in {

          val countryIndex = Index(0)

          val answers =
            emptyUserAnswers
              .set(VatRatesFromNiPage(countryIndex), List(Option1)).success.value

          VatOnSalesFromNiPage(countryIndex, Index(0)).navigate(NormalMode, answers)
            .mustEqual(routes.CheckSalesFromNiController.onPageLoad(NormalMode, answers.period, countryIndex))
        }
      }
    }
  }
}

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

import base.SpecBase
import controllers.routes
import models.{Index, NormalMode}

class SalesFromNiListPageSpec extends SpecBase {

  "SalesFromNiListPage" - {

    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to Country of Consumption with an index equal to the number of countries we have detials for" in {

          val answers
          = emptyUserAnswers
            .set(CountryOfConsumptionFromNiPage(Index(0)), "foo").success.value

          SalesFromNiListPage.navigate(answers, NormalMode, addAnother = true)
            .mustEqual(routes.CountryOfConsumptionFromNiController.onPageLoad(NormalMode, emptyUserAnswers.period, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Check Your Answers" in {

          SalesFromNiListPage.navigate(emptyUserAnswers, NormalMode, addAnother = false)
            .mustEqual(routes.CheckYourAnswersController.onPageLoad(emptyUserAnswers.period))
        }
      }
    }
  }
}

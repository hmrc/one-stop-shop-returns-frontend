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

package pages.corrections

import controllers.routes
import models.NormalMode
import pages.behaviours.PageBehaviours

class CorrectPreviousReturnPageSpec extends PageBehaviours {

  "CorrectPreviousReturnPage" - {

    beRetrievable[Boolean](CorrectPreviousReturnPage)

    beSettable[Boolean](CorrectPreviousReturnPage)

    beRemovable[Boolean](CorrectPreviousReturnPage)

    "must navigate in Normal mode" - {

      "to Which return period do you want to correct when the answer is yes" in {

        val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

        CorrectPreviousReturnPage.navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, answers.period))
      }

      "to Check your answers when the answer is no" in {

        val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, false).success.value

        CorrectPreviousReturnPage.navigate(NormalMode, answers)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }
    }
  }
}

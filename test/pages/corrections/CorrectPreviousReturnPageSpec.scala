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
import models.{CheckMode, Index, NormalMode}
import pages.behaviours.PageBehaviours

class CorrectPreviousReturnPageSpec extends PageBehaviours {

  "CorrectPreviousReturnPage" - {

    beRetrievable[Boolean](CorrectPreviousReturnPage)

    beSettable[Boolean](CorrectPreviousReturnPage)

    beRemovable[Boolean](CorrectPreviousReturnPage)

    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to the single version of the periods page when there is only a single period" in {

          val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

          CorrectPreviousReturnPage.navigate(NormalMode, answers, 1)
            .mustEqual(controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, answers.period,Index(0)))
        }

        "to Which return period do you want to correct when there are multiple periods" in {

          val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

          CorrectPreviousReturnPage.navigate(NormalMode, answers, 2)
            .mustEqual(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, answers.period, Index(0)))
        }
      }

      "to Check your answers when the answer is no" in {

        val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, false).success.value

        CorrectPreviousReturnPage.navigate(NormalMode, answers, 0)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }
    }

    "must navigate in Check mode" - {

      "when the answer is yes" - {

        "to the single version of the periods page when there is only a single period" in {

          val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

          CorrectPreviousReturnPage.navigate(CheckMode, answers, 1)
            .mustEqual(controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(CheckMode, answers.period,Index(0)))
        }

        "to Which return period do you want to correct when there are multiple periods" in {

          val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, true).success.value

          CorrectPreviousReturnPage.navigate(CheckMode, answers, 2)
            .mustEqual(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(CheckMode, answers.period, Index(0)))
        }
      }

      "to Check your answers when the answer is no" in {

        val answers = emptyUserAnswers.set(CorrectPreviousReturnPage, false).success.value

        CorrectPreviousReturnPage.navigate(CheckMode, answers, 0)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }
    }
  }
}

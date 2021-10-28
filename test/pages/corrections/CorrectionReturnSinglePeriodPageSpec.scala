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
import models.{Index, NormalMode, Period}
import pages.behaviours.PageBehaviours

class CorrectionReturnSinglePeriodPageSpec extends PageBehaviours {

  "CorrectionReturnSinglePeriodPage" - {

    beRetrievable[Boolean](CorrectionReturnSinglePeriodPage)

    beSettable[Boolean](CorrectionReturnSinglePeriodPage)

    beRemovable[Boolean](CorrectionReturnSinglePeriodPage)

    "must navigate in Normal mode" - {

      "to Which country would you like to correct page when answer is true" in {

        val answers = emptyUserAnswers.set(CorrectionReturnSinglePeriodPage, true).success.value

        CorrectionReturnSinglePeriodPage.navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, answers.period, Index(0)))
      }

      "to Which country would you like to correct page when answer is false" in {

        val answers = emptyUserAnswers.set(CorrectionReturnSinglePeriodPage, false).success.value

        CorrectionReturnSinglePeriodPage.navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(answers.period))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionReturnSinglePeriodPage.navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{CheckMode, Index, NormalMode, Period}
import pages.behaviours.PageBehaviours

class CorrectionReturnPeriodPageSpec extends PageBehaviours {

  "CorrectionReturnPeriodPage" - {

    beRetrievable[Period](CorrectionReturnPeriodPage(Index(0)))

    beSettable[Period](CorrectionReturnPeriodPage(Index(0)))

    beRemovable[Period](CorrectionReturnPeriodPage(Index(0)))

    "must navigate in Normal mode" - {

      "to Which country would you like to correct page when answer is valid" in {

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), period).success.value

        CorrectionReturnPeriodPage(Index(0)).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, answers.period, Index(0), Index(0)))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionReturnPeriodPage(Index(0)).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in Check mode" - {

      "to Which country would you like to correct page when answer is valid" in {

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), period).success.value

        CorrectionReturnPeriodPage(Index(0)).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(CheckMode, answers.period, Index(0), Index(0)))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionReturnPeriodPage(Index(0)).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

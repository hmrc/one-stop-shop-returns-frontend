/*
 * Copyright 2023 HM Revenue & Customs
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

import models.{CheckMode, Index, NormalMode, Quarter, StandardPeriod}
import org.scalacheck.Arbitrary._
import pages.behaviours.PageBehaviours

class RemovePeriodCorrectionPageSpec extends PageBehaviours {

  "RemovePeriodCorrectionPage" - {

    beRetrievable[Boolean](RemovePeriodCorrectionPage(index))

    beSettable[Boolean](RemovePeriodCorrectionPage(index))

    beRemovable[Boolean](RemovePeriodCorrectionPage(index))

    "must navigate in Normal mode" - {

      "to VatPeriodCorrectionsList page when there is a correction period present" in {

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value

        RemovePeriodCorrectionPage(index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, answers.period))
      }

      "to VatPeriodCorrectionsList page when there are multiple correction periods present" in {

        val period1 = StandardPeriod(2021, Quarter.Q4)

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionReturnPeriodPage(Index(1)), period1).success.value

        RemovePeriodCorrectionPage(Index(1)).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, answers.period))
      }

      "to CorrectPreviousReturn page when there are no correction periods present" in {

        val answers = emptyUserAnswers

        RemovePeriodCorrectionPage(index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, answers.period))
      }

    }

    "must navigate in Check mode" - {

      "to VatPeriodCorrectionsList page when there is a correction period present" in {

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value

        RemovePeriodCorrectionPage(index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period))
      }

      "to VatPeriodCorrectionsList page when there are multiple correction periods present" in {

        val period1 = StandardPeriod(2021, Quarter.Q4)

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionReturnPeriodPage(Index(1)), period1).success.value

        RemovePeriodCorrectionPage(Index(1)).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period))
      }

      "to CorrectPreviousReturn page when there are no correction periods present" in {

        val answers = emptyUserAnswers

        RemovePeriodCorrectionPage(index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(CheckMode, answers.period))
      }

    }
  }
}

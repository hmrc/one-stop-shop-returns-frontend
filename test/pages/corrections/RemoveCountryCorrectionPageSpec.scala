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

import models.{CheckMode, Country, Index, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class RemoveCountryCorrectionPageSpec extends PageBehaviours {

  "RemoveCountryCorrectionPage" - {

    beRetrievable[Boolean](RemoveCountryCorrectionPage(index))

    beSettable[Boolean](RemoveCountryCorrectionPage(index))

    beRemovable[Boolean](RemoveCountryCorrectionPage(index))

    "must navigate in Normal mode" - {

      "to VatCorrectionsList page when there are still correction countries for the period present" in {

        val country1  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), country1).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        RemoveCountryCorrectionPage(index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.VatCorrectionsListController.onPageLoad(NormalMode, answers.period, index))
      }

      "to VatPeriodCorrectionsList page when there are no correction countries for the period present and there are other correction periods already present" in {

        val country1  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), country1).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        RemoveCountryCorrectionPage(Index(1)).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, answers.period))
      }

      "to CorrectPreviousReturn page when there are no correction countries for the period present and there are no other correction periods present" in {

        val answers = emptyUserAnswers

        RemoveCountryCorrectionPage(index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, answers.period))
      }

    }

    "must navigate in Check mode" - {

      "to VatCorrectionsList page when there are still correction countries for the period present" in {

        val country1  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), country1).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        RemoveCountryCorrectionPage(index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.VatCorrectionsListController.onPageLoad(CheckMode, answers.period, index))
      }

      "to VatPeriodCorrectionsList page when there are no correction countries for the period present and there are other correction periods already present" in {

        val country1  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), country1).success.value
          .set(CountryVatCorrectionPage(index, index), BigDecimal(10)).success.value

        RemoveCountryCorrectionPage(Index(1)).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, answers.period))
      }

      "to CorrectPreviousReturn page when there are no correction countries for the period present and there are no other correction periods present" in {

        val answers = emptyUserAnswers

        RemoveCountryCorrectionPage(index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(CheckMode, answers.period))
      }

    }
  }
}

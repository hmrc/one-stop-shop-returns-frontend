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
import models.{CheckMode, CheckThirdLoopMode, Country, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class UndeclaredCountryCorrectionPageSpec extends PageBehaviours {

  "UndeclaredCountryCorrectionPage" - {

    beRetrievable[Boolean](UndeclaredCountryCorrectionPage(index, index))

    beSettable[Boolean](UndeclaredCountryCorrectionPage(index, index))

    beRemovable[Boolean](UndeclaredCountryCorrectionPage(index, index))

    "must navigate in Normal mode" - {

      "to What is your correction for the total VAT payable page when answer is yes" in {

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), true).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, answers.period, index, index))
      }

      "to Correction country page when answer is no" in {

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), false).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(NormalMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        UndeclaredCountryCorrectionPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in Check mode" - {

      "to What is your correction for the total VAT payable page when answer is yes" in {

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), true).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckMode, answers.period, index, index))
      }

      "to Correction country page when answer is no" in {

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), false).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(CheckMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in CheckThirdLoop mode" - {

      "to What is your correction for the total VAT payable page when answer is yes" in {

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), true).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckThirdLoopMode, answers.period, index, index))
      }

      "to Correction country page when answer is no" in {

        val country  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(UndeclaredCountryCorrectionPage(index, index), false).success.value

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CorrectionCountryController.onPageLoad(CheckThirdLoopMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        UndeclaredCountryCorrectionPage(index, index).navigate(CheckThirdLoopMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  }
}

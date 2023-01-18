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

import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Index, NormalMode}
import pages.behaviours.PageBehaviours

class VatPayableForCountryPageSpec extends PageBehaviours {

  "VatPayableForCountryPage" - {

    beRetrievable[Boolean](VatPayableForCountryPage(Index(0), Index(0)))

    beSettable[Boolean](VatPayableForCountryPage(Index(0), Index(0)))

    beRemovable[Boolean](VatPayableForCountryPage(Index(0), Index(0)))

    "must navigate in Normal mode" - {

      "to CheckVatPayableAmount page when answer is true" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), true).success.value

        VatPayableForCountryPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(NormalMode, answers.period, index, index))
      }

      "to CountryVatCorrection page when answer is false" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), false).success.value

        VatPayableForCountryPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, answers.period, index, index, false))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in Check mode" - {

      "to CheckVatPayableAmount page when answer is true" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), true).success.value

        VatPayableForCountryPage(index, index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(CheckMode, answers.period, index, index))
      }

      "to CountryVatCorrection page when answer is false" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), false).success.value

        VatPayableForCountryPage(index, index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckMode, answers.period, index, index, false))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in CheckLoop mode" - {

      "to CheckVatPayableAmount page when answer is true" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), true).success.value

        VatPayableForCountryPage(index, index).navigate(CheckLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(NormalMode, answers.period, index, index))
      }

      "to CountryVatCorrection page when answer is false" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), false).success.value

        VatPayableForCountryPage(index, index).navigate(CheckLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckLoopMode, answers.period, index, index, false))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(CheckLoopMode, emptyUserAnswers)
          .mustEqual(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in CheckSecondLoop mode" - {

      "to CheckSecondLoopVatPayableAmount page when answer is true" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), true).success.value

        VatPayableForCountryPage(index, index).navigate(CheckSecondLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(CheckSecondLoopMode, answers.period, index, index))
      }

      "to CountryVatCorrection page when answer is false" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), false).success.value

        VatPayableForCountryPage(index, index).navigate(CheckSecondLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckSecondLoopMode, answers.period, index, index, false))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(CheckSecondLoopMode, emptyUserAnswers)
          .mustEqual(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in CheckThirdLoop mode" - {

      "to CheckThirdLoopVatPayableAmount page when answer is true" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), true).success.value

        VatPayableForCountryPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CheckVatPayableAmountController.onPageLoad(CheckThirdLoopMode, answers.period, index, index))
      }

      "to CountryVatCorrection page when answer is false" in {

        val answers = emptyUserAnswers.set(VatPayableForCountryPage(index, index), false).success.value

        VatPayableForCountryPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckThirdLoopMode, answers.period, index, index, false))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(CheckThirdLoopMode, emptyUserAnswers)
          .mustEqual(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

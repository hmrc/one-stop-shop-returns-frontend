/*
 * Copyright 2024 HM Revenue & Customs
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
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}


class CorrectionCountryPageSpec extends PageBehaviours {

  "CorrectionCountryPage" - {

    beRetrievable[Country](CorrectionCountryPage(index, index))

    beSettable[Country](CorrectionCountryPage(index, index))

    beRemovable[Country](CorrectionCountryPage(index, index))

    "must navigate in Normal mode" - {

      "to What is your correction for the total VAT payable page when answer is valid and the country was present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val previouslyDeclaredCorrectionAmount: BigDecimal = BigDecimal(1500)

        val answers = emptyUserAnswers
          .set(CorrectionCountryPage(index, index), country).success.value
          .set(
            PreviouslyDeclaredCorrectionAmountQuery(index, index),
            PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = previouslyDeclaredCorrectionAmount)
          ).success.value

        CorrectionCountryPage(index, index).navigate(NormalMode, answers)
          .mustEqual(
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              NormalMode, answers.period, index, index, undeclaredCountry = false
            )
          )
      }

      "to Undeclared country page when answer is valid and the country was not present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(NormalMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionCountryPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in Check mode" - {

      "to What is your correction for the total VAT payable page when answer is valid and the country was present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val previouslyDeclaredCorrectionAmount: BigDecimal = BigDecimal(1500)

        val answers = emptyUserAnswers
          .set(CorrectionCountryPage(index, index), country).success.value
          .set(
            PreviouslyDeclaredCorrectionAmountQuery(index, index),
            PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = previouslyDeclaredCorrectionAmount)
          ).success.value

        CorrectionCountryPage(index, index).navigate(CheckMode, answers)
          .mustEqual(
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              CheckMode, answers.period, index, index, undeclaredCountry = false
            )
          )
      }

      "to Undeclared country page when answer is valid and the country was not present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(CheckMode, answers)
          .mustEqual(controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(CheckMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionCountryPage(index, index).navigate(CheckMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }

    "must navigate in CheckThirdLoop mode" - {

      "to What is your correction for the total VAT payable page when answer is valid and the country was present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val previouslyDeclaredCorrectionAmount: BigDecimal = BigDecimal(1500)

        val answers = emptyUserAnswers
          .set(CorrectionCountryPage(index, index), country).success.value
          .set(
            PreviouslyDeclaredCorrectionAmountQuery(index, index),
            PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = previouslyDeclaredCorrectionAmount)
          ).success.value

        CorrectionCountryPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(
            controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(
              CheckThirdLoopMode, answers.period, index, index, undeclaredCountry = false
            )
          )
      }

      "to Undeclared country page when answer is valid and the country was not present in the previous Vat return" in {

        val country = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(CheckThirdLoopMode, answers)
          .mustEqual(controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(CheckThirdLoopMode, answers.period, index, index))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionCountryPage(index, index).navigate(CheckThirdLoopMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

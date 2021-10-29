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
import models.{Country, Index, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours


class CorrectionCountryPageSpec extends PageBehaviours {

  "CorrectionCountryPage" - {

    beRetrievable[Country](CorrectionCountryPage(index, index))

    beSettable[Country](CorrectionCountryPage(index, index))

    beRemovable[Country](CorrectionCountryPage(index, index))

    "must navigate in Normal mode" - {

      "to What is your correction for the total VAT payable page when answer is valid and the country was present in the previous Vat return for NI" in {

        val country  = arbitrary[Country].sample.value
        val countries = Seq(country)

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(NormalMode, answers, countries, Seq.empty)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, answers.period))
      }

      "to What is your correction for the total VAT payable page when answer is valid and the country was present in the previous Vat return for EU" in {

        val country  = arbitrary[Country].sample.value
        val countries = Seq(country)

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(NormalMode, answers, Seq.empty, countries)
          .mustEqual(controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(NormalMode, answers.period))
      }

      "to Undeclared country page when answer is valid and the country was not present in the previous Vat return" in {

        val country  = arbitrary[Country].sample.value

        val answers = emptyUserAnswers.set(CorrectionCountryPage(index, index), country).success.value

        CorrectionCountryPage(index, index).navigate(NormalMode, answers, Seq(), Seq())
          .mustEqual(controllers.corrections.routes.UndeclaredCountryCorrectionController.onPageLoad(NormalMode, answers.period))
      }

      "to Journey recovery page when answer is invalid" in {

        CorrectionCountryPage(index, index).navigate(NormalMode, emptyUserAnswers, Seq(), Seq())
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

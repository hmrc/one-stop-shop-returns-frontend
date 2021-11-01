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
import models.{Country, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class CountryVatCorrectionPageSpec extends PageBehaviours {

  "CountryVatCorrectionPage" - {

    beRetrievable[BigDecimal](CountryVatCorrectionPage(index, index))

    beSettable[BigDecimal](CountryVatCorrectionPage(index, index))

    beRemovable[BigDecimal](CountryVatCorrectionPage(index, index))

    "must navigate in Normal mode" - {

      "to Check your answers page when answer is valid" in {

        val answers = emptyUserAnswers.set(CountryVatCorrectionPage(index, index), BigDecimal(100)).success.value

        CountryVatCorrectionPage(index, index).navigate(NormalMode, answers)
          .mustEqual(controllers.corrections.routes.VatCorrectionsListController.onPageLoad(NormalMode, answers.period, index))
      }

      "to Journey recovery page when answer is invalid" in {

        CountryVatCorrectionPage(index, index).navigate(NormalMode, emptyUserAnswers)
          .mustEqual(routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }
}

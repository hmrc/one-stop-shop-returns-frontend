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

import controllers.corrections.routes
import models.{CheckMode, Country, Index, NormalMode}
import org.scalacheck.Arbitrary.arbitrary
import pages.behaviours.PageBehaviours

class VatCorrectionsListPageSpec extends PageBehaviours {

  "VatCorrectionsListPage" - {
    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to Correction Country with an index equal to the number of countries we have details for" in {

          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CorrectionCountryPage(index, Index(0)), country).success.value

          VatCorrectionsListPage(index).navigate(answers, NormalMode, addAnother = true)
            .mustEqual(routes.CorrectionCountryController.onPageLoad(NormalMode, answers.period, index, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Vat period corrections list page" in {

          VatCorrectionsListPage(index).navigate(emptyUserAnswers, NormalMode, addAnother = false)
            .mustEqual(routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, emptyUserAnswers.period))
        }
      }
    }

    "must navigate in Check mode" - {

      "when the answer is yes" - {

        "to Correction Country with an index equal to the number of countries we have details for" in {

          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CorrectionCountryPage(index, Index(0)), country).success.value

          VatCorrectionsListPage(index).navigate(answers, CheckMode, addAnother = true)
            .mustEqual(routes.CorrectionCountryController.onPageLoad(CheckMode, answers.period, index, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Vat period corrections list page" in {

          VatCorrectionsListPage(index).navigate(emptyUserAnswers, CheckMode, addAnother = false)
            .mustEqual(routes.VatPeriodCorrectionsListController.onPageLoad(CheckMode, emptyUserAnswers.period))
        }
      }
    }
  }
}

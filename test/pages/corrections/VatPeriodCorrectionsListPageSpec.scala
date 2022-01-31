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

import connectors.ReturnStatusConnector
import controllers.actions.AuthenticatedControllerComponents
import models.{Country, Index, NormalMode, Period}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.behaviours.PageBehaviours
import play.api.inject.bind

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success


class VatPeriodCorrectionsListPageSpec extends PageBehaviours {

  "VatPeriodCorrectionsListPage" - {
    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to CorrectionReturnPeriod when there are already some corrections" in {

          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CorrectionReturnPeriodPage(index), period).success.value

          VatPeriodCorrectionsListPage.navigate(NormalMode, answers, addAnother = true)
            .mustEqual(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, answers.period, Index(1)))
        }

        "to CorrectionReturnPeriod when there are no corrections" in {

          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers

          VatPeriodCorrectionsListPage.navigate(NormalMode, answers, addAnother = true)
            .mustEqual(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, answers.period, Index(0)))
        }
      }

      "when the answer is no" - {

        "to CheckYourAnswers" in {
          val answers =
            emptyUserAnswers
              .set(CorrectionReturnPeriodPage(index), period).success.value
          VatPeriodCorrectionsListPage.navigate(NormalMode, answers, addAnother = false)
            .mustEqual(controllers.routes.CheckYourAnswersController.onPageLoad(period))
        }
      }
    }

    "cleanup" -{
      "must delete empty periods" in {
        val mockReturnStatusConnector = mock[ReturnStatusConnector]

        val answers = emptyUserAnswers
          .set(CorrectionReturnPeriodPage(index), Period("2021", "Q3").get).success.value
          .set(CorrectionReturnPeriodPage(Index(1)), Period("2022", "Q1").get).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()
        val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

        val result = VatPeriodCorrectionsListPage.cleanup(answers, cc)
        result.futureValue mustEqual(Success(emptyUserAnswers))
      }
    }
  }
}

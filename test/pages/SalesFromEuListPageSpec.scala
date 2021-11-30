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

package pages

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.{CheckMode, Country, Index, NormalMode}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

class SalesFromEuListPageSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAppConfig)
  }

  "SalesFromEuList page" - {

    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to Country of Sale with an index equal to the number of countries we have details for" in {

          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfSaleFromEuPage(Index(0)), country).success.value

          SalesFromEuListPage.navigate(answers, NormalMode, addAnother = true, mockAppConfig)
            .mustEqual(routes.CountryOfSaleFromEuController.onPageLoad(NormalMode, answers.period, Index(1)))
        }
      }

      "when the answer is no" - {

        "to Check your answers when the Corrections toggle is false" in {

          when(mockAppConfig.correctionToggle).thenReturn(false)

          SalesFromEuListPage.navigate(emptyUserAnswers, NormalMode, addAnother = false, mockAppConfig)
            .mustEqual(routes.CheckYourAnswersController.onPageLoad(emptyUserAnswers.period))
        }

        "to Do you want to correct a previous return page when the Corrections toggle is true" in {

          when(mockAppConfig.correctionToggle).thenReturn(true)

          SalesFromEuListPage.navigate(emptyUserAnswers, NormalMode, addAnother = false, mockAppConfig)
            .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, emptyUserAnswers.period))
        }
      }
    }

    "must navigate in Check mode" - {

      "when the answer is yes" - {

        "to Country of Sale with an index equal to the number of countries we have details for" in {
          val country = arbitrary[Country].sample.value

          val answers =
            emptyUserAnswers
              .set(CountryOfSaleFromEuPage(Index(0)), country).success.value

          SalesFromEuListPage.navigate(answers, CheckMode, addAnother = true, mockAppConfig)
            .mustEqual(routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, answers.period, Index(1)))
        }

      }

      "when the answer is no" - {

        "to Check your answers if correction toggle is false" in {
          when(mockAppConfig.correctionToggle).thenReturn(false)
          SalesFromEuListPage.navigate(emptyUserAnswers, CheckMode, addAnother = false, mockAppConfig)
            .mustEqual(routes.CheckYourAnswersController.onPageLoad(emptyUserAnswers.period))
        }

        "to CheckYourAnswers and correction toggle is true" in {
          when(mockAppConfig.correctionToggle).thenReturn(true)
          val country = arbitrary[Country].sample.value
          SalesFromEuListPage.navigate(emptyUserAnswers, CheckMode, addAnother = false, mockAppConfig)
            .mustEqual(routes.CheckYourAnswersController.onPageLoad(emptyUserAnswers.period))
        }
      }
    }
  }
}

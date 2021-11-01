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
import models.{CheckMode, Index, NormalMode}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import pages.behaviours.PageBehaviours
import org.scalatestplus.mockito.MockitoSugar

class SoldGoodsFromEuPageSpec extends PageBehaviours with SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAppConfig)
  }

  "SoldGoodsFromEuPage" - {

    beRetrievable[Boolean](SoldGoodsFromEuPage)

    beSettable[Boolean](SoldGoodsFromEuPage)

    beRemovable[Boolean](SoldGoodsFromEuPage)

    "must navigate in Normal mode" - {

      "to Country of Sale from EU when the answer is yes" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, true).success.value

        SoldGoodsFromEuPage.navigate(NormalMode, answers, mockAppConfig)
          .mustEqual(routes.CountryOfSaleFromEuController.onPageLoad(NormalMode, answers.period, Index(0)))
      }

      "to Check your answers when the answer is no and the Corrections feature toggle is false" in {

        when(mockAppConfig.correctionToggle).thenReturn(false)

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, false).success.value

        SoldGoodsFromEuPage.navigate(NormalMode, answers, mockAppConfig)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }

      "to Do you want to correct a previous return page when the answer is no and the Corrections feature toggle is true" in {

        when(mockAppConfig.correctionToggle).thenReturn(true)

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, false).success.value

        SoldGoodsFromEuPage.navigate(NormalMode, answers, mockAppConfig)
          .mustEqual(controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(NormalMode, answers.period))
      }

    }

    "must navigate in Check mode" - {

      "to Country of Sale from EU when the answer is yes" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, true).success.value

        SoldGoodsFromEuPage.navigate(CheckMode, answers)
          .mustEqual(routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, answers.period, Index(0)))
      }

      "to Check your answers when the answer is no" in {

        val answers = emptyUserAnswers.set(SoldGoodsFromEuPage, false).success.value

        SoldGoodsFromEuPage.navigate(CheckMode, answers)
          .mustEqual(routes.CheckYourAnswersController.onPageLoad(answers.period))
      }
    }

    "cleanup" - {

      "must not remove anything when answers is yes" in {
        val result = SoldGoodsFromEuPage.cleanup(Some(true), completeUserAnswers).success.value

        result mustBe completeUserAnswers
      }

      "must remove all EU countries sales when answers is no" in {
        val answers = completeUserAnswers.set(SoldGoodsFromEuPage, false).success.value
        val result = SoldGoodsFromEuPage.cleanup(Some(false), answers).success.value
        val expectedUserAnswers = completeSalesFromNIUserAnswers.set(SoldGoodsFromEuPage, false).success.value

        result mustBe expectedUserAnswers
      }
    }
  }
}

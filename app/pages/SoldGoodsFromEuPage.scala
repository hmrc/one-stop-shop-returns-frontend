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

import config.FrontendAppConfig
import controllers.routes
import models.{CheckMode, Index, Mode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllSalesFromEuQuery

import scala.util.Try

case object SoldGoodsFromEuPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "soldGoodsFromEu"

  def navigate(mode: Mode, answers: UserAnswers, config: FrontendAppConfig): Call =
    answers.get(SoldGoodsFromEuPage) match {
      case Some(true)  => routes.CountryOfSaleFromEuController.onPageLoad(mode, answers.period, Index(0))
      case Some(false) => if(config.correctionToggle) {
        if(mode == CheckMode){
          routes.CheckYourAnswersController.onPageLoad(answers.period)
        }else{
          controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(mode, answers.period)
        }
      } else {
        routes.CheckYourAnswersController.onPageLoad(answers.period)
      }
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  override protected def navigateInCheckMode(answers: UserAnswers): Call = {
    answers.get(SoldGoodsFromEuPage) match {
      case Some(true)  => routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, answers.period, Index(0))
      case Some(false) => routes.CheckYourAnswersController.onPageLoad(answers.period)
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(false) => userAnswers.remove(AllSalesFromEuQuery)
      case _           => super.cleanup(value, userAnswers)
    }
  }
}

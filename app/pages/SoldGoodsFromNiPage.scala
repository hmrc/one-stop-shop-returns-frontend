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

import controllers.routes
import models.{CheckMode, Index, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllSalesFromNiQuery

import scala.util.{Success, Try}

case object SoldGoodsFromNiPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "soldGoodsFromNi"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    answers.get(SoldGoodsFromNiPage) match {
      case Some(true) => routes.CountryOfConsumptionFromNiController.onPageLoad(NormalMode, answers.period, Index(0))
      case _          => routes.IndexController.onPageLoad()
    }

  override def navigateInCheckMode(answers: UserAnswers): Call =
    answers.get(SoldGoodsFromNiPage) match {
      case Some(true) => routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, answers.period, Index(0)) // TODO index
      case _ => routes.CheckYourAnswersController.onPageLoad(answers.period)
    }

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(false) => userAnswers.remove(AllSalesFromNiQuery)
      case _ => super.cleanup(value, userAnswers)
    }
  }
}

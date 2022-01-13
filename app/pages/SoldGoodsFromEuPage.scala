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

package pages

import controllers.routes
import models.{CheckMode, Index, Mode, NormalMode, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.{AllSalesFromEuQuery, DeriveNumberOfSalesFromEu}

import scala.util.Try

case object SoldGoodsFromEuPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "soldGoodsFromEu"

  override def navigate(mode: Mode, answers: UserAnswers): Call = {
    val salesFromEu = answers.get(DeriveNumberOfSalesFromEu).getOrElse(0)

    (mode, answers.get(SoldGoodsFromEuPage)) match {
      case (CheckMode, Some(true)) if salesFromEu > 0 => routes.CheckYourAnswersController.onPageLoad(answers.period)
      case (CheckMode, Some(true)) => routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, answers.period, Index(0))
      case (NormalMode, Some(true)) => routes.CountryOfSaleFromEuController.onPageLoad(mode, answers.period, Index(0))
      case (CheckMode, Some(false)) => routes.CheckYourAnswersController.onPageLoad(answers.period)
      case (NormalMode, Some(false)) => controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(mode, answers.period)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  }


  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(false) => userAnswers.remove(AllSalesFromEuQuery)
      case _ => super.cleanup(value, userAnswers)
    }
  }
}

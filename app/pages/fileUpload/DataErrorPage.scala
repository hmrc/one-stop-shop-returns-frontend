/*
 * Copyright 2026 HM Revenue & Customs
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

package pages.fileUpload

import controllers.routes
import models.{CheckMode, NormalMode, UserAnswers}
import pages.QuestionPage
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object DataErrorPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "dataError"

  override def navigateInNormalMode(answers: UserAnswers): Call = {
    answers.get(this) match {
      case Some(true) =>
        controllers.fileUpload.routes.FileUploadController.onPageLoad(NormalMode, answers.period)
      case Some(false) =>
        routes.SoldGoodsFromNiController.onPageLoad(NormalMode, answers.period)
      case _ =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }

  override def navigateInCheckMode(answers: UserAnswers): Call = {
    answers.get(this) match {
      case Some(true) =>
        controllers.fileUpload.routes.FileUploadController.onPageLoad(CheckMode, answers.period)
      case Some(false) =>
        routes.SoldGoodsFromNiController.onPageLoad(CheckMode, answers.period)
      case _ =>
        routes.JourneyRecoveryController.onPageLoad()
    }
  }
}

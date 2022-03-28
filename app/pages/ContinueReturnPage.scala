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
import models.ContinueReturn.{Continue, Delete}
import models.{ContinueReturn, UserAnswers}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

case object ContinueReturnPage extends QuestionPage[ContinueReturn] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "continueReturn"

  def navigate(answers: UserAnswers, questionAnswer: ContinueReturn): Call =
    (questionAnswer, answers.get(SavedProgressPage)) match {
      case (Continue, Some(url)) => Call(GET, url)
      case (Delete, _) =>  controllers.routes.DeleteReturnController.onPageLoad(answers.period)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}

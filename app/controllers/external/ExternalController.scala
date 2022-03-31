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

package controllers.external

import controllers.actions.AuthenticatedControllerComponents
import models.{Period, SessionData}
import models.external.{ContinueReturn, ExternalRequest, ExternalResponse, ReturnsHistory, StartReturn, YourAccount}
import models.responses.NotFound
import play.api.{Logger, Logging}
import play.api.libs.json.{JsPath, JsValue, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{SessionRepository, UserAnswersRepository}
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExternalController @Inject()(
                                    sessionRepository: SessionRepository,
                                    cc: AuthenticatedControllerComponents
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with WithJsonBody with Logging {

  override protected def controllerComponents: MessagesControllerComponents = cc

  val key = (JsPath \ "returnUrl")

  def onExternal(page: String, period: Option[Period] = None): Action[JsValue] =  cc.authAndGetSavedAnswers.async(parse.json) {
    implicit request => withJsonBody[ExternalRequest] {
      externalRequest =>
      (page, period) match {
        case (YourAccount.name, None) =>
          saveReturnUrl(request.userId, externalRequest)
          Future.successful(Ok(Json.toJson(ExternalResponse(YourAccount.url))))
        case (ReturnsHistory.name, None) =>
          saveReturnUrl(request.userId, externalRequest)
          Future.successful(Ok(Json.toJson(ExternalResponse(ReturnsHistory.url))))
        case (StartReturn.name, Some(returnPeriod)) =>
          saveReturnUrl(request.userId, externalRequest)
          Future.successful(Ok(Json.toJson(ExternalResponse(StartReturn.url(returnPeriod)))))
        case (ContinueReturn.name, Some(returnPeriod)) =>
          saveReturnUrl(request.userId, externalRequest)
          Future.successful(Ok(Json.toJson(ExternalResponse(ContinueReturn.url(returnPeriod)))))

        case _ => Future.successful(NotFound)
      }
    }
  }

  private def saveReturnUrl(userId: String, externalRequest: ExternalRequest): Future[Boolean] = {
    for {
      sessionData <- sessionRepository.get(userId)
      updatedData <- Future.fromTry(sessionData.headOption.getOrElse(SessionData(userId)).set(key, externalRequest.returnUrl))
      savedData <- sessionRepository.set(updatedData)
    } yield {
      savedData
    }
  }.recover{
    case e: Exception =>
      logger.error(s"An error occurred while saving the external returnUrl in the session, ${e.getMessage}")
      false
  }
}

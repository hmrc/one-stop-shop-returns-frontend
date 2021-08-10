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

package controllers

import connectors.RegistrationConnector
import controllers.actions.AuthenticatedControllerComponents
import models.UserAnswers
import models.requests.OptionalDataRequest
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 registrationConnector: RegistrationConnector,
                                 view: IndexView
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetOptionalData.async {
    implicit request =>
      if (request.userAnswers.isDefined) {
        Ok(view()).toFuture
      } else {
        getAndCacheRegistrationDetails
      }
  }

  private def getAndCacheRegistrationDetails(
                                              implicit request: OptionalDataRequest[_],
                                              hc: HeaderCarrier
                                            ): Future[Result] =
    registrationConnector.get.flatMap {
      case Some(registration) =>
        val answers = UserAnswers(request.userId, registration, Json.obj())
        cc.sessionRepository.set(answers).map(_ => Ok(view()))
      case None =>
        Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
    }
}

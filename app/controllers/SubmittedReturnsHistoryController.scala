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

import connectors.VatReturnConnector
import controllers.actions._
import models.Quarter.Q3
import models.Period

import javax.inject.Inject
import play.api.i18n.I18nSupport
import logging.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmittedReturnsHistoryView
import models.responses.{NotFound => NotFoundResponse}

import scala.concurrent.ExecutionContext

class SubmittedReturnsHistoryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: SubmittedReturnsHistoryView,
                                       vatReturnConnector: VatReturnConnector
                                     ) (implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val period = new Period(2021, Q3)

  def onPageLoad: Action[AnyContent] = cc.authAndGetOptionalDataNoReturnCheck(period).async {
    implicit request =>
      vatReturnConnector.get(period).map {
        case Right(vatReturn) =>
          Ok(view(Some(vatReturn)))
        case Left(NotFoundResponse) =>
          Ok(view(None))
        case Left(e) =>
          logger.error(s"Unexpected result from api while getting return: ${e}")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }.recover {
        case e: Exception =>
          logger.error(s"Error while getting previous return: ${e.getMessage}", e)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }

  }
}

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

import connectors.ReturnStatusConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.SubmissionStatus
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       returnStatusConnector: ReturnStatusConnector,
                                       view: IndexView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
        case Right(availablePeriodsWithStatus) =>
          Ok(view(
            request.registration.registeredCompanyName,
            request.vrn.vrn,
            availablePeriodsWithStatus
              .filter(_.status == SubmissionStatus.Overdue)
              .map(_.period),
            availablePeriodsWithStatus
              .find(_.status == SubmissionStatus.Due)
              .map(_.period)
          ))
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }

  }
}

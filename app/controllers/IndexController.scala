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
import controllers.actions.AuthenticatedControllerComponents
import models.{Period, SubmissionStatus}
import models.Quarter.Q3
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import java.time.{LocalDate, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndexController @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 vatReturnConnector: VatReturnConnector,
                                 view: IndexView
                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      val year = 2021
      val period = Period(year, Q3)

      vatReturnConnector.get(period).map {
        case Right(_) =>
          SubmissionStatus.Complete
        case _ =>

          if(LocalDate.now(ZoneOffset.UTC).isAfter(period.paymentDeadline)) {
            SubmissionStatus.Overdue
          } else {
            SubmissionStatus.Due
          }
      }.map { submissionStatus =>
        Ok(view(
          request.registration.registeredCompanyName,
          request.vrn.vrn,
          period,
          submissionStatus
        ))
      }


  }
}

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

package controllers.corrections

import connectors.ReturnStatusConnector
import controllers.actions._
import forms.corrections.CorrectPreviousReturnFormProvider
import models.SubmissionStatus.Complete
import models.{Mode, Period}
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectPreviousReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectPreviousReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: CorrectPreviousReturnFormProvider,
                                       view: CorrectPreviousReturnView,
                                       returnStatusConnector: ReturnStatusConnector
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get(CorrectPreviousReturnPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period))),

        value =>
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectPreviousReturnPage, value))
        _ <- cc.sessionRepository.set(updatedAnswers)
        periods <- returnStatusConnector.listStatuses(request.registration.commencementDate)
      } yield {

        periods match {
          case Right(periods) => {
            val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)
            val allPeriods = periods.filter(_.status.equals(Complete)).map(_.period)
            val uncompletedCorrectionPeriods = allPeriods.diff(completedCorrectionPeriods).distinct
            Redirect(CorrectPreviousReturnPage.navigate(mode, updatedAnswers, uncompletedCorrectionPeriods.size))
          }
          case Left(value) =>
            logger.error(s"there was an error $value")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }

      }
    )
  }
}

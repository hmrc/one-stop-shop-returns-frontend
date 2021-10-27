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
import forms.corrections.CorrectionReturnSinglePeriodFormProvider
import models.SubmissionStatus.Complete
import models.{Index, Mode, NormalMode, Period}
import pages.corrections.CorrectionReturnSinglePeriodPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionReturnSinglePeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnSinglePeriodController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: CorrectionReturnSinglePeriodFormProvider,
                                       view: CorrectionReturnSinglePeriodView,
                                       returnStatusConnector: ReturnStatusConnector
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
        case Right(returnStatuses) =>
          val periods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          periods.size match {
            case 0 => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case 1 =>
              val preparedForm = request.userAnswers.get(CorrectionReturnSinglePeriodPage) match {
                case None => form
                case Some(value) => form.fill(value)
              }

              Ok(view(preparedForm, mode, period, periods.head.displayText))
            case _ => Redirect(
              controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period, Index(0))
            )
          }
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors => {
          returnStatusConnector.listStatuses(request.registration.commencementDate).map {
            case Right(returnStatuses) =>
              val periods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

              periods.size match {
                case 0 => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                case 1 => BadRequest(view(formWithErrors, mode, period, periods.head.displayText))
                case _ => Redirect(
                  controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(NormalMode, period, Index(0))
                )
              }

            case Left(value) =>
              logger.error(s"there was an error $value")
              throw new Exception(value.toString)
          }
        },

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnSinglePeriodPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CorrectionReturnSinglePeriodPage.navigate(mode, updatedAnswers))
      )
  }
}

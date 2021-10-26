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

import connectors.{ReturnStatusConnector, VatReturnConnector}
import controllers.actions._
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.SubmissionStatus.Complete
import models.{Mode, NormalMode, Period}
import pages.corrections.CorrectionReturnPeriodPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionReturnPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnPeriodController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: CorrectionReturnPeriodFormProvider,
                                       returnStatusConnector: ReturnStatusConnector,
                                       view: CorrectionReturnPeriodView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  private val form = formProvider()

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
          case Right(returnStatuses) =>
            val periods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

            if(periods.size < 2) {
              Redirect(
                controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period)
              )
            } else {
              val preparedForm = request.userAnswers.get(CorrectionReturnPeriodPage) match {
                case None => form
                case Some(value) => form.fill(value)
              }
              Ok(view(preparedForm, mode, period, periods))
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

              if(periods.size < 2) {
                Redirect(
                  controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(NormalMode, period)
                )
              } else {
                BadRequest(view(
                  formWithErrors, mode, period, returnStatuses.filter(_.status.equals(Complete)).map(_.period)
                ))
              }
            case Left(value) =>
              logger.error(s"there was an error $value")
              throw new Exception(value.toString)
        }},
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnPeriodPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield {
            Redirect(CorrectionReturnPeriodPage.navigate(mode, updatedAnswers))
          }
      )
  }
}

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
import models.{Index, Mode, Period}
import pages.corrections.{CorrectionReturnPeriodPage, CorrectionReturnSinglePeriodPage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
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

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period).async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
        case Right(returnStatuses) =>

          val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

          val allPeriods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          val uncompletedCorrectionPeriods = allPeriods.diff(completedCorrectionPeriods).distinct

          uncompletedCorrectionPeriods.size match {
            case 0 => Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(request.userAnswers.period))
            case 1 => Ok(view(form, mode, period, uncompletedCorrectionPeriods.head.displayText, index))
            case _ => Redirect(
              controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(mode, period, index)
            )
          }
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period).async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).flatMap {
        case Right(returnStatuses) =>

          val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

          val allPeriods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          val uncompletedCorrectionPeriods = allPeriods.diff(completedCorrectionPeriods).distinct

          uncompletedCorrectionPeriods.size match {
            case 0 => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case 1 => form.bindFromRequest().fold(
                formWithErrors => {
                  Future.successful(BadRequest(view(formWithErrors, mode, period, uncompletedCorrectionPeriods.head.displayText, index)))
                },
                value =>
                  if(value) {
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnPeriodPage(index), uncompletedCorrectionPeriods.head))
                      _              <- cc.sessionRepository.set(updatedAnswers)
                    } yield Redirect(CorrectionReturnSinglePeriodPage(index).navigate(mode, updatedAnswers, value))
                  } else {
                    Future.successful(Redirect(CorrectionReturnSinglePeriodPage(index).navigate(mode, request.userAnswers, value)))
                  }

              )
            case _ => Future.successful(Redirect(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(mode, period, index)))
          }

        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }
}

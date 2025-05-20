/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.actions.*
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.{Index, Mode, Period}
import pages.corrections.CorrectionReturnPeriodPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.AllCorrectionPeriodsQuery
import services.corrections.CorrectionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.*
import views.html.corrections.CorrectionReturnPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnPeriodController @Inject()(
                                                  cc: AuthenticatedControllerComponents,
                                                  formProvider: CorrectionReturnPeriodFormProvider,
                                                  correctionService: CorrectionService,
                                                  view: CorrectionReturnPeriodView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period).async {
    implicit request =>
      correctionService.getCorrectionPeriodsAndUncompleted().map { (allPeriodWithin3years, uncompletedCorrectionPeriods) =>

        if (uncompletedCorrectionPeriods.size < 2) {
          Redirect(
            controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(mode, period, index)
          )
        } else {
          val form = formProvider(index, allPeriodWithin3years, request.userAnswers
            .get(AllCorrectionPeriodsQuery).getOrElse(Seq.empty).map(_.correctionReturnPeriod))

          val preparedForm = request.userAnswers.get(CorrectionReturnPeriodPage(index)) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, mode, period, uncompletedCorrectionPeriods, index))
        }
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period).async {
    implicit request =>

      correctionService.getCorrectionPeriodsAndUncompleted().flatMap { (allPeriodWithin3years, uncompletedCorrectionPeriods) =>

        val form = formProvider(index, allPeriodWithin3years, request.userAnswers
          .get(AllCorrectionPeriodsQuery).getOrElse(Seq.empty).map(_.correctionReturnPeriod))

        form.bindFromRequest().fold(
          formWithErrors => {
            if (allPeriodWithin3years.size < 2) {
              Redirect(
                controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(mode, period, index)
              ).toFuture
            } else {
              BadRequest(view(
                formWithErrors, mode, period, uncompletedCorrectionPeriods, index
              )).toFuture
            }
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnPeriodPage(index), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(CorrectionReturnPeriodPage(index).navigate(mode, updatedAnswers))
            }
        )

      }
  }
}
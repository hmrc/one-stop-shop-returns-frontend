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
import forms.corrections.VatPeriodCorrectionsListFormProvider
import models.SubmissionStatus.Complete
import models.{Index, Mode, NormalMode, Period}
import models.Period._
import pages.corrections.VatPeriodCorrectionsListPage
import play.api.i18n.I18nSupport
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.VatPeriodCorrectionsListView
import views.html.corrections.VatPeriodAvailableCorrectionsListView
import pages.PageConstants.{correctionPeriod, corrections}
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPeriodAvailableCorrectionsListController @Inject()(
                                                    cc: AuthenticatedControllerComponents,
                                                    formProvider: VatPeriodCorrectionsListFormProvider,
                                                    view: VatPeriodAvailableCorrectionsListView,
                                                    returnStatusConnector: ReturnStatusConnector
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(VatPeriodCorrectionsListPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
        case Right(returnStatuses) =>
          val periods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          if(periods.isEmpty) {
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          } else {
            val completedCorrectionPeriods: List[Period] = (request.userAnswers.data \ corrections).asOpt[List[JsObject]]
              .map(json => json.flatMap(o => (o \ correctionPeriod).asOpt[Period])).getOrElse(List())

            val availableCorrectionPeriods = periods.diff(completedCorrectionPeriods).distinct

            if(availableCorrectionPeriods.isEmpty) {
              Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period))
            } else {
              Ok(view(preparedForm, mode, period, completedCorrectionPeriods))
            }
          }
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).flatMap {
        case Right(returnStatuses) =>
          val periods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          if(periods.isEmpty) {
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

          val completedCorrectionPeriods: List[Period] = (request.userAnswers.data \ corrections).asOpt[List[JsObject]]
            .map(json => json.flatMap(o => (o \ correctionPeriod).asOpt[Period])).getOrElse(List())

          val availableCorrectionPeriods = periods.diff(completedCorrectionPeriods).distinct

          if(availableCorrectionPeriods.isEmpty) {
            Future.successful(Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period)))
          } else {
            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, period, completedCorrectionPeriods))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(VatPeriodCorrectionsListPage, value))
                  _              <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(VatPeriodCorrectionsListPage.navigate(mode, updatedAnswers, value))
            )
          }
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }
}

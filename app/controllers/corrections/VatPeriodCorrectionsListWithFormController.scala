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
import models.{Mode, Period}
import pages.corrections.VatPeriodCorrectionsListPage
import play.api.Logging
import play.api.data.FormError
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.corrections.VatPeriodCorrectionsListSummary
import views.html.corrections.VatPeriodAvailableCorrectionsListView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPeriodCorrectionsListWithFormController @Inject()(
                                                    cc: AuthenticatedControllerComponents,
                                                    formProvider: VatPeriodCorrectionsListFormProvider,
                                                    view: VatPeriodAvailableCorrectionsListView,
                                                    returnStatusConnector: ReturnStatusConnector
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with CompletionChecks with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>

      returnStatusConnector.listStatuses(request.registration.commencementDate).map {
        case Right(returnStatuses) =>
          val allPeriods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

          if(allPeriods.isEmpty) {
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          } else {

            val completedCorrectionPeriods: List[Period] = request.userAnswers
              .get(DeriveCompletedCorrectionPeriods).getOrElse(List())

            val uncompletedCorrectionPeriods: List[Period] = allPeriods.diff(completedCorrectionPeriods).distinct.toList

            val completedCorrectionPeriodsModel: Seq[ListItem] = VatPeriodCorrectionsListSummary.getCompletedRows(request.userAnswers, mode)

            if(uncompletedCorrectionPeriods.isEmpty) {
              Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(mode, period))
            } else {
              withCompleteCorrections(onFailure = incompletePeriods => {
                val errors = incompletePeriods.map(period =>
                  FormError(period.toString, s"Please complete corrections for ${period.displayText}"))
                val formWithErrors = form.copy(errors = form.errors ++ errors)
                Ok(view(formWithErrors, mode, period, completedCorrectionPeriodsModel, incompletePeriods))
              })(Ok(view(form, mode, period, completedCorrectionPeriodsModel, List.empty)))
            }
          }
        case Left(value) =>
          logger.error(s"there was an error $value")
          throw new Exception(value.toString)
      }
  }

  def onSubmit(mode: Mode, period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      withCompleteCorrectionsAsync(onFailure = _ => Future.successful(Redirect(routes.VatPeriodCorrectionsListWithFormController.onPageLoad(mode, period)))) {
        returnStatusConnector.listStatuses(request.registration.commencementDate).flatMap {
          case Right(returnStatuses) =>

            val allPeriods = returnStatuses.filter(_.status.equals(Complete)).map(_.period)

            if(allPeriods.isEmpty) {
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }

            val completedCorrectionPeriods: List[Period] = request.userAnswers
              .get(DeriveCompletedCorrectionPeriods).getOrElse(List())

            val uncompletedCorrectionPeriods: List[Period] = allPeriods.diff(completedCorrectionPeriods).distinct.toList
            val completedCorrectionPeriodsModel: Seq[ListItem] = VatPeriodCorrectionsListSummary.getCompletedRows(request.userAnswers, mode)

            if(uncompletedCorrectionPeriods.isEmpty) {
              Future.successful(Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(mode, period)))
            } else {
              form.bindFromRequest().fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, period, completedCorrectionPeriodsModel,List.empty ))),
                value => {
                  VatPeriodCorrectionsListPage.cleanup(request.userAnswers, cc).map{
                    value =>
                      if(value) {
                        Redirect(VatPeriodCorrectionsListPage.navigate(mode, request.userAnswers, value))
                      } else{
                        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                      }
                  }

                }
              )
            }
          case Left(value) =>
            logger.error(s"there was an error $value")
            throw new Exception(value.toString)
        }
      }

  }
}

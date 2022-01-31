/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.actions._
import controllers.{routes => baseRoutes}
import forms.corrections.VatCorrectionsListFormProvider
import models.corrections.CorrectionToCountry
import models.{Country, Index, Mode, Period}
import pages.corrections.VatCorrectionsListPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.corrections.VatCorrectionsListSummary
import views.html.corrections.VatCorrectionsListView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class VatCorrectionsListController @Inject()(
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: VatCorrectionsListFormProvider,
                                              view: VatCorrectionsListView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with VatCorrectionsBaseController with CompletionChecks with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val form = formProvider()

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible(period) {
    implicit request =>
      getNumberOfCorrections(periodIndex) { (number, correctionPeriod) =>
        val canAddCountries = number < Country.euCountries.size
        val list = VatCorrectionsListSummary.addToListRows(request.userAnswers, mode, periodIndex)
        withCompleteData[CorrectionToCountry](
          periodIndex,
          data = getIncompleteCorrections _,
          onFailure = (incompleteCorrections: Seq[CorrectionToCountry]) => {
            Ok(view(form, mode, list, period, correctionPeriod, periodIndex, canAddCountries, incompleteCorrections.map(_.correctionCountry.name)))
          }) {
          Ok(view(form, mode, list, period, correctionPeriod, periodIndex, canAddCountries, Seq.empty))
        }
      }
  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible(period) { implicit request =>
      withCompleteData[CorrectionToCountry](
        periodIndex,
        data = getIncompleteCorrections _,
        onFailure = (incompleteCorrections: Seq[CorrectionToCountry]) => {
          if(incompletePromptShown) {
            firstIndexedIncompleteCorrection(periodIndex, incompleteCorrections) match {
              case Some(incompleteCorrection) =>
                Redirect(routes.CheckVatPayableAmountController.onPageLoad( mode,  period,  periodIndex, Index(incompleteCorrection._2)))
              case None =>
                Redirect(baseRoutes.JourneyRecoveryController.onPageLoad())
            }
          } else {
            Redirect(routes.VatCorrectionsListController.onPageLoad( mode,  period,  periodIndex))
          }
        })(
        onSuccess = {
          getNumberOfCorrections(periodIndex) { (number, correctionPeriod) =>
            val canAddCountries = number < Country.euCountries.size
            form.bindFromRequest().fold(
              formWithErrors => {
                val list = VatCorrectionsListSummary.addToListRows(request.userAnswers, mode, periodIndex)
                BadRequest(view(formWithErrors, mode, list, period, correctionPeriod, periodIndex, canAddCountries, Seq.empty))
              },
              value =>
                Redirect(VatCorrectionsListPage(periodIndex).navigate(request.userAnswers, mode, value))
            )
          }
        }
      )
  }
}

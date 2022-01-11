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
import models.{CheckSecondLoopMode, Index, Mode, NormalMode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.corrections.{CountryVatCorrectionSummary, NewVatTotalSummary, PreviousVatTotalSummary}
import viewmodels.govuk.summarylist._
import views.html.corrections.CheckVatPayableAmountView
import controllers.{routes => baseRoutes}
import models.corrections.CorrectionToCountry

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckVatPayableAmountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: CheckVatPayableAmountView,
                                       service: VatReturnService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with CompletionChecks with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible(period).async {
    implicit request =>
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))
      val newMode = if(mode == CheckSecondLoopMode){NormalMode} else mode
      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          for {
            originalAmount <- service.getLatestVatAmountForPeriodAndCountry(country, correctionPeriod)
          } yield {
            val summaryList = SummaryListViewModel(
              rows = Seq(
                Some(PreviousVatTotalSummary.row(originalAmount)),
                CountryVatCorrectionSummary.row(request.userAnswers, periodIndex, countryIndex, mode),
                NewVatTotalSummary.row(request.userAnswers, periodIndex, countryIndex, originalAmount)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            withCompleteData[CorrectionToCountry](
              periodIndex,
              data = getIncompleteCorrections _,
              onFailure = (_: Seq[CorrectionToCountry]) => {
              Ok(view(period, summaryList, country, newMode, correctionPeriod, periodIndex, countryIndex, countryCorrectionComplete = false))
            }) {
              Ok(view(period, summaryList, country, newMode, correctionPeriod, periodIndex, countryIndex, countryCorrectionComplete = true))
            }
          }
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible(period) { implicit request =>
      val incomplete = getIncompleteCorrectionToCountry(periodIndex, countryIndex)
      if(incomplete.isEmpty) {
        Redirect(controllers.corrections.routes.VatCorrectionsListController.onPageLoad(mode, period, periodIndex))
      } else {
          Redirect(routes.CorrectionCountryController.onPageLoad(
            mode,
            period,
            periodIndex,
            countryIndex))
      }
  }
}

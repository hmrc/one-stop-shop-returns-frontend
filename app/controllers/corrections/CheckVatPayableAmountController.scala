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

import controllers.actions._
import models.{Index, Mode, Period}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.corrections.{CountryVatCorrectionSummary, NewVatTotalSummary, PreviousVatTotalSummary}
import viewmodels.govuk.summarylist._
import views.html.corrections.CheckVatPayableAmountView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckVatPayableAmountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: CheckVatPayableAmountView,
                                       service: VatReturnService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionToggle(period).async {
    implicit request =>
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))

      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          for {
            originalAmount <- service.getVatOwedToCountryOnReturn(country, correctionPeriod)
          } yield {

            val summaryList = SummaryListViewModel(
              rows = Seq(
                Some(PreviousVatTotalSummary.row(originalAmount)),
                CountryVatCorrectionSummary.row(request.userAnswers, periodIndex, countryIndex),
                NewVatTotalSummary.row(request.userAnswers, periodIndex, countryIndex, originalAmount)
              ).flatten
            ).withCssClass("govuk-!-margin-bottom-9")

            Ok(view(period, summaryList, country, mode, correctionPeriod, periodIndex))
          }
      }
  }
}

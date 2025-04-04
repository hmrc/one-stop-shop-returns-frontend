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

package controllers

import controllers.actions.AuthenticatedControllerComponents
import models.{Index, Mode, Period, SalesFromCountryWithOptionalVat}
import pages.{CheckSalesToEuPage, VatRatesFromEuPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.CardTitle
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.TitledSummaryList
import viewmodels.checkAnswers.{NetValueOfSalesFromEuSummary, VatOnSalesFromEuSummary, VatRatesFromEuSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckSalesToEuView

import javax.inject.Inject

class CheckSalesToEuController @Inject()(
                                          cc: AuthenticatedControllerComponents,
                                          view: CheckSalesToEuView
                                        )
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountries(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val messages = messagesApi.preferred(request)

          val mainList = SummaryListViewModel(
            rows = Seq(VatRatesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, mode)).flatten
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(messages("vatRatesFromEu.checkYourAnswersLabel")))),
              actions = None
            )
          )

          val vatRateLists: Seq[TitledSummaryList] =
            request.userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map(_.zipWithIndex.map {
              case (vatRate, i) =>
                TitledSummaryList(
                  title = None,
                  list = SummaryListViewModel(
                    rows = Seq(
                      NetValueOfSalesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, Index(i), vatRate, mode),
                      VatOnSalesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, Index(i), vatRate, mode)
                    ).flatten
                  ).withCard(
                      card = Card(
                        title = Some(CardTitle(content = HtmlContent(messages("checkSalesToEu.vatRateTitle", vatRate.rateForDisplay)))),
                        actions = None
                      )
                    )
                )
            }).getOrElse(Seq.empty)

          withCompleteData[SalesFromCountryWithOptionalVat](
            index = countryFromIndex,
            data = getIncompleteToEuSales _,
            onFailure = (incompleteSales: Seq[SalesFromCountryWithOptionalVat]) =>
              Ok(view(
                mode,
                mainList,
                vatRateLists,
                request.userAnswers.period,
                countryFromIndex,
                countryToIndex,
                countryFrom,
                countryTo,
                incompleteSales.map(_.countryOfConsumption.name)
              ))) {
            Ok(view(mode, mainList, vatRateLists, request.userAnswers.period, countryFromIndex, countryToIndex, countryFrom, countryTo, Seq.empty))
          }
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] =
    cc.authAndGetData(period) {
      implicit request => {
        withCompleteData[SalesFromCountryWithOptionalVat](
          index = countryFromIndex,
          data = getIncompleteToEuSales _,
          onFailure = (_: Seq[SalesFromCountryWithOptionalVat]) =>
            if (incompletePromptShown) {
              firstIndexedIncompleteSaleToEu(countryFromIndex) match {
                case Some(incompleteSales) =>
                  Redirect(routes.VatRatesFromEuController.onPageLoad(mode, period, countryFromIndex, Index(incompleteSales._2)))
                case None =>
                  Redirect(routes.JourneyRecoveryController.onPageLoad())
              }
            } else {
              Redirect(routes.CheckSalesToEuController.onPageLoad(mode, period, countryFromIndex, countryToIndex))
            }
        ) {
          Redirect(CheckSalesToEuPage(countryFromIndex).navigate(mode, request.userAnswers))
        }
      }
    }
}

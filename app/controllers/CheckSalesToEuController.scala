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

package controllers

import controllers.actions.AuthenticatedControllerComponents
import models.{Index, Mode, Period}
import pages.{CheckSalesToEuPage, VatRatesFromEuPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.TitledSummaryList
import viewmodels.checkAnswers.{NetValueOfSalesFromEuSummary, VatOnSalesFromEuSummary, VatRatesFromEuSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckSalesToEuView

import javax.inject.Inject

class CheckSalesToEuController @Inject()(
                                          cc: AuthenticatedControllerComponents,
                                          view: CheckSalesToEuView
                                        )
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountries(countryFromIndex, countryToIndex) {
        case(countryFrom, countryTo) =>

          val messages = messagesApi.preferred(request)

          val mainList = SummaryListViewModel(
            rows = Seq(VatRatesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, mode)).flatten
          )

          val vatRateLists: Seq[TitledSummaryList] =
            request.userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)).map(_.zipWithIndex.map {
              case (vatRate, i) =>
                TitledSummaryList(
                  title = messages("checkSalesToEu.vatRateTitle", vatRate.rateForDisplay),
                  list = SummaryListViewModel(
                    rows = Seq(
                      NetValueOfSalesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, Index(i), vatRate, mode),
                      VatOnSalesFromEuSummary.row(request.userAnswers, countryFromIndex, countryToIndex, Index(i), vatRate, mode)
                    ).flatten
                  )
                )
            }).getOrElse(Seq.empty)

          Ok(view(mode, mainList, vatRateLists, period, countryFromIndex, countryToIndex, countryFrom, countryTo))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      Redirect(CheckSalesToEuPage(countryFromIndex).navigate(mode, request.userAnswers))
  }
}

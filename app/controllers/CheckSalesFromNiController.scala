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

import com.google.inject.Inject
import controllers.actions.AuthenticatedControllerComponents
import models.{Index, Mode, Period, VatRateAndSalesWithOptionalVat}
import pages.{CheckSalesFromNiPage, VatRatesFromNiPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.CardTitle
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.TitledSummaryList
import viewmodels.checkAnswers.{NetValueOfSalesFromNiSummary, VatOnSalesFromNiSummary, VatRatesFromNiSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckSalesFromNiView

class CheckSalesFromNiController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckSalesFromNiView
                                          )
  extends FrontendBaseController with SalesFromNiBaseController with CompletionChecks with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountry(index) {
        country =>

          val messages = messagesApi.preferred(request)

          val mainList = SummaryListViewModel(
            rows = Seq(VatRatesFromNiSummary.row(request.userAnswers, index, mode)).flatten
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(messages("vatRatesFromNi.checkYourAnswersLabel")))),
              actions = None
            )
          )

          val vatRateLists: Seq[TitledSummaryList] =
            request.userAnswers.get(VatRatesFromNiPage(index)).map(_.zipWithIndex.map {
              case (vatRate, i) =>

                TitledSummaryList(
                  title = None,
                  list = SummaryListViewModel(
                    rows = Seq(
                      NetValueOfSalesFromNiSummary.row(request.userAnswers, index, Index(i), vatRate, mode),
                      VatOnSalesFromNiSummary.row(request.userAnswers, index, Index(i), vatRate, mode)
                    ).flatten
                  ).withCard(
                    card = Card(
                      title = Some(CardTitle(content = HtmlContent(messages("checkSalesFromNi.vatRateTitle", vatRate.rateForDisplay)))),
                      actions = None
                    )
                  )
                )
            }).getOrElse(Seq.empty)

          withCompleteData[VatRateAndSalesWithOptionalVat](
            index,
            data = getIncompleteNiVatRateAndSales _,
            onFailure = (incomplete: Seq[VatRateAndSalesWithOptionalVat]) => {
            Ok(view(mode, mainList, vatRateLists, request.userAnswers.period, index, country, incomplete))
          }) {
            Ok(view(mode, mainList, vatRateLists, request.userAnswers.period, index, country))
          }
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      withCompleteData[VatRateAndSalesWithOptionalVat](
        index,
        data = getIncompleteNiVatRateAndSales _,
        onFailure = (_: Seq[VatRateAndSalesWithOptionalVat]) => {
          if(incompletePromptShown) {
            Redirect(routes.VatRatesFromNiController.onPageLoad(
              mode,
              period,
              index))
          } else {
            Redirect(routes.CheckSalesFromNiController.onPageLoad(mode, period, index))
          }
        }) {
        Redirect(CheckSalesFromNiPage.navigate(mode, request.userAnswers))
      }
  }
}

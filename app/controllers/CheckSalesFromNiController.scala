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

package controllers

import com.google.inject.Inject
import controllers.actions.AuthenticatedControllerComponents
import models.{Index, Mode, Period}
import pages.VatRatesFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.TitledSummaryList
import viewmodels.checkAnswers.{NetValueOfSalesFromNiSummary, VatOnSalesFromNiSummary, VatRatesFromNiSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckSalesFromNiView

class CheckSalesFromNiController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckSalesFromNiView
                                          )
  extends FrontendBaseController with SalesFromNiBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountry(index) {
        country =>

          val messages = messagesApi.preferred(request)

          val mainList = SummaryListViewModel(
            rows = Seq(VatRatesFromNiSummary.row(request.userAnswers, index)).flatten
          )

          val vatRateLists: Seq[TitledSummaryList] =
            request.userAnswers.get(VatRatesFromNiPage(index)).map(_.zipWithIndex.map {
              case (vatRate, i) =>

                TitledSummaryList(
                  title = messages("checkSalesFromNi.vatRateTitle", vatRate.toString),
                  list = SummaryListViewModel(
                    rows = Seq(
                      NetValueOfSalesFromNiSummary.row(request.userAnswers, index, Index(i)),
                      VatOnSalesFromNiSummary.row(request.userAnswers, index, Index(i))
                    ).flatten
                  )
                )
            }).getOrElse(Seq.empty)

          Ok(view(mode, mainList, vatRateLists, period, country))
      }
  }
}

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
import models.{Index, Period}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView,
                                            vatReturnService: VatReturnService
                                          ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val businessRows = Seq(
        BusinessNameSummary.row(request.registration),
        BusinessVRNSummary.row(request.registration),
        ReturnPeriodSummary.row(request.userAnswers),
        SoldGoodsFromNiSummary.row(request.userAnswers)
      ).flatten

      val businessSummaryList = SummaryListViewModel(
        rows = businessRows
      ).withCssClass("govuk-!-margin-bottom-9")

      val rows = Seq(
        TotalNINetValueOfSalesSummary.row(request.userAnswers),
        TotalNIVatOnSalesSummary.row(request.userAnswers)
      ).flatten

      val list = SummaryListViewModel(
        rows = rows
      ).withCssClass("govuk-!-margin-bottom-9")

      val totalRows = Seq(
        TotalNetValueOfSalesSummary.row(request.userAnswers),
        TotalVatOnSalesSummary.row(request.userAnswers)
      ).flatten

      val totalList = SummaryListViewModel(
        rows = totalRows
      ).withCssClass("govuk-!-margin-bottom-9")

      Ok(view(Map(
        None -> businessSummaryList,
        Some("checkYourAnswers.salesFromNi.heading") -> list,
        Some("checkYourAnswers.allSales.heading") -> totalList
      )))
  }
}

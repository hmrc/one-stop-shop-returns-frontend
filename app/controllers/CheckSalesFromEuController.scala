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

import controllers.actions.AuthenticatedControllerComponents
import models.{Index, Mode, Period}
import pages.CheckSalesFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.TitledSummaryList
import viewmodels.govuk.summarylist._
import views.html.CheckSalesFromEuView

import javax.inject.Inject

class CheckSalesFromEuController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckSalesFromEuView
                                          ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val mainList = SummaryListViewModel(
        rows = Seq.empty
      )

      val vatRateLists: Seq[TitledSummaryList] = Seq.empty

      Ok(view(mode, mainList, vatRateLists, period, index))
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      Redirect(CheckSalesFromEuPage.navigate(mode, request.userAnswers))
  }
}

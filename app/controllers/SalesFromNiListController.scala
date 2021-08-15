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
import forms.SalesFromNiListFormProvider
import models.{Country, Mode, Period}
import pages.SalesFromNiListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.SalesFromNiSummary
import views.html.SalesFromNiListView

import javax.inject.Inject

class SalesFromNiListController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: SalesFromNiListFormProvider,
                                           view: SalesFromNiListView
                                         )
  extends FrontendBaseController with SalesFromNiBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val form = formProvider()

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getNumberOfSalesFromNi {
        number =>

          val canAddCountries = number < Country.euCountries.size
          val list            = SalesFromNiSummary.addToListRows(request.userAnswers, mode)

          Ok(view(form, mode, list, period, canAddCountries))
      }
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getNumberOfSalesFromNi {
        number =>

          val canAddCountries = number < Country.euCountries.size

          form.bindFromRequest().fold(
            formWithErrors => {
              val list = SalesFromNiSummary.addToListRows(request.userAnswers, mode)
              BadRequest(view(formWithErrors, mode, list, period, canAddCountries))
            },
            value =>
              Redirect(SalesFromNiListPage.navigate(request.userAnswers, mode, value))
          )
      }
  }
}

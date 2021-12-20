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
import controllers.corrections.routes
import forms.SalesToEuListFormProvider
import models.{Country, Index, Mode, Period}
import pages.SalesToEuListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.SalesToEuSummary
import views.html.SalesToEuListView

import javax.inject.Inject

class SalesToEuListController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: SalesToEuListFormProvider,
                                           view: SalesToEuListView
                                         )
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getNumberOfSalesToEuAndCountry(index) {
        case (number, country) =>

          val form = formProvider(country)

          val canAddCountries = number < Country.euCountries.size
          val list            = SalesToEuSummary.addToListRows(request.userAnswers, mode, index)

          withCompleteEuSales(index, onFailure = incompleteSales => {
            Ok(view(form, mode, list, period, index, canAddCountries, country, incompleteSales.map(_.countryOfConsumption.name)))
          })(Ok(view(form, mode, list, period, index, canAddCountries, country, Seq.empty)))
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      withCompleteEuSales(index, onFailure = _ => {
        if(incompletePromptShown) {
          firstIndexedIncompleteSaleToEu(index) match {
            case Some(incompleteCountryTo) =>
              Redirect(routes.CheckSalesToEuController.onPageLoad( mode,  period, index, Index(incompleteCountryTo._2)))
            case None =>
              Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
        } else {
          Redirect(routes.SalesToEuListController.onPageLoad( mode,  period, index))
        }
      }) {
        getNumberOfSalesToEuAndCountry(index) {
          case (number, country) =>

            val form = formProvider(country)
            val canAddCountries = number < Country.euCountries.size

            form.bindFromRequest().fold(
              formWithErrors => {
                val list = SalesToEuSummary.addToListRows(request.userAnswers, mode, index)
                BadRequest(view(formWithErrors, mode, list, period, index, canAddCountries, country, Seq()))
              },
              value =>
                Redirect(SalesToEuListPage(index).navigate(request.userAnswers, mode, value))
            )
        }
      }
  }
}

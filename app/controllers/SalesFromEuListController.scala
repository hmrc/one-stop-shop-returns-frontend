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
import forms.SalesFromEuListFormProvider
import models.{Country, Index, Mode, Period, SalesFromEuWithOptionalVat}
import pages.SalesFromEuListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import viewmodels.checkAnswers.SalesFromEuSummary
import views.html.SalesFromEuListView

import javax.inject.Inject

class SalesFromEuListController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           formProvider: SalesFromEuListFormProvider,
                                           view: SalesFromEuListView
                                         )
  extends FrontendBaseController with SalesFromEuBaseController with CompletionChecks with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val form = formProvider()

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getNumberOfSalesFromEu {
        number =>

          val canAddCountries = number < Country.euCountries.size
          val list            = SalesFromEuSummary.addToListRows(request.userAnswers, mode)

          withCompleteData[SalesFromEuWithOptionalVat](
            data = getIncompleteFromEuSales _,
            onFailure = (incompleteSales: Seq[SalesFromEuWithOptionalVat]) => {
            Ok(view(form, mode, list, request.userAnswers.period, canAddCountries, incompleteSales.map(_.countryOfSale)))
          })(Ok(view(form, mode, list, request.userAnswers.period, canAddCountries)))
      }
  }

  def onSubmit(mode: Mode, period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      withCompleteData[SalesFromEuWithOptionalVat](
        data = getIncompleteFromEuSales _,
        onFailure = (_: Seq[SalesFromEuWithOptionalVat]) => {
        if(incompletePromptShown) {
          firstIndexedIncompleteSaleFromEu() match {
            case Some(incompleteSalesFromCountry) if incompleteSalesFromCountry._1.salesFromCountry.isEmpty =>
              Redirect(routes.CountryOfConsumptionFromEuController.onPageLoad( mode,  period, Index(incompleteSalesFromCountry._2), Index(0)))
            case Some(incompleteSalesFromCountry) =>
              Redirect(routes.SalesToEuListController.onPageLoad( mode,  period, Index(incompleteSalesFromCountry._2)))
            case None =>
              Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
        } else {
          Redirect(routes.SalesFromEuListController.onPageLoad( mode,  period))
        }
      }) {
        getNumberOfSalesFromEu {
          number =>

            val canAddCountries = number < Country.euCountries.size

            form.bindFromRequest().fold(
              formWithErrors => {
                val list = SalesFromEuSummary.addToListRows(request.userAnswers, mode)
                BadRequest(view(formWithErrors, mode, list, request.userAnswers.period, canAddCountries))
              },
              value =>
                Redirect(SalesFromEuListPage.navigate(request.userAnswers, mode, value))
            )
        }
      }
  }
}

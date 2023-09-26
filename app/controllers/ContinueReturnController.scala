/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions._
import forms.ContinueReturnFormProvider
import models.{ContinueReturn, Country, Period}
import pages.{ContinueReturnPage, SavedProgressPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AllSalesFromEuQueryWithOptionalVatQuery, AllSalesFromNiWithOptionalVatQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContinueReturnView

import javax.inject.Inject

class ContinueReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: ContinueReturnFormProvider,
                                       view: ContinueReturnView
                                     ) extends FrontendBaseController with SalesFromNiBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      request.userAnswers.get(SavedProgressPage).map(
        _ => Ok(view(form, period))
      ).getOrElse(
        Redirect(controllers.routes.StartReturnController.onPageLoad(period))
      )

  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, period)),
        value => {
          val isOMP = request.registration.isOnlineMarketplace
          val hasNiToNiSale = request.userAnswers.get(AllSalesFromNiWithOptionalVatQuery).getOrElse(List.empty)
            .exists(indexedSalesFromCountryWithOptionalVat => indexedSalesFromCountryWithOptionalVat.countryOfConsumption.code == Country.northernIreland.code)
          val hasSameEuToEuSale = request.userAnswers.get(AllSalesFromEuQueryWithOptionalVatQuery).getOrElse(List.empty)
            .exists(indexedSalesFromEuCountryWithOptionalVat =>
              indexedSalesFromEuCountryWithOptionalVat.salesFromCountry.exists(_.headOption
                .exists(_.countryOfConsumption.code == indexedSalesFromEuCountryWithOptionalVat.countryOfSale.code))
            )

          (isOMP, hasNiToNiSale, hasSameEuToEuSale, value) match {
            case (false, true, false, ContinueReturn.Continue) =>
              Redirect(routes.NiToNiInterceptController.onPageLoad(period))
            case (false, false, true, ContinueReturn.Continue) =>
              Redirect(routes.CountryToSameCountryController.onPageLoad(period))
            case (false, true, true, ContinueReturn.Continue) =>
              Redirect(routes.NiToNiInterceptController.onPageLoad(period))
            case (false, false, false, ContinueReturn.Continue) =>
              Redirect(ContinueReturnPage.navigate(request.userAnswers, value))
            case _ =>
              Redirect(ContinueReturnPage.navigate(request.userAnswers, value))
          }

        }
      )
  }
}

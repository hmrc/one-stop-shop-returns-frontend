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

import controllers.actions._
import forms.VatRatesFromEuFormProvider
import models.{Index, Mode, Period}
import pages.VatRatesFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.VatRatesFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromEuController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: VatRatesFromEuFormProvider,
                                        vatRateService: VatRateService,
                                        view: VatRatesFromEuView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with VatRateBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountries(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val vatRates = vatRateService.vatRates(period, countryFrom)
          val form     = formProvider(vatRates)

          val preparedForm = request.userAnswers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo, checkboxItems(vatRates)))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountriesAsync(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val vatRates = vatRateService.vatRates(period, countryFrom)
          val form     = formProvider(vatRates)
          
          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo, checkboxItems(vatRates))).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(VatRatesFromEuPage(countryFromIndex, countryToIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatRatesFromEuPage(countryFromIndex, countryToIndex).navigate(mode, updatedAnswers))
          )
      }
  }
}

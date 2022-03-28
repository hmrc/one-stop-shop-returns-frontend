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

import controllers.actions._
import forms.VatOnSalesFromNiFormProvider
import models.{Index, Mode, Period}
import pages.VatOnSalesFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.VatOnSalesFromNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatOnSalesFromNiController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: VatOnSalesFromNiFormProvider,
                                        view: VatOnSalesFromNiView,
                                        vatRateService: VatRateService
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromNiBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountryVatRateAndNetSales(countryIndex, vatRateIndex) {
        case (country, vatRate, netSales) =>

          val form = formProvider(vatRate, netSales)
          val standardVat = vatRateService.standardVatOnSales(netSales, vatRate)

          val preparedForm = request.userAnswers.get(VatOnSalesFromNiPage(countryIndex, vatRateIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, period, countryIndex, vatRateIndex, country, vatRate, netSales, standardVat))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryVatRateAndNetSalesAsync(countryIndex, vatRateIndex) {
        case (country, vatRate, netSales) =>

          val form = formProvider(vatRate, netSales)
          val standardVat = vatRateService.standardVatOnSales(netSales, vatRate)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, period, countryIndex, vatRateIndex, country, vatRate, netSales, standardVat))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(VatOnSalesFromNiPage(countryIndex, vatRateIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatOnSalesFromNiPage(countryIndex, vatRateIndex).navigate(mode, updatedAnswers))
          )
      }
  }
}

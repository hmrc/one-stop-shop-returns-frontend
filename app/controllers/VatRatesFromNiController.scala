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
import forms.VatRatesFromNiFormProvider
import models.{Index, Mode, Period}
import pages.VatRatesFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.VatRatesFromNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromNiController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: VatRatesFromNiFormProvider,
                                        view: VatRatesFromNiView,
                                        vatRateService: VatRateService
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromNiBaseController with VatRateBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountry(index) {
        country =>

          val vatRates = vatRateService.vatRates(period, country)
          val form     = formProvider(vatRates)

          val preparedForm = request.userAnswers.get(VatRatesFromNiPage(index)) match {
            case None => form
            case Some(value) =>
              form.fill(value)
          }

          Ok(view(preparedForm, mode, period, index, country, checkboxItems(vatRates)))
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryAsync(index) {
        country =>

          val vatRates = vatRateService.vatRates(period, country)
          val form     = formProvider(vatRates)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, mode, period, index, country, checkboxItems(vatRates))).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(VatRatesFromNiPage(index), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatRatesFromNiPage(index).navigate(mode, updatedAnswers))
          )
      }
  }
}

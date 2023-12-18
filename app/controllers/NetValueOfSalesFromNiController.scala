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
import forms.NetValueOfSalesFromNiFormProvider
import models.{Index, Mode, Period}
import pages.NetValueOfSalesFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NetValueOfSalesFromNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NetValueOfSalesFromNiController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: NetValueOfSalesFromNiFormProvider,
                                        view: NetValueOfSalesFromNiView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromNiBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountryAndVatRate(countryIndex, vatRateIndex) {
        case (country, vatRate) =>

          val form = formProvider(vatRate)
          val preparedForm = request.userAnswers.get(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, request.userAnswers.period, countryIndex, vatRateIndex, country, vatRate))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryAndVatRateAsync(countryIndex, vatRateIndex) {
        case (country, vatRate) =>

          val form = formProvider(vatRate)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, request.userAnswers.period, countryIndex, vatRateIndex, country, vatRate))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex).navigate(mode, updatedAnswers))
          )

      }
  }
}

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

import controllers.actions._
import forms.NetValueOfSalesFromEuFormProvider
import models.{Index, Mode, Period}
import pages.NetValueOfSalesFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.NetValueOfSalesFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NetValueOfSalesFromEuController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: NetValueOfSalesFromEuFormProvider,
                                        view: NetValueOfSalesFromEuView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index): Action[AnyContent] =
    cc.authAndGetData(period) {
      implicit request =>
        getCountriesAndVatRate(countryFromIndex, countryToIndex, vatRateIndex) {
          case (countryFrom, countryTo, vatRate) =>

            val form = formProvider(vatRate)

            val preparedForm = request.userAnswers.get(NetValueOfSalesFromEuPage(countryFromIndex, countryToIndex, vatRateIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            Ok(view(
              preparedForm,
              mode,
              request.userAnswers.period,
              countryFromIndex,
              countryToIndex,
              vatRateIndex,
              countryFrom,
              countryTo,
              vatRate
            ))
        }
    }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index, vatRateIndex: Index): Action[AnyContent] =
    cc.authAndGetData(period).async {
      implicit request =>
        getCountriesAndVatRateAsync(countryFromIndex, countryToIndex, vatRateIndex) {
          case (countryFrom, countryTo, vatRate) =>

            val form = formProvider(vatRate)

            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(
                  formWithErrors,
                  mode,
                  request.userAnswers.period,
                  countryFromIndex,
                  countryToIndex,
                  vatRateIndex,
                  countryFrom,
                  countryTo,
                  vatRate
                )).toFuture,

              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(NetValueOfSalesFromEuPage(countryFromIndex, countryToIndex, vatRateIndex), value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(NetValueOfSalesFromEuPage(countryFromIndex, countryToIndex, vatRateIndex).navigate(mode, updatedAnswers))
            )
        }
    }
}

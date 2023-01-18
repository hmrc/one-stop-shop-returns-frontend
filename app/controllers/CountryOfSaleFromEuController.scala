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
import forms.CountryOfSaleFromEuFormProvider
import models.{Index, Mode, Period}
import pages.CountryOfSaleFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllSalesFromEuQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryOfSaleFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryOfSaleFromEuController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: CountryOfSaleFromEuFormProvider,
                                        view: CountryOfSaleFromEuView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val form =
        formProvider(
          index,
          request.userAnswers
            .get(AllSalesFromEuQuery)
            .getOrElse(Seq.empty)
            .map(_.countryOfSale)
        )

      val preparedForm = request.userAnswers.get(CountryOfSaleFromEuPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period, index))
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val form =
        formProvider(
          index,
          request.userAnswers
            .get(AllSalesFromEuQuery)
            .getOrElse(Seq.empty)
            .map(_.countryOfSale)
        )

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period, index))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryOfSaleFromEuPage(index), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CountryOfSaleFromEuPage(index).navigate(mode, updatedAnswers))
      )
  }
}

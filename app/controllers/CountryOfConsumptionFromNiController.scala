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
import forms.CountryOfConsumptionFromNiFormProvider
import models.{Index, Mode, Period}
import pages.CountryOfConsumptionFromNiPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllSalesFromNiQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryOfConsumptionFromNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryOfConsumptionFromNiController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: CountryOfConsumptionFromNiFormProvider,
                                        view: CountryOfConsumptionFromNiView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      val form =
        formProvider(
          index,
          request.userAnswers.get(AllSalesFromNiQuery)
            .getOrElse(Seq.empty)
            .map(_.countryOfConsumption),
          request.registration.isOnlineMarketplace
        )

      val preparedForm = request.userAnswers.get(CountryOfConsumptionFromNiPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, request.userAnswers.period, index, request.registration.isOnlineMarketplace))
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val form =
        formProvider(
          index,
          request.userAnswers.get(AllSalesFromNiQuery)
            .getOrElse(Seq.empty)
            .map(_.countryOfConsumption),
          request.registration.isOnlineMarketplace
        )

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            formWithErrors,
            mode,
            request.userAnswers.period,
            index,
            request.registration.isOnlineMarketplace
          ))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryOfConsumptionFromNiPage(index), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CountryOfConsumptionFromNiPage(index).navigate(mode, updatedAnswers))
      )
  }
}

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
import forms.DeleteSalesFromEuFormProvider

import javax.inject.Inject
import models.{Index, Mode, Period}
import pages.DeleteSalesFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.SalesFromEuQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import views.html.DeleteSalesFromEuView

import scala.concurrent.{ExecutionContext, Future}

class DeleteSalesFromEuController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: DeleteSalesFromEuFormProvider,
                                       view: DeleteSalesFromEuView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountryFrom(index) {
        country =>

          val form = formProvider(country)

          val preparedForm = request.userAnswers.get(DeleteSalesFromEuPage(index)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, period, index, country))
      }
  }

  def onSubmit(mode: Mode, period: Period, index: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryFromAsync(index) {
        country =>

          val form = formProvider(country)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, mode, period, index, country)).toFuture,

            value =>
              if(value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(SalesFromEuQuery(index)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteSalesFromEuPage(index).navigate(mode, updatedAnswers))
              } else {
                Redirect(DeleteSalesFromEuPage(index).navigate(mode, request.userAnswers)).toFuture
              }
          )
      }
  }
}

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
import forms.DeleteSalesToEuFormProvider

import javax.inject.Inject
import models.{Index, Mode, Period}
import pages.DeleteSalesToEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.SalesToEuQuery
import utils.FutureSyntax._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DeleteSalesToEuView

import scala.concurrent.{ExecutionContext, Future}

class DeleteSalesToEuController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: DeleteSalesToEuFormProvider,
                                       view: DeleteSalesToEuView
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountries(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val form = formProvider(countryFrom, countryTo)

          val preparedForm = request.userAnswers.get(DeleteSalesToEuPage(countryFromIndex, countryToIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountriesAsync(countryFromIndex, countryToIndex) {
        case (countryFrom, countryTo) =>

          val form = formProvider(countryFrom, countryTo)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, period, countryFromIndex, countryToIndex, countryFrom, countryTo))),

            value =>
              if(value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(SalesToEuQuery(countryFromIndex, countryToIndex)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteSalesToEuPage(countryFromIndex, countryToIndex).navigate(mode, updatedAnswers))
              } else {
                Redirect(DeleteSalesToEuPage(countryFromIndex, countryToIndex).navigate(mode, request.userAnswers)).toFuture
              }
          )
      }
  }
}

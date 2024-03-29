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
import forms.SoldGoodsFromEuFormProvider
import models.{Mode, Period}
import pages.SoldGoodsFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SoldGoodsFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SoldGoodsFromEuController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: SoldGoodsFromEuFormProvider,
                                       view: SoldGoodsFromEuView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get(SoldGoodsFromEuPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, request.userAnswers.period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, request.userAnswers.period))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SoldGoodsFromEuPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(SoldGoodsFromEuPage.navigate(mode, updatedAnswers))
      )
  }
}

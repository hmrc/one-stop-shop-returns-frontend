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
import forms.ContinueReturnFormProvider
import javax.inject.Inject
import models.{Mode, Period}
import pages.ContinueReturnPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContinueReturnView

import scala.concurrent.{ExecutionContext, Future}

class ContinueReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: ContinueReturnFormProvider,
                                       view: ContinueReturnView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      Ok(view(form, period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, period)),
        value => Redirect(ContinueReturnPage.navigate(request.userAnswers, value))
      )
  }
}
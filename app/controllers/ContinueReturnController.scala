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
import forms.ContinueReturnFormProvider
import models.Period
import pages.{ContinueReturnPage, SavedProgressPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ContinueReturnView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ContinueReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: ContinueReturnFormProvider,
                                       view: ContinueReturnView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      request.userAnswers.get(SavedProgressPage).map(
        _ => Ok(view(form, period))
      ).getOrElse(
        Redirect(controllers.routes.StartReturnController.onPageLoad(period))
      )

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
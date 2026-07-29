/*
 * Copyright 2026 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.actions.*
import models.{NormalMode, Period}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.CheckRegistrationUptoDateView

import javax.inject.Inject

class CheckRegistrationUptoDateController @Inject()(
                                                     cc: AuthenticatedControllerComponents,
                                                     frontendAppConfig: FrontendAppConfig,
                                                     view: CheckRegistrationUptoDateView
                                                   ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetRegistrationWithoutCheckBouncedEmail() {
    implicit request =>
      Ok(view(period, frontendAppConfig.changeYourRegistrationUrl))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetRegistrationWithoutCheckBouncedEmail().async {
    implicit request =>
      Redirect(controllers.fileUpload.routes.WantToUploadFileController.onPageLoad(NormalMode, period)).toFuture
  }
}

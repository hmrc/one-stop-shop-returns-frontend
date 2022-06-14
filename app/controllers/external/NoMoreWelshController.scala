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

package controllers.external

import controllers.actions.AuthenticatedControllerComponents
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NoMoreWelshTranslationsView
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class NoMoreWelshController @Inject()(
                                       view: NoMoreWelshTranslationsView,
                                       cc: AuthenticatedControllerComponents
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  override protected def controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(redirectUrl: Option[String] = None): Action[AnyContent] = cc.auth {
    implicit request =>
      Ok(view(redirectUrl))
  }
}
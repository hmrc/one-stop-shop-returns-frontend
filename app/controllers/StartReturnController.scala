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
import forms.StartReturnFormProvider
import models.Period
import pages.StartReturnPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PartialReturnPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.StartReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: StartReturnFormProvider,
                                       partialReturnPeriodService: PartialReturnPeriodService,
                                       view: StartReturnView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period).async {
    implicit request =>
      val form = formProvider(period)

      partialReturnPeriodService.getPartialReturnPeriod(request.registration, period).map { maybePartialReturnPeriod =>
        Ok(view(form, period, maybePartialReturnPeriod))
      }
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period).async {
    implicit request =>

      val form = formProvider(period)

      form.bindFromRequest().fold(
        formWithErrors => {
          partialReturnPeriodService.getPartialReturnPeriod(request.registration, period).map { maybePartialReturnPeriod =>
            BadRequest(view(formWithErrors, period, maybePartialReturnPeriod))
          }
        },

        value => {
          if (!value) {
            cc.sessionRepository.clear(request.userId)
          }
          Future.successful(Redirect(StartReturnPage.navigate(period, value)))
        }
      )
  }
}

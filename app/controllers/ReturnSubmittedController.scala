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

import controllers.actions.AuthenticatedControllerComponents
import models.{Period, ReturnReference}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

import javax.inject.Inject

class ReturnSubmittedController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           view: ReturnSubmittedView
                                         ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      // TODO: Get amount owed from user answers / service / submitted payload
      val vatOwed         = currencyFormat(1)
      val returnReference = ReturnReference(request.vrn, period)
      val email           = request.registration.contactDetails.emailAddress

      Ok(view(period, returnReference, vatOwed, email))
  }
}

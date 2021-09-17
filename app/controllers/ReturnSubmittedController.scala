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

import connectors.VatReturnConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.{Period, ReturnReference}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnSalesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnSubmittedController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           view: ReturnSubmittedView,
                                           vatReturnConnector: VatReturnConnector,
                                           vatReturnSalesService: VatReturnSalesService
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      vatReturnConnector.get(period).map {
        case Right(vatReturn) =>

          val vatOwed = vatReturnSalesService.getTotalVatOnSales(vatReturn)
          val returnReference = ReturnReference(request.vrn, period)
          val email = request.registration.contactDetails.emailAddress
          val showPayNow = vatOwed > 0

          Ok(view(period, returnReference, currencyFormat(vatOwed), email, showPayNow))
        case _ =>
          Redirect(routes.YourAccountController.onPageLoad())
      }.recover {
        case e: Exception =>
          logger.error(s"Error occurred: ${e.getMessage}", e)
          throw e
      }

  }
}

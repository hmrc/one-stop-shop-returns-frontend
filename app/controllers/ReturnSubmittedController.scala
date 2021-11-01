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
import queries.EmailConfirmationQuery
import services.VatReturnSalesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyFormatter._
import views.html.ReturnSubmittedView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReturnSubmittedController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           view: ReturnSubmittedView,
                                           vatReturnConnector: VatReturnConnector,
                                           vatReturnSalesService: VatReturnSalesService,
                                           clock: Clock
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = {
    (cc.actionBuilder andThen cc.identify andThen cc.getRegistration andThen cc.getData(period) andThen cc.requireData).async {
    implicit request =>

      vatReturnConnector.get(period).map {
        case Right(vatReturn) =>
          val vatOwed = vatReturnSalesService.getTotalVatOnSales(vatReturn)
          val returnReference = ReturnReference(request.vrn, period)
          val email = request.registration.contactDetails.emailAddress
          val showEmailConfirmation = request.userAnswers.get(EmailConfirmationQuery)
          val displayPayNow = vatOwed > 0
          val amountToPayInPence: Long = (vatOwed * 100).toLong
          val overdueReturn = period.paymentDeadline.isBefore(LocalDate.now(clock))

          Ok(view(
            period,
            returnReference,
            currencyFormat(vatOwed),
            showEmailConfirmation.get,
            email,
            displayPayNow,
            amountToPayInPence,
            overdueReturn
          ))
        case _ =>
          Redirect(routes.YourAccountController.onPageLoad())
      }.recover {
        case e: Exception =>
          logger.error(s"Error occurred: ${e.getMessage}", e)
          throw e
      }
    }
  }
}

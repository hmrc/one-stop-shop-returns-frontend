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

import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
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
import scala.concurrent.{ExecutionContext, Future}

class ReturnSubmittedController @Inject()(
                                           cc: AuthenticatedControllerComponents,
                                           view: ReturnSubmittedView,
                                           vatReturnConnector: VatReturnConnector,
                                           correctionConnector: CorrectionConnector,
                                           vatReturnSalesService: VatReturnSalesService,
                                           clock: Clock
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetDataSimple(period).async {
    implicit request =>

      (for {
        vatReturnResponse <- vatReturnConnector.get(period) // TODO -> Need flag with etmp totalVATAmountDueForAllMSGBP value 
        correctionResponse <- correctionConnector.get(period)
        maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
       } yield (vatReturnResponse, correctionResponse, maybeSavedExternalUrl)).flatMap {
        case (Right(vatReturn), correctionResponse, maybeSavedExternalUrl) =>

          val maybeCorrectionPayload =
            correctionResponse match {
              case Right(correctionPayload) => Some(correctionPayload)
              case _ => None
            }

          val vatOwed = vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload)
          val returnReference = ReturnReference(request.vrn, period)
          val email = request.registration.contactDetails.emailAddress
          val showEmailConfirmation = request.userAnswers.get(EmailConfirmationQuery)
          val displayPayNow = vatOwed > 0
          val amountToPayInPence: Long = (vatOwed * 100).toLong
          val overdueReturn = period.paymentDeadline.isBefore(LocalDate.now(clock))

          val backToYourAccountUrl = maybeSavedExternalUrl.fold(_ => None, _.url)

          for {
            _ <- cc.sessionRepository.clear(request.userId)
          } yield {
            Ok(view(
              request.userAnswers.period,
              returnReference,
              currencyFormat(vatOwed),
              showEmailConfirmation.getOrElse(false),
              email,
              displayPayNow,
              amountToPayInPence,
              overdueReturn,
              backToYourAccountUrl
            ))
          }
        case _ =>
            Future.successful(Redirect(routes.YourAccountController.onPageLoad()))
      }.recover {
        case e: Exception =>
          logger.error(s"Error occurred: ${e.getMessage}", e)
          throw e
      }
    }
}

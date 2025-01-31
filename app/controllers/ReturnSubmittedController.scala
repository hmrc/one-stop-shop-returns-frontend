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

import config.FrontendAppConfig
import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.domain.VatReturn
import models.etmp.EtmpVatReturn
import models.{Period, ReturnReference}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.EmailConfirmationQuery
import services.VatReturnSalesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyFormatter.*
import utils.FutureSyntax.FutureOps
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
                                           frontendAppConfig: FrontendAppConfig,
                                           clock: Clock
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetDataSimple(period).async {
    implicit request =>

      (for {
        vatOwed <- getVatOwed(period)
        maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
      } yield (vatOwed, maybeSavedExternalUrl)).flatMap {
        case (vatOwed, maybeSavedExternalUrl) =>

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
      }.recover {
        case e: Exception =>
          logger.error(s"Error occurred: ${e.getMessage}", e)
          throw e
      }
  }
  
  private def getVatOwed(period: Period)(implicit hc: HeaderCarrier): Future[BigDecimal] = {
    if (frontendAppConfig.strategicReturnApiEnabled) {
      vatReturnConnector.getEtmpVatReturn(period).flatMap {
        case Right(etmpVatReturn: EtmpVatReturn) =>
          etmpVatReturn.totalVATAmountDueForAllMSGBP.toFuture
        case Left(error) =>
          val message: String = s"There wss an error retrieving the ETMP VAT return: $error"
          val exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
    } else {
      (for {
        vatReturnResponse <- vatReturnConnector.get(period)
        correctionResponse <- correctionConnector.get(period)
      } yield (vatReturnResponse, correctionResponse)).flatMap {
        case (Right(vatReturn), correctionResponse) =>
          val maybeCorrectionPayload = correctionResponse match {
            case Right(correctionPayload) =>
              Some(correctionPayload)
            case _ => None
          }
          vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload).toFuture

        case (Left(error), _) =>
          val message: String = s"There wss an error retrieving the VAT return: $error"
          val exception = new Exception(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
    }
  }
}

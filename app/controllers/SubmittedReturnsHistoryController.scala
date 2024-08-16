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

import connectors.financialdata.FinancialDataConnector
import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
import controllers.actions._
import logging.Logging
import models.{Period, StandardPeriod}
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.requests.RegistrationRequest
import models.responses.{NotFound => ResponseNotFound}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnSalesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnsHistoryController @Inject()(
                                                   cc: AuthenticatedControllerComponents,
                                                   view: SubmittedReturnsHistoryView,
                                                   financialDataConnector: FinancialDataConnector,
                                                   vatReturnConnector: VatReturnConnector,
                                                   correctionConnector: CorrectionConnector,
                                                   vatReturnSalesService: VatReturnSalesService
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      for {
        maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
        submittedVatReturns <- vatReturnConnector.getSubmittedVatReturns()
        financialData <- getFinancialInformation()
        periodToPaymentInfo <- getPaymentInfoForReturns(submittedVatReturns, financialData)
      } yield {
        val externalUrl = maybeSavedExternalUrl.fold(_ => None, _.url)
        val displayBanner = financialData.allOutstandingPayments.exists(_.paymentStatus == PaymentStatus.Unknown)

        Ok(view(periodToPaymentInfo, displayBanner, externalUrl))
      }

  }

  private def getFinancialInformation()(implicit request: RegistrationRequest[_]): Future[CurrentPayments] = {
    financialDataConnector.getFinancialData(request.vrn).map {
      case Right(vatReturnsWithFinancialData) =>
        vatReturnsWithFinancialData
      case Left(e) =>
        logger.error(s"There were some errors: $e")
        throw new Exception(s"$e")
    }
  }

  private def getPaymentInfoForReturns(
                                        vatReturns: Seq[VatReturn],
                                        currentPayments: CurrentPayments
                                      )(implicit hc: HeaderCarrier): Future[Map[Period, Payment]] = {
    Future.sequence(vatReturns.map { vatReturn =>
      val allPayments = currentPayments.allOutstandingPayments ++ currentPayments.excludedPayments

      allPayments.find(_.period == vatReturn.period) match {
        case Some(payment) =>
          Map(vatReturn.period -> payment).toFuture
        case _ =>
          correctionConnector.get(vatReturn.period).map {
            case Right(correctionPayload) =>
              mapPeriodToPayment(vatReturn, Some(correctionPayload))
            case Left(ResponseNotFound) =>
              mapPeriodToPayment(vatReturn, None)
            case _ =>
              val errorMessage = "Was unable to get a correction response for fallback payment info"
              logger.error(errorMessage)
              throw new Exception(errorMessage)


          }
      }
    }).map(_.flatten.toMap)
  }

  private def mapPeriodToPayment(vatReturn: VatReturn, maybeCorrectionPayload: Option[CorrectionPayload]): Map[Period, Payment] = {
    val amountOwedFromReturn = vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload)

    val paymentStatus = if (amountOwedFromReturn == 0) {
      PaymentStatus.NilReturn
    } else {
      PaymentStatus.Paid
    }
    Map(vatReturn.period -> Payment(
      period = StandardPeriod.fromPeriod(vatReturn.period),
      amountOwed = 0,
      dateDue = vatReturn.period.paymentDeadline,
      paymentStatus = paymentStatus
    ))
  }
}

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

import config.Constants.submittedReturnsPeriodsLimit
import config.FrontendAppConfig
import connectors.VatReturnConnector
import connectors.financialdata.FinancialDataConnector
import controllers.actions.*
import logging.Logging
import models.Period
import models.financialdata.{CurrentPayments, Payment, PaymentStatus}
import models.requests.RegistrationRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmittedReturnsHistoryView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnsHistoryController @Inject()(
                                                   cc: AuthenticatedControllerComponents,
                                                   config: FrontendAppConfig,
                                                   view: SubmittedReturnsHistoryView,
                                                   financialDataConnector: FinancialDataConnector,
                                                   vatReturnConnector: VatReturnConnector,
                                                   obligationService: ObligationsService,
                                                   clock: Clock,
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      val submittedVatReturnsFuture = if (config.strategicReturnApiEnabled) {
        obligationService.getFulfilledObligations(request.vrn).map { obligations =>
          obligations.map(obligation =>
            Period.fromEtmpPeriodKey(obligation.periodKey))
        }
      } else {
        vatReturnConnector.getSubmittedVatReturns().map(_.map(_.period))
      }
      for {
        maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
        submittedPeriods <- submittedVatReturnsFuture
        filteredSubmittedVatReturns = filteredReturnsWithinSixYears(submittedPeriods)
        financialData <- getFinancialInformation()
        periodToPaymentInfo = getPaymentInfoForReturns(filteredSubmittedVatReturns, financialData)
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
                                        periods: Seq[Period],
                                        currentPayments: CurrentPayments
                                      ): Map[Period, Payment] = {

    val allPayments = currentPayments.allOutstandingPayments ++ currentPayments.excludedPayments ++ currentPayments.completedPayments

    periods.flatMap { period =>
      allPayments.find(_.period == period) match {
        case Some(payment) =>
          Map(period -> payment)
        case _ =>
          val message = s"Unable to find period in financial data for expected period ${period}"
          val exception = IllegalStateException(message)
          logger.error(exception.getMessage, exception)
          throw exception
      }
    }.toMap

  }

  private def getPeriodsWithinSixYears(periods: Seq[Period]): Seq[Period] = {
    val endOfPreviousPeriod = LocalDate.now(clock).withDayOfMonth(1).minusDays(1)
    periods.filter { period =>
      period.lastDay.isAfter(endOfPreviousPeriod.minusYears(submittedReturnsPeriodsLimit))

    }
  }

  private def filteredReturnsWithinSixYears(periods: Seq[Period]): Seq[Period] = {
    val periodsWithinSixYears = getPeriodsWithinSixYears(periods)
    periods.filter(period => periodsWithinSixYears.contains(period))
  }
}


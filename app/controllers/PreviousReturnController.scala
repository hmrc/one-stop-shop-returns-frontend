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
import connectors.VatReturnConnector
import connectors.financialdata.FinancialDataConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.etmp.EtmpVatReturn
import models.exclusions.{ExcludedTrader, ExclusionReason}
import models.requests.RegistrationRequest
import models.{PartialReturnPeriod, Period}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryList, SummaryListRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist.*
import viewmodels.previousReturn.*
import views.html.NewPreviousReturnView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousReturnController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          newView: NewPreviousReturnView,
                                          vatReturnConnector: VatReturnConnector,
                                          financialDataConnector: FinancialDataConnector,
                                          clock: Clock
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request => {
      newOnPageLoad(period)
    }
  }

  private def newOnPageLoad(period: Period)
                           (implicit hc: HeaderCarrier, messages: Messages, request: RegistrationRequest[_]): Future[Result] = {
    if (isPeriodOlderThanSixYears(period)) {
      Redirect(controllers.routes.NoLongerAbleToViewReturnController.onPageLoad()).toFuture
    } else {
      for {
        etmpVatReturnResponse <- vatReturnConnector.getEtmpVatReturn(period)
        getChargeResult <- financialDataConnector.getCharge(period)
      } yield {
        (etmpVatReturnResponse, getChargeResult) match {
          case (Right(etmpVatReturn), chargeResult) =>
            val maybeCharge = chargeResult.fold(_ => None, charge => charge)

            val determinedPeriod = determineIfPartialPeriod(period, etmpVatReturn)

            val outstandingAmount = maybeCharge.map(_.outstandingAmount)

            val outstanding: BigDecimal = outstandingAmount.getOrElse(etmpVatReturn.totalVATAmountPayable)
            val vatDeclared: BigDecimal = etmpVatReturn.totalVATAmountDueForAllMSGBP

            val currentReturnExcluded = isCurrentlyExcluded(request.registration.excludedTrader) &&
              hasActiveWindowExpired(Period.fromEtmpPeriodKey(etmpVatReturn.periodKey).paymentDeadline)

            val displayPayNow = !currentReturnExcluded &&
              (vatDeclared > 0 && outstanding > 0)

            val returnIsExcludedAndOutstandingAmount: Boolean = currentReturnExcluded && (etmpVatReturn.totalVATAmountDueForAllMSGBP > 0 && outstanding > 0)

            val mainSummaryList = SummaryListViewModel(rows = getMainSummaryList(etmpVatReturn, outstandingAmount, determinedPeriod))

            val vatOwedInPence: Long = (outstanding * 100).toLong

            Ok(newView(
              period = determinedPeriod,
              mainSummaryList = mainSummaryList,
              allNiSales = PreviousReturnTotalNetValueOfSalesFromNiSummary.rows(etmpVatReturn.goodsSupplied),
              allEuSales = PreviousReturnTotalNetValueOfSalesToEuSummary.rows(etmpVatReturn.goodsDispatched),
              corrections = PreviousReturnCorrectionsSummary.correctionRows(etmpVatReturn.correctionPreviousVATReturn),
              negativeAndZeroBalanceCorrectionCountries = PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn),
              vatOwedSummaryList = getVatOwedSummaryList(etmpVatReturn),
              displayPayNow = displayPayNow,
              totalVatPayable = vatDeclared,
              returnIsExcludedAndOutstandingAmount = returnIsExcludedAndOutstandingAmount,
              vatOwedInPence = vatOwedInPence
            ))

          case _ =>
            val message: String = s"There was an error retrieving ETMP VAT return"
            val exception = new Exception(message)
            logger.error(exception.getMessage, exception)
            throw exception
        }
      }
    }
  }
  
  private def isPeriodOlderThanSixYears(period: Period): Boolean = {
    val sixYearsOld = LocalDate.now(clock).minusYears(submittedReturnsPeriodsLimit)
    period.lastDay.isBefore(sixYearsOld)
  }

  private def getMainSummaryList(
                                  etmpVatReturn: EtmpVatReturn,
                                  outstandingAmount: Option[BigDecimal],
                                  period: Period
                                )(implicit messages: Messages): Seq[SummaryListRow] = {

    Seq(
      NewPreviousReturnSummary.rowPayableVatDeclared(etmpVatReturn.totalVATAmountDueForAllMSGBP),
      NewPreviousReturnSummary.rowAmountLeftToPay(outstandingAmount),
      NewPreviousReturnSummary.rowReturnSubmittedDate(etmpVatReturn.returnVersion),
      NewPreviousReturnSummary.rowPaymentDueDate(period.paymentDeadline),
      NewPreviousReturnSummary.rowReturnReference(etmpVatReturn.returnReference),
      NewPreviousReturnSummary.rowPaymentReference(etmpVatReturn.paymentReference)
    ).flatten
  }

  private def getVatOwedSummaryList(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows = PreviousReturnVatOwedSummary.row(etmpVatReturn)
    ).withCard(
      card = Card(
        title = Some(CardTitle(
          content = if (etmpVatReturn.correctionPreviousVATReturn.isEmpty) {
            HtmlContent(messages("newPreviousReturn.vatOwed.title"))
          } else {
            HtmlContent(messages("newPreviousReturn.vatOwed.titleWithCorrections"))
          }
        ))
      )
    )
  }

  private def isCurrentlyExcluded(exclusion: Option[ExcludedTrader]): Boolean = {
    exclusion.exists(_.exclusionReason != ExclusionReason.Reversal)
  }

  private def hasActiveWindowExpired(dueDate: LocalDate): Boolean = {
    val today = LocalDate.now(clock)
    today.isAfter(dueDate.plusYears(3))
  }

  private def determineIfPartialPeriod(period: Period, etmpVatReturn: EtmpVatReturn): Period = {
    if (period.firstDay == etmpVatReturn.returnPeriodFrom && period.lastDay == etmpVatReturn.returnPeriodTo) {
      period
    } else {
      PartialReturnPeriod(
        firstDay = etmpVatReturn.returnPeriodFrom,
        lastDay = etmpVatReturn.returnPeriodTo,
        year = period.year,
        quarter = period.quarter
      )
    }
  }
}


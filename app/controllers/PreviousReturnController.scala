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
import connectors.corrections.CorrectionConnector
import connectors.financialdata.FinancialDataConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.Period
import models.etmp.EtmpVatReturn
import models.exclusions.{ExcludedTrader, ExclusionReason}
import models.requests.RegistrationRequest
import models.responses.NotFound as NotFoundResponse
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PartialReturnPeriodService, VatReturnSalesService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryList, SummaryListRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist.*
import viewmodels.previousReturn.*
import viewmodels.previousReturn.corrections.CorrectionSummary
import views.html.{NewPreviousReturnView, PreviousReturnView}

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousReturnController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          view: PreviousReturnView,
                                          newView: NewPreviousReturnView,
                                          vatReturnConnector: VatReturnConnector,
                                          financialDataConnector: FinancialDataConnector,
                                          correctionConnector: CorrectionConnector,
                                          vatReturnSalesService: VatReturnSalesService,
                                          partialReturnPeriodService: PartialReturnPeriodService,
                                          frontendAppConfig: FrontendAppConfig,
                                          clock: Clock
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request => {

      if (frontendAppConfig.strategicReturnApiEnabled) {
        newOnPageLoad(period)
      } else {
        legacyOnPageLoad(period)
      }
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

            val outstandingAmount = maybeCharge.map(_.outstandingAmount)

            val outstanding: BigDecimal = outstandingAmount.getOrElse(etmpVatReturn.totalVATAmountPayable)
            val vatDeclared: BigDecimal = etmpVatReturn.totalVATAmountDueForAllMSGBP

            val currentReturnExcluded = isCurrentlyExcluded(request.registration.excludedTrader) &&
              hasActiveWindowExpired(Period.fromEtmpPeriodKey(etmpVatReturn.periodKey).paymentDeadline)

            val displayPayNow = !currentReturnExcluded &&
              (vatDeclared > 0 && outstanding > 0)

            val returnIsExcludedAndOutstandingAmount: Boolean = currentReturnExcluded && (etmpVatReturn.totalVATAmountDueForAllMSGBP > 0 && outstanding > 0)

            val mainSummaryList = SummaryListViewModel(rows = getMainSummaryList(etmpVatReturn, outstandingAmount, period))

            val vatOwedInPence: Long = (outstanding * 100).toLong

            partialReturnPeriodService.getPartialReturnPeriod(request.registration, period).map { maybePartialReturnPeriod =>
              Ok(newView(
                period = maybePartialReturnPeriod.getOrElse(period),
                mainSummaryList = mainSummaryList,
                allEuSales = PreviousReturnTotalNetValueOfSalesSummary.rows(etmpVatReturn.goodsSupplied),
                corrections = PreviousReturnCorrectionsSummary.correctionRows(etmpVatReturn.correctionPreviousVATReturn),
                negativeAndZeroBalanceCorrectionCountries = PreviousReturnDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn),
                vatOwedSummaryList = getVatOwedSummaryList(etmpVatReturn),
                displayPayNow = displayPayNow,
                totalVatPayable = outstanding,
                returnIsExcludedAndOutstandingAmount = returnIsExcludedAndOutstandingAmount,
                vatOwedInPence = vatOwedInPence
              ))
            }

          case _ =>
            val message: String = s"There was an error retrieving ETMP VAT return"
            val exception = new Exception(message)
            logger.error(exception.getMessage, exception)
            throw exception
        }
      }
    }.flatten
  }

  private def legacyOnPageLoad(period: Period)(implicit hc: HeaderCarrier, request: RegistrationRequest[_]): Future[Result] = {
    if (isPeriodOlderThanSixYears(period)) {
      Redirect(controllers.routes.NoLongerAbleToViewReturnController.onPageLoad()).toFuture
    } else {

      for {
        vatReturnResult <- vatReturnConnector.get(period)
        getChargeResult <- financialDataConnector.getCharge(period)
        correctionPayload <- correctionConnector.get(period)
        maybeExternalUrl <- vatReturnConnector.getSavedExternalEntry()
      } yield (vatReturnResult, getChargeResult, correctionPayload, maybeExternalUrl)
    }.map {
      case (Right(vatReturn), chargeResponse, correctionPayload, maybeExternalUrl) =>
        val maybeCorrectionPayload =
          correctionPayload match {
            case Right(correction) => Some(correction)
            case _ => None
          }

        val totalVatOwed = vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload)

        val (charge, displayBanner) = chargeResponse match {
          case Right(chargeOption) =>
            val hasVatOwed = totalVatOwed > 0
            (chargeOption, chargeOption.isEmpty && hasVatOwed)
          case _ => (None, true)
        }

        val amountOutstanding = charge.map(_.outstandingAmount)

        val mainList =
          SummaryListViewModel(rows = PreviousReturnSummary.mainListRows(vatReturn, totalVatOwed, amountOutstanding))
        val displayPayNow =
          if (request.registration.excludedTrader.exists(_.isExcludedNotReversed) && period.isInExpiredPeriod(clock)) {
            false
          } else {
            totalVatOwed > 0 && amountOutstanding.forall(outstanding => outstanding > 0)
          }
        val vatOwedInPence: Long = (amountOutstanding.getOrElse(totalVatOwed) * 100).toLong

        val hasCorrections = maybeCorrectionPayload.exists(_.corrections.nonEmpty)
        val totalVatList = SummaryListViewModel(rows = PreviousReturnSummary.totalVatSummaryRows(totalVatOwed, hasCorrections))
        val externalUrl = maybeExternalUrl.fold(_ => None, _.url)
        Ok(view(
          vatReturn = vatReturn,
          mainList = mainList,
          niSalesList = SaleAtVatRateSummary.getAllNiSales(vatReturn),
          euSalesList = SaleAtVatRateSummary.getAllEuSales(vatReturn),
          correctionsForPeriodList = CorrectionSummary.getCorrectionPeriods(maybeCorrectionPayload),
          declaredVatAfterCorrections = CorrectionSummary.getDeclaredVat(maybeCorrectionPayload, vatReturn),
          totalVatList = Some(totalVatList),
          displayPayNow = displayPayNow,
          vatOwedInPence = vatOwedInPence,
          displayBanner = displayBanner,
          externalUrl = externalUrl
        ))

      case (Left(NotFoundResponse), _, _, maybeExternalUrl) =>
        val externalUrl = maybeExternalUrl.fold(_ => None, _.url)
        Redirect(externalUrl.getOrElse(routes.YourAccountController.onPageLoad().url))
      case (Left(e), _, _, _) =>
        logger.error(s"Unexpected result from api while getting return: $e")
        Redirect(routes.JourneyRecoveryController.onPageLoad())

      case _ => Redirect(routes.JourneyRecoveryController.onPageLoad())
    }.recover {

      case e: Exception =>
        logger.error(s"Error while getting previous return: ${e.getMessage}", e)
        Redirect(routes.JourneyRecoveryController.onPageLoad())
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
}


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

import config.FrontendAppConfig
import connectors.VatReturnConnector
import connectors.corrections.CorrectionConnector
import connectors.financialdata.FinancialDataConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.Period
import models.domain.VatReturn
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnSalesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.TitledSummaryList
import viewmodels.govuk.summarylist._
import viewmodels.previousReturn.{PreviousReturnSummary, SaleAtVatRateSummary, TotalSalesSummary}
import views.html.PreviousReturnView
import models.responses.{NotFound => NotFoundResponse}
import viewmodels.previousReturn.corrections.CorrectionSummary

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviousReturnController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          view: PreviousReturnView,
                                          vatReturnConnector: VatReturnConnector,
                                          correctionConnector: CorrectionConnector,
                                          vatReturnSalesService: VatReturnSalesService,
                                          financialDataConnector: FinancialDataConnector,
                                          appConfig: FrontendAppConfig
  )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      {
        for {
          vatReturnResult <- vatReturnConnector.get(period)
          getChargeResult <- financialDataConnector.getCharge(period)
          correctionPayload <- correctionConnector.get(period)
        } yield (vatReturnResult, getChargeResult, correctionPayload)
      }.map {
        case (Right(vatReturn), chargeResponse, correctionPayload) =>
          val maybeCorrectionPayload = if(appConfig.correctionToggle) {
            correctionPayload match {
              case Right(correction) => Some(correction)
              case _ => None
            }
          } else {
            None
          }

          val totalVatOwed = vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturn, maybeCorrectionPayload)

          val (charge, displayBanner) = chargeResponse match {
            case Right(chargeOption) =>
              val hasVatOwed = totalVatOwed > 0
              (chargeOption, chargeOption.isEmpty && hasVatOwed)
            case _ => (None, true)
          }

          val clearedAmount = charge.map(_.clearedAmount)
          val amountOutstanding = charge.map(_.outstandingAmount)

          val mainList =
            SummaryListViewModel(rows = PreviousReturnSummary.rows(vatReturn, totalVatOwed, clearedAmount, amountOutstanding))
          val displayPayNow = totalVatOwed > 0 && amountOutstanding.forall(outstanding => outstanding > 0)
          val vatOwedInPence: Long = (amountOutstanding.getOrElse(totalVatOwed) * 100).toLong

          Ok(view(
            vatReturn,
            mainList,
            SaleAtVatRateSummary.getAllNiSales(vatReturn),
            SaleAtVatRateSummary.getAllEuSales(vatReturn),
            getAllSales(vatReturn),
            CorrectionSummary.getAllCorrections(maybeCorrectionPayload),
            displayPayNow,
            vatOwedInPence,
            displayBanner
          ))

        case (Left(NotFoundResponse), _, _) =>
          Redirect(routes.YourAccountController.onPageLoad())
        case (Left(e), _, _) =>
          logger.error(s"Unexpected result from api while getting return: $e")
          Redirect(routes.JourneyRecoveryController.onPageLoad())

        case _ => Redirect(routes.JourneyRecoveryController.onPageLoad())
      }.recover {

        case e: Exception =>
          logger.error(s"Error while getting previous return: ${e.getMessage}", e)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private[this] def getAllSales(vatReturn: VatReturn)(implicit messages: Messages): TitledSummaryList = {
    val netSalesFromNi = vatReturnSalesService.getTotalNetSalesToCountry(vatReturn.salesFromNi)
    val netSalesFromEu = vatReturnSalesService.getEuTotalNetSales(vatReturn.salesFromEu)
    val vatOnSalesFromNi = vatReturnSalesService.getTotalVatOnSalesToCountry(vatReturn.salesFromNi)
    val vatOnSalesFromEu = vatReturnSalesService.getEuTotalVatOnSales(vatReturn.salesFromEu)
    val totalVatOnSales = vatReturnSalesService.getTotalVatOnSalesBeforeCorrection(vatReturn)

    TitledSummaryList(
      title = messages("previousReturn.allSales.title"),
      list = SummaryListViewModel(
        rows = TotalSalesSummary.rows(
          netSalesFromNi = netSalesFromNi,
          netSalesFromEu = netSalesFromEu,
          vatOnSalesFromNi = vatOnSalesFromNi,
          vatOnSalesFromEu = vatOnSalesFromEu,
          totalVatOnSales = totalVatOnSales,
          correctionToggle = appConfig.correctionToggle
        )
      )
    )
  }
}

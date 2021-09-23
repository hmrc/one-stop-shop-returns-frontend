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

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviousReturnController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          view: PreviousReturnView,
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
          val mainList =
            SummaryListViewModel(rows = PreviousReturnSummary.rows(vatReturn, vatOwed))
          val displayPayNow = vatOwed > 0
          val vatOwedInPence: Long = (vatOwed * 100).toLong

          Ok(view(
            vatReturn,
            mainList,
            SaleAtVatRateSummary.getAllNiSales(vatReturn),
            SaleAtVatRateSummary.getAllEuSales(vatReturn),
            getAllSales(vatReturn, vatOwed),
            displayPayNow,
            vatOwedInPence
          ))
        case Left(NotFoundResponse) =>
          Redirect(routes.YourAccountController.onPageLoad())
        case Left(e) =>
          logger.error(s"Unexpected result from api while getting return: $e")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }.recover {
        case e: Exception =>
          logger.error(s"Error while getting previous return: ${e.getMessage}", e)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private[this] def getAllSales(vatReturn: VatReturn, vatOwed: BigDecimal)(implicit messages: Messages): TitledSummaryList = {
    val netSalesFromNi = vatReturnSalesService.getTotalNetSalesToCountry(vatReturn.salesFromNi)
    val netSalesFromEu = vatReturnSalesService.getEuTotalNetSales(vatReturn.salesFromEu)
    val vatOnSalesFromNi = vatReturnSalesService.getTotalVatOnSalesToCountry(vatReturn.salesFromNi)
    val vatOnSalesFromEu = vatReturnSalesService.getEuTotalVatOnSales(vatReturn.salesFromEu)
    val totalVatOnSales = vatOwed

    TitledSummaryList(
      title = messages("previousReturn.allSales.title"),
      list = SummaryListViewModel(
        rows = TotalSalesSummary.rows(netSalesFromNi, netSalesFromEu, vatOnSalesFromNi, vatOnSalesFromEu, totalVatOnSales)
      )
    )
  }
}

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
import connectors.VatReturnHttpParser.VatReturnResponse
import connectors.financialdata.FinancialDataConnector
import connectors.financialdata.FinancialDataHttpParser.ChargeResponse
import controllers.actions._
import models.Quarter.Q3
import models.Period

import javax.inject.Inject
import play.api.i18n.I18nSupport
import logging.Logging
import models.financialdata.Charge
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmittedReturnsHistoryView
import models.responses.{NotFound => NotFoundResponse}
import services.VatReturnSalesService

import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnsHistoryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: SubmittedReturnsHistoryView,
                                       vatReturnConnector: VatReturnConnector,
                                       vatReturnSalesService: VatReturnSalesService,
                                       financialDataConnector: FinancialDataConnector
                                     ) (implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  val period = new Period(2021, Q3)

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      val returns: Future[(VatReturnResponse, ChargeResponse)] = for {
        vr <- vatReturnConnector.get(period)
        cr <- financialDataConnector.getCharge(period)
      } yield (vr, cr)

    returns.map {
        case (Right(vatReturn), chargeReturn) =>
          val chargeOption = chargeReturn.toOption
          val vatOwed = chargeOption.map(_.outstandingAmount)
            .getOrElse(vatReturnSalesService.getTotalVatOnSales(vatReturn))
          val vatOwedInPence: Long = (vatOwed * 100).toLong
          Ok(view(Some(vatReturn), chargeOption, Some(vatOwedInPence)))
        case (Left(NotFoundResponse), _) =>
          Ok(view(None, None, None))
        case (Left(e), _) =>
          logger.error(s"Unexpected result from api while getting return: $e")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }.recover {
        case e: Exception =>
          logger.error(s"Error while getting previous return: ${e.getMessage}", e)
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }
}

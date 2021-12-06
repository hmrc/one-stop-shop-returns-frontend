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

import connectors.financialdata.FinancialDataConnector
import controllers.actions._
import logging.Logging
import models.financialdata.VatReturnWithFinancialData
import models.responses.ErrorResponse
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnSalesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmittedReturnsHistoryController @Inject()(
                                                   cc: AuthenticatedControllerComponents,
                                                   view: SubmittedReturnsHistoryView,
                                                   financialDataConnector: FinancialDataConnector,
                                                   vatReturnSalesService: VatReturnSalesService
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  type VatReturnWithFinancialDataResponse = Either[ErrorResponse, VatReturnWithFinancialData]

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      financialDataConnector.getVatReturnWithFinancialData(request.registration.commencementDate).map {
        case Right(vatReturnsWithFinancialData) =>
          val displayBanner = {
            if (vatReturnsWithFinancialData.nonEmpty) {
              vatReturnsWithFinancialData.exists { data =>
                data.charge.isEmpty && data.vatOwed
                  .getOrElse(
                    (
                      vatReturnSalesService.getTotalVatOnSalesAfterCorrection(
                        data.vatReturn,
                        data.corrections
                      ) * 100
                    ).toLong) > 0
              }
            } else {
              false
            }
          }

          val vatReturnsWithFinancialDataWithVatOwedCalculated = vatReturnsWithFinancialData.map { vatReturnWithFinancialData =>
            val vatOwed = vatReturnWithFinancialData.vatOwed
              .getOrElse((vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturnWithFinancialData.vatReturn, vatReturnWithFinancialData.corrections) * 100).toLong)

            vatReturnWithFinancialData.copy(vatOwed = Some(vatOwed))
          }

          Ok(view(vatReturnsWithFinancialDataWithVatOwedCalculated, displayBanner))
        case Left(e) =>
          logger.warn(s"There were some errors: $e")
          Ok(view(Seq.empty, true))
      }

  }
}

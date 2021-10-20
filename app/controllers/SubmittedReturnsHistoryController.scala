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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmittedReturnsHistoryController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: SubmittedReturnsHistoryView,
                                       financialDataConnector: FinancialDataConnector
                                     ) (implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  type VatReturnWithFinancialDataResponse = Either[ErrorResponse, VatReturnWithFinancialData]

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      financialDataConnector.getVatReturnWithFinancialData(request.registration.commencementDate).map {
        case Right(vatReturnsWithFinancialData) =>
          val displayBanner = {
            if(vatReturnsWithFinancialData.nonEmpty) {
              vatReturnsWithFinancialData.exists(_.charge.isEmpty)
            } else {
              false
            }
          }

          Ok(view(vatReturnsWithFinancialData, displayBanner))
        case Left(e) =>
          logger.warn(s"There were some errors: $e")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }

  }
}

/*
      Future.sequence(
        periodService.getReturnPeriods(request.registration.commencementDate).map {
          period =>
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
                Right(VatReturnWithFinancialData(Some(vatReturn), chargeOption, Some(vatOwedInPence)))
              case (Left(NotFoundResponse), _) =>
                Right(VatReturnWithFinancialData(None, None, None))
              case (Left(e), _) =>
                logger.error(s"Unexpected result from api while getting return: $e")
                Left(e)
            }
        }
      ).map { vatReturnWithFinancialDataResponse =>
        vatReturnWithFinancialDataResponse.count(_.isLeft) match {
          case 0 =>
            val models = vatReturnWithFinancialDataResponse.filter(_.isRight).map(_.right.get)  // TODO need safer way to get
            val displayBanner =
              if(models.nonEmpty) {
                models.exists(_.charge.isEmpty)
              } else {
                false
              }

            Ok(view(models.toList, displayBanner))
          case _ =>
            val errors = vatReturnWithFinancialDataResponse.filter(_.isLeft).map(_.left.get) // TODO need safer way to get.. Fold?
            logger.warn(s"There were some errors: $errors")
            Redirect(routes.JourneyRecoveryController.onPageLoad()) // TODO does redirecting to journey recovery make sense?
        }
      }
 */

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
                                                   financialDataConnector: FinancialDataConnector,
                                                   vatReturnConnector: VatReturnConnector
                                                 )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  type VatReturnWithFinancialDataResponse = Either[ErrorResponse, VatReturnWithFinancialData]

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      for{
        maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
        financialDataResponse <- financialDataConnector.getVatReturnWithFinancialData(request.registration.commencementDate)
      } yield {
        val externalUrl = maybeSavedExternalUrl.fold(_ => None, _.url)
        financialDataResponse match {
          case Right(vatReturnsWithFinancialData) =>
            val displayBanner = {
              if (vatReturnsWithFinancialData.nonEmpty) {
                vatReturnsWithFinancialData.exists { data =>
                  data.showUpdating
                }
              } else {
                false
              }
            }

            Ok(view(vatReturnsWithFinancialData, displayBanner, externalUrl))
          case Left(e) =>
            logger.error(s"There were some errors: $e")
            throw new Exception(s"$e")

        }
      }



  }
}

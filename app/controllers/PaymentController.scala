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

import config.Service
import connectors.PaymentConnector
import controllers.actions.AuthenticatedControllerComponents
import models.Period
import models.requests.{PaymentPeriod, PaymentRequest}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentController @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 paymentConnector: PaymentConnector,
                                 config: Configuration
                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val baseUrl = config.get[Service]("microservice.services.pay-api")
  protected val controllerComponents: MessagesControllerComponents = cc

  def makePayment(period: Period, amountInPence: Long): Action[AnyContent] =
    cc.authAndGetRegistration.async {
      implicit request => {
        val paymentRequest =
          PaymentRequest(
            request.vrn,
            PaymentPeriod(period),
            amountInPence,
            Some(period.paymentDeadline)
          )

        paymentConnector.submit(paymentRequest)
          .map {
            case Right(value) => Redirect(value.nextUrl)
            case _ => Redirect(s"$baseUrl/pay/service-unavailable")
          }
      }
  }
}

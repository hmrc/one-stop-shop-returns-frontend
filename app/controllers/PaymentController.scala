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

import connectors.PaymentConnector
import controllers.actions.AuthenticatedControllerComponents
import models.Period
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PaymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentController @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 paymentService: PaymentService,
                                 paymentConnector: PaymentConnector
                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, amount: Long): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      val paymentRequest = paymentService.buildPaymentRequest(request.vrn, period, amount)
      paymentConnector.submit(paymentRequest)
        .map {
          case Right(value) => Redirect(value.nextUrl)
          case _ => {
            ???
          }
        }
  }
}

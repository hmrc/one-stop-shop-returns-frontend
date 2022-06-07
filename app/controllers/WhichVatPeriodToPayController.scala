/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.WhichVatPeriodToPayFormProvider
import models.financialdata.{Payment, PaymentStatus}
import models.{Mode, Period}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WhichVatPeriodToPayView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhichVatPeriodToPayController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       financialDataConnector: FinancialDataConnector,
                                       formProvider: WhichVatPeriodToPayFormProvider,
                                       view: WhichVatPeriodToPayView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode): Action[AnyContent] = cc.authAndGetSavedAnswers.async {
    implicit request =>
      financialDataConnector.getCurrentPayments(request.vrn).map {
        case Right(payments) =>
          val allPayments = payments.overduePayments ++ payments.duePayments
          val paymentError = allPayments.exists(_.paymentStatus == PaymentStatus.Unknown)
          if(allPayments.size == 1) {
            redirectToOnlyPayment(allPayments.head)
          } else {
            Ok(view( form, mode, payments, paymentError = paymentError))
          }
        case _ => journeyRecovery()
      } recover {
        case e: HttpException =>
          logger.warn(s"Unexpected response from FinancialDataConnector: ${e.responseCode}")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = cc.authAndGetSavedAnswers.async {
    implicit request =>
      financialDataConnector.getCurrentPayments(request.vrn) map {
          case Right(payments) =>
            val allPayments = payments.overduePayments ++ payments.duePayments
            val paymentError = allPayments.exists(_.paymentStatus == PaymentStatus.Unknown)
            if(allPayments.size == 1) {
              redirectToOnlyPayment(allPayments.head)
            } else {
              form.bindFromRequest().fold(
                formWithErrors => BadRequest(view(formWithErrors, mode, payments, paymentError)),
                value => redirectToChosenPayment(allPayments, value)(request))
            }
          case _ => journeyRecovery()
        } recover {
          case e: HttpException =>
            logger.warn(s"Unexpected response from FinancialDataConnector: ${e.responseCode}")
            journeyRecovery()
        }
  }

  private def redirectToOnlyPayment(allPayments: Payment) =
    Redirect(routes.PaymentController.makePayment(allPayments.period, allPayments.amountOwed.longValue * 100))

  private def redirectToChosenPayment(allPayments: Seq[Payment], period: Period)
                             (implicit request: Request[_]) = {
    allPayments
      .find(_.period == period)
      .map(p => p.amountOwed.longValue * 100)
      .map(owed => Redirect(routes.PaymentController.makePayment(period, owed)))
      .getOrElse(journeyRecovery())
  }

  private def journeyRecovery() =
    Redirect(routes.JourneyRecoveryController.onPageLoad())

}

/*
 * Copyright 2023 HM Revenue & Customs
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
import models.Period
import models.financialdata.{Payment, PaymentStatus}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.payments.{NoPaymentsView, WhichVatPeriodToPayView}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhichVatPeriodToPayController @Inject()(
                                               cc: AuthenticatedControllerComponents,
                                               financialDataConnector: FinancialDataConnector,
                                               sessionRepository: SessionRepository,
                                               formProvider: WhichVatPeriodToPayFormProvider,
                                               view: WhichVatPeriodToPayView,
                                               viewNoPayment: NoPaymentsView
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      (for {
        financialDataResponse <- financialDataConnector.getCurrentPayments(request.vrn)
        sessionData <- sessionRepository.get(request.userId)
      } yield {
        financialDataResponse match {
          case Right(payments) =>
            val allPayments = payments.overduePayments ++ payments.duePayments
            val paymentError = allPayments.exists(_.paymentStatus == PaymentStatus.Unknown)
            if (allPayments.isEmpty) {
              val backToYourAccountUrl = sessionData.headOption.flatMap(_.get[String](ExternalReturnUrlQuery.path))
                .getOrElse(routes.YourAccountController.onPageLoad().url)
              Ok(viewNoPayment(backToYourAccountUrl))
            } else if (allPayments.size == 1) {
              redirectToOnlyPayment(allPayments.head)
            } else {
              Ok(view(form, payments, paymentError = paymentError))
            }
          case _ => journeyRecovery()
        }
      }) recover {
        case e: HttpException =>
          logger.warn(s"Unexpected response from FinancialDataConnector in onPageLoad: ${e.responseCode}")
          journeyRecovery()
      }
  }

  def onSubmit(): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      financialDataConnector.getCurrentPayments(request.vrn) map {
        case Right(payments) =>
          val allPayments = payments.overduePayments ++ payments.duePayments
          val paymentError = allPayments.exists(_.paymentStatus == PaymentStatus.Unknown)
          if (allPayments.size == 1) {
            redirectToOnlyPayment(allPayments.head)
          } else {
            form.bindFromRequest().fold(
              formWithErrors => BadRequest(view(formWithErrors, payments, paymentError)),
              value => redirectToChosenPayment(allPayments, value))
          }
        case _ => journeyRecovery()
      } recover {
        case e: HttpException =>
          logger.warn(s"Unexpected response from FinancialDataConnector in onSubmit: ${e.responseCode}")
          journeyRecovery()
      }
  }

  private def redirectToOnlyPayment(allPayments: Payment): Result =
    Redirect(routes.PaymentController.makePayment(allPayments.period, (allPayments.amountOwed * 100).toLong))

  private def redirectToChosenPayment(allPayments: Seq[Payment], period: Period): Result = {
    allPayments
      .find(_.period == period)
      .map(p => (p.amountOwed * 100).toLong)
      .map(owed => Redirect(routes.PaymentController.makePayment(period, owed)))
      .getOrElse(journeyRecovery())
  }

  private def journeyRecovery(): Result =
    Redirect(routes.JourneyRecoveryController.onPageLoad())

}

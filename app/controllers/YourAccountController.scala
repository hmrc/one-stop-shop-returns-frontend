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

import connectors.ReturnStatusConnector
import connectors.financialdata.FinancialDataConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.{PeriodWithStatus, SubmissionStatus}
import models.financialdata.VatReturnWithFinancialData
import models.requests.RegistrationRequest
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{FinancialDataService, VatReturnSalesService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       returnStatusConnector: ReturnStatusConnector,
                                       financialDataConnector: FinancialDataConnector,
                                       financialDataService: FinancialDataService,
                                       vatReturnSalesService: VatReturnSalesService,
                                       view: IndexView,
                                       clock: Clock,
                                       sessionRepository: SessionRepository
                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      val results = getPeriodsAndFinancialData()

      results.flatMap {
        case (Right(availablePeriodsWithStatus), Right(vatReturnsWithFinancialData)) =>
          val filteredPeriodsWithOutstandingAmounts =
            getFilteredPeriodsWithOutstandingAmounts(vatReturnsWithFinancialData)
          val duePeriodsWithOutstandingAmounts =
            filteredPeriodsWithOutstandingAmounts.filterNot(_.vatReturn.period.isOverdue(clock))
          val overduePeriodsWithOutstandingAmounts =
            filteredPeriodsWithOutstandingAmounts.filter(_.vatReturn.period.isOverdue(clock))
          val sortedPeriodsToSubmit = availablePeriodsWithStatus
            .filter(p => p.status == SubmissionStatus.Due || p.status == SubmissionStatus.Overdue).
            sortBy(_.period.firstDay.toEpochDay)

             sortedPeriodsToSubmit.headOption.map(p => sessionRepository.get(request.userId, p.period)).getOrElse(Future.successful(None))
               .map( u =>
                Ok(view(
                  request.registration.registeredCompanyName,
                  request.vrn.vrn,
                  availablePeriodsWithStatus
                    .filter(_.status == SubmissionStatus.Overdue)
                    .map(_.period),
                  availablePeriodsWithStatus
                    .find(_.status == SubmissionStatus.Due)
                    .map(_.period),
                  duePeriodsWithOutstandingAmounts,
                  overduePeriodsWithOutstandingAmounts,
                  filteredPeriodsWithOutstandingAmounts.exists(_.charge.isEmpty),
                  periodInProgress = u.map(_.period)
                )))
        case (Right(availablePeriodsWithStatus), Left(error)) =>
          logger.warn(s"There was an error with getting payment information $error")
          val sortedPeriodsToSubmit = availablePeriodsWithStatus
            .filter(p => p.status == SubmissionStatus.Due || p.status == SubmissionStatus.Overdue)
            .sortBy(_.period.firstDay.toEpochDay)
          sortedPeriodsToSubmit.headOption.map(p => sessionRepository.get(request.userId, p.period)).getOrElse(Future.successful(None))
            .map( u =>
              Ok(view(
                request.registration.registeredCompanyName,
                request.vrn.vrn,
                availablePeriodsWithStatus
                  .filter(_.status == SubmissionStatus.Overdue)
                  .map(_.period),
                availablePeriodsWithStatus
                  .find(_.status == SubmissionStatus.Due)
                  .map(_.period),
                Seq.empty,
                Seq.empty,
                paymentError = true,
                periodInProgress = u.map(_.period)
              )))
        case (Left(error), Left(error2)) =>
          logger.error(s"there was an error with period with status $error and getting periods with outstanding amounts $error2")
          throw new Exception(error.toString)
        case (Left(error), _) =>
          logger.error(s"there was an error during period with status $error")
          throw new Exception(error.toString)
        case (_, Left(error)) =>
          logger.error(s"there was an error getting periods with outstanding amounts $error")
          throw new Exception(error.toString)
      }
  }

  private def getFilteredPeriodsWithOutstandingAmounts(vatReturnsWithFinancialData: Seq[VatReturnWithFinancialData]) = {
    financialDataService
      .filterIfPaymentIsOutstanding(vatReturnsWithFinancialData)
      .map(vatReturnWithFinancialData =>
        vatReturnWithFinancialData.vatOwed match {
          case Some(_) => vatReturnWithFinancialData
          case _ =>
            vatReturnWithFinancialData.copy(
              vatOwed = Some(
                (vatReturnSalesService.getTotalVatOnSalesAfterCorrection(vatReturnWithFinancialData.vatReturn, vatReturnWithFinancialData.corrections) * 100).toLong
              )
            )
        }
      )
  }

  private def getPeriodsAndFinancialData()(implicit request: RegistrationRequest[AnyContent] ) = {
    for {
      availablePeriodsWithStatus <- returnStatusConnector.listStatuses(request.registration.commencementDate)
      vatReturnsWithFinancialData <- financialDataConnector.getVatReturnWithFinancialData(request.registration.commencementDate)
    } yield (availablePeriodsWithStatus, vatReturnsWithFinancialData)
  }

}

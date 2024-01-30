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

package services

import connectors.ReturnStatusConnector
import models.{PartialReturnPeriod, Period, PeriodWithStatus, SubmissionStatus}
import models.registration.Registration
import services.exclusions.ExclusionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartialReturnPeriodService @Inject()(
                                            exclusionService: ExclusionService,
                                            returnStatusConnector: ReturnStatusConnector
                                          )(implicit ec: ExecutionContext) {

  def getPartialReturnPeriod(registration: Registration, period: Period)(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {
    registration.excludedTrader match {
      case None =>
        getMaybeFirstPartialReturnPeriod(registration)
      case Some(excludedTrader) =>
        excludedTrader.exclusionReason match {
          case 6 =>
            exclusionService.currentReturnIsFinal(registration, period).map {
              case true =>
                val maybeEndDate = excludedTrader.effectiveDate.map(_.minusDays(1))
                Some(PartialReturnPeriod(
                  period.firstDay,
                  maybeEndDate.getOrElse(period.lastDay),
                  period.year,
                  period.quarter
                ))
              case _ => None
            }
          case _ => Future.successful(None)
        }
    }
  }

  private def getMaybeFirstPartialReturnPeriod(registration: Registration)(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {
    returnStatusConnector.listStatuses(registration.commencementDate).map {
      case Right(returnPeriods) if isFirstPeriod(returnPeriods, registration.commencementDate) =>
        val firstReturnPeriod = returnPeriods.head.period
        registration.transferringMsidEffectiveFromDate.flatMap {
          case transferringMsidEffectiveFromDate if isWithinPeriod(firstReturnPeriod, transferringMsidEffectiveFromDate) =>
            Some(PartialReturnPeriod(
              transferringMsidEffectiveFromDate,
              firstReturnPeriod.lastDay,
              firstReturnPeriod.year,
              firstReturnPeriod.quarter
            ))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  private def isFirstPeriod(periods: Seq[PeriodWithStatus], checkDate: LocalDate): Boolean = {
    val firstUnsubmittedPeriod = periods.filter(period =>
      Seq(SubmissionStatus.Next, SubmissionStatus.Due, SubmissionStatus.Overdue).contains(period.status)
    ).head
    isWithinPeriod(firstUnsubmittedPeriod.period, checkDate)
  }

  private def isWithinPeriod(period: Period, checkDate: LocalDate): Boolean =
    !checkDate.isBefore(period.firstDay) &&
      !checkDate.isAfter(period.lastDay)

}

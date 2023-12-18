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

package services

import base.SpecBase
import connectors.ReturnStatusConnector
import models.{PartialReturnPeriod, PeriodWithStatus, StandardPeriod, SubmissionStatus}
import models.Quarter.Q4
import models.exclusions.ExcludedTrader
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{mock, when}
import services.exclusions.ExclusionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PartialReturnPeriodServiceSpec extends SpecBase {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockReturnStatusConnector = mock[ReturnStatusConnector]
  private val mockExclusionService = mock[ExclusionService]

  "PartialReturnPeriodService#getPartialReturnPeriod should" - {

    "return a partial return period when it's the first return and transferring msid" in {

      val startDate = period.lastDay.minusDays(10)
      val commencementDate = startDate.plusDays(1)

      val registrationWithTransferringMsidEffectiveFromDate = registration
        .copy(commencementDate = commencementDate, transferringMsidEffectiveFromDate = Some(startDate))

      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Future(Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))))

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithTransferringMsidEffectiveFromDate, period).futureValue mustBe
        Some(PartialReturnPeriod(startDate, period.lastDay, period.year, period.quarter))
    }

    "return none when it's not first period" in {

      val startDate = period.lastDay.minusDays(10)

      val registrationWithTransferringMsidEffectiveFromDate = registration.copy(transferringMsidEffectiveFromDate = Some(startDate))

      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Future(Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))))

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithTransferringMsidEffectiveFromDate, period).futureValue mustBe
        None
    }

    "return none when effective date falls outside of first period" in {

      val startDate = period.lastDay.plusDays(10)
      val commencementDate = startDate.plusDays(1)

      val registrationWithTransferringMsidEffectiveFromDate = registration.copy(commencementDate = commencementDate, transferringMsidEffectiveFromDate = Some(startDate))

      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Future(Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))))

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithTransferringMsidEffectiveFromDate, period).futureValue mustBe
        None
    }

    "return a partial return for a excluded trader's final return" in {
      val excludedEffectiveDate = period.lastDay.minusDays(10)
      val endDate = excludedEffectiveDate.minusDays(1)
      val commencementDate = period.firstDay.plusDays(4)
      val excludedTrader = ExcludedTrader(vrn, 6, period, Some(excludedEffectiveDate))

      val registrationWithExcludedTrader = registration
        .copy(commencementDate = commencementDate, excludedTrader = Some(excludedTrader))

      when(mockExclusionService.currentReturnIsFinal(any(), any())(any(), any())) thenReturn Future.successful(true)

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithExcludedTrader, period).futureValue mustBe
        Some(PartialReturnPeriod(period.firstDay, endDate, period.year, period.quarter))
    }

    "return none for a excluded trader's non final return" in {
      val endDate = period.lastDay.minusDays(10)
      val commencementDate = period.firstDay.plusDays(4)
      val excludedTrader = ExcludedTrader(vrn, 6, period, Some(endDate))

      val registrationWithExcludedTrader = registration
        .copy(commencementDate = commencementDate, excludedTrader = Some(excludedTrader))

      when(mockExclusionService.currentReturnIsFinal(any(), any())(any(), any())) thenReturn Future.successful(false)

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithExcludedTrader, period).futureValue mustBe
        None
    }

    "return none for a excluded trader that isn't code 6" in {
      val endDate = period.lastDay.minusDays(10)
      val commencementDate = period.firstDay.plusDays(4)
      val excludedTrader = ExcludedTrader(vrn, 4, period, Some(endDate))

      val registrationWithExcludedTrader = registration
        .copy(commencementDate = commencementDate, excludedTrader = Some(excludedTrader))

      when(mockExclusionService.currentReturnIsFinal(any(), any())(any(), any())) thenReturn Future.successful(true)

      val service = new PartialReturnPeriodService(mockExclusionService, mockReturnStatusConnector)

      service.getPartialReturnPeriod(registrationWithExcludedTrader, period).futureValue mustBe
        None
    }

  }

}

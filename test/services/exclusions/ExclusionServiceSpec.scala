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

package services.exclusions

import base.SpecBase
import connectors.VatReturnConnector
import models.Period
import models.Quarter.{Q2, Q3}
import models.exclusions.ExcludedTrader
import models.registration.Registration
import models.requests.RegistrationRequest
import models.responses.NotFound
import org.mockito.{Mockito, MockitoSugar}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ExclusionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit private lazy val ec: ExecutionContext = ExecutionContext.global

  private val mockRegistrationRequest = mock[RegistrationRequest[AnyContent]]
  private val mockRegistration = mock[Registration]
  private val vatReturnConnector = mock[VatReturnConnector]
  private val exclusionService = new ExclusionService(vatReturnConnector)

  private val exclusionCode = Gen.oneOf("02", "04", "01", "03", "05", "06").sample.value.toInt
  private val exclusionReason = Gen.oneOf("01", "02", "03", "04", "05", "06", "-01").sample.value.toInt
  private val exclusionPeriod = Period(2022, Q3)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationRequest)
    super.beforeEach()
  }

  ".hasSubmittedFinalReturn" - {

    "must return true if final return completed" in {
      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
          Some(ExcludedTrader(Vrn("123456789"), exclusionCode, exclusionReason, exclusionPeriod))

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))

      exclusionService.hasSubmittedFinalReturn(mockRegistrationRequest.registration)(hc, ec).futureValue mustBe true
    }

    "must return false if final return not completed" in {
      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionCode, exclusionReason, exclusionPeriod))

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      exclusionService.hasSubmittedFinalReturn(mockRegistrationRequest.registration)(hc, ec).futureValue mustBe false
    }
  }

  ".currentReturnIsFinal" - {

    "must return true if current return is final" in {

      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionCode, exclusionReason, exclusionPeriod))

      when(vatReturnConnector.get(eqTo(exclusionPeriod))(any())) thenReturn Future.successful(Left(NotFound))

      exclusionService.currentReturnIsFinal(mockRegistrationRequest.registration, Period(2022, Q3))(hc, ec).futureValue mustBe true
    }

    "must return false if the current return is not final for excluded trader" in {

      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionCode, exclusionReason, exclusionPeriod))

      when(vatReturnConnector.get(eqTo(exclusionPeriod))(any())) thenReturn Future.successful(Right(completeVatReturn))

      exclusionService.currentReturnIsFinal(mockRegistrationRequest.registration, Period(2022, Q2))(hc, ec).futureValue mustBe false
    }
  }

}

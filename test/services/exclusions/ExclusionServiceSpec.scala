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

package services.exclusions

import base.SpecBase
import config.FrontendAppConfig
import connectors.VatReturnConnector
import models.Quarter.{Q2, Q3}
import models.StandardPeriod
import models.exclusions.{ExcludedTrader, ExclusionLinkView, ExclusionReason, ExclusionViewType}
import models.registration.Registration
import models.requests.RegistrationRequest
import models.responses.NotFound
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{Mockito, MockitoSugar}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.test.Helpers.running
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class ExclusionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit private lazy val ec: ExecutionContext = ExecutionContext.global

  private val mockRegistrationRequest: RegistrationRequest[AnyContent] = mock[RegistrationRequest[AnyContent]]
  private val mockRegistration: Registration = mock[Registration]
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]
  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val exclusionService = new ExclusionService(mockVatReturnConnector, mockFrontendAppConfig, stubClockAtArbitraryDate)

  private val exclusionReason = Gen.oneOf(ExclusionReason.values.filterNot(x => Seq(ExclusionReason.TransferringMSID, ExclusionReason.Reversal).contains(x))).sample.value
  private val finalReturnPeriod = StandardPeriod(2022, Q2)
  private val effectiveDate = StandardPeriod(2022, Q3).firstDay

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationRequest)
    super.beforeEach()
  }

  ".hasSubmittedFinalReturn" - {

    "must return true if final return completed" in {
      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionReason, effectiveDate, quarantined = false))

      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))

      exclusionService.hasSubmittedFinalReturn(mockRegistrationRequest.registration)(hc, ec).futureValue mustBe true
    }

    "must return false if final return not completed" in {
      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionReason, effectiveDate, quarantined = false))

      when(mockVatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      exclusionService.hasSubmittedFinalReturn(mockRegistrationRequest.registration)(hc, ec).futureValue mustBe false
    }
  }

  ".currentReturnIsFinal" - {

    "must return true if current return is final" in {

      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionReason, effectiveDate, quarantined = false))

      when(mockVatReturnConnector.get(eqTo(finalReturnPeriod))(any())) thenReturn Future.successful(Left(NotFound))

      exclusionService.currentReturnIsFinal(
        mockRegistrationRequest.registration,
        finalReturnPeriod
      )(hc, ec).futureValue mustBe true
    }

    "must return false if the current return is not final for excluded trader" in {

      when(mockRegistrationRequest.registration) thenReturn mockRegistration

      when(mockRegistration.excludedTrader) thenReturn
        Some(ExcludedTrader(Vrn("123456789"), exclusionReason, effectiveDate, quarantined = false))

      when(mockVatReturnConnector.get(eqTo(finalReturnPeriod))(any())) thenReturn Future.successful(Right(completeVatReturn))

      exclusionService.currentReturnIsFinal(
        mockRegistrationRequest.registration,
        finalReturnPeriod
      )(hc, ec).futureValue mustBe false
    }
  }

  ".calculateExclusionViewType" - {

    "must return Default if not excluded trader" in {

      exclusionService.calculateExclusionViewType(
        excludedTrader = None,
        canCancel = false,
        hasSubmittedFinalReturn = false,
        hasDueReturnsLessThanThreeYearsOld = false,
        hasDueReturnThreeYearsOld = false,
        hasDeregisteredFromVat = false
      ) mustBe ExclusionViewType.Default
    }

    "must return RejoinEligible if excluded trader can't cancel" in {

      val excludedTrader = ExcludedTrader(
        vrn = vrn,
        exclusionReason = ExclusionReason.NoLongerSupplies,
        effectiveDate = effectiveDate,
        quarantined = false
      )

      exclusionService.calculateExclusionViewType(
        excludedTrader = Some(excludedTrader),
        canCancel = false,
        hasSubmittedFinalReturn = true,
        hasDueReturnsLessThanThreeYearsOld = false,
        hasDueReturnThreeYearsOld = false,
        hasDeregisteredFromVat = false
      ) mustBe ExclusionViewType.RejoinEligible
    }

    "must return ReversalEligible if excluded trader can cancel" in {

      val excludedTrader = ExcludedTrader(
        vrn = vrn,
        exclusionReason = ExclusionReason.NoLongerSupplies,
        effectiveDate = effectiveDate,
        quarantined = false
      )

      exclusionService.calculateExclusionViewType(
        excludedTrader = Some(excludedTrader),
        canCancel = true,
        hasSubmittedFinalReturn = false,
        hasDueReturnsLessThanThreeYearsOld = false,
        hasDueReturnThreeYearsOld = false,
        hasDeregisteredFromVat = false
      ) mustBe ExclusionViewType.ReversalEligible
    }

    "must return ExcludedFinalReturnPending if excluded trader can't cancel and hasn't submitted final return" in {

      val excludedTrader = ExcludedTrader(
        vrn = vrn,
        exclusionReason = ExclusionReason.NoLongerSupplies,
        effectiveDate = effectiveDate,
        quarantined = false
      )

      exclusionService.calculateExclusionViewType(
        excludedTrader = Some(excludedTrader),
        canCancel = false,
        hasSubmittedFinalReturn = false,
        hasDueReturnsLessThanThreeYearsOld = false,
        hasDueReturnThreeYearsOld = false,
        hasDeregisteredFromVat = false
      ) mustBe ExclusionViewType.ExcludedFinalReturnPending
    }

    "must return Quarantined if trader is quarantined" in {

      val instant = Instant.parse("2024-03-31T12:00:00Z")
      val newClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

      val exclusionService = new ExclusionService(mockVatReturnConnector, mockFrontendAppConfig, newClock)

      val effectiveDate = StandardPeriod(2022, Q2).firstDay

      val excludedTrader = ExcludedTrader(
        vrn = vrn,
        exclusionReason = ExclusionReason.FailsToComply,
        effectiveDate = effectiveDate,
        quarantined = true
      )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        clock = Some(newClock),
        registration = registration.copy(excludedTrader = Some(excludedTrader))
      ).build()

      running(application) {

        exclusionService.calculateExclusionViewType(
          excludedTrader = Some(excludedTrader),
          canCancel = false,
          hasSubmittedFinalReturn = false,
          hasDueReturnsLessThanThreeYearsOld = false,
          hasDueReturnThreeYearsOld = false,
          hasDeregisteredFromVat = false
        ) mustBe ExclusionViewType.Quarantined
      }
    }

    "must return ExcludedFinalReturnPending if trader is no longer quarantined" in {

      val instant = Instant.parse("2024-04-01T12:00:00Z")
      val newClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

      val exclusionService = new ExclusionService(mockVatReturnConnector, mockFrontendAppConfig, newClock)

      val effectiveDate = StandardPeriod(2022, Q2).firstDay

      val excludedTrader = ExcludedTrader(
        vrn = vrn,
        exclusionReason = ExclusionReason.FailsToComply,
        effectiveDate = effectiveDate,
        quarantined = true
      )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        clock = Some(newClock),
        registration = registration.copy(excludedTrader = Some(excludedTrader))
      ).build()

      running(application) {

        exclusionService.calculateExclusionViewType(
          excludedTrader = Some(excludedTrader),
          canCancel = false,
          hasSubmittedFinalReturn = false,
          hasDueReturnsLessThanThreeYearsOld = false,
          hasDueReturnThreeYearsOld = false,
          hasDeregisteredFromVat = false
        ) mustBe ExclusionViewType.ExcludedFinalReturnPending
      }
    }
  }

  ".getLink" - {

    "must not return an exclusion view type when trader is Quarantined" in {

      val exclusionViewType: ExclusionViewType = ExclusionViewType.Quarantined

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult = None

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }

    "must not return an exclusion view type when trader is ExcludedFinalReturnPending" in {

      val exclusionViewType: ExclusionViewType = ExclusionViewType.ExcludedFinalReturnPending

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult = None

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }

    "must not return an exclusion view type when trader is DeregisteredTrader" in {

      val exclusionViewType: ExclusionViewType = ExclusionViewType.DeregisteredTrader

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult = None

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }

    "must return a rejoin this service exclusion view type when trader is eligible to rejoin the service" in {

      when(mockFrontendAppConfig.rejoinThisService) thenReturn "/start-rejoin-journey"

      val exclusionViewType: ExclusionViewType = ExclusionViewType.RejoinEligible

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult: Option[ExclusionLinkView] =
          Some(ExclusionLinkView(
            displayText = msgs("index.details.rejoinService"),
            id = "rejoin-this-service",
            href = "/start-rejoin-journey"
          ))

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }

    "must return a cancel your request to leave this service exclusion view type when trader is eligible to cancel their request to leave the service" in {

      when(mockFrontendAppConfig.leaveOneStopShopUrl) thenReturn "/leave-one-stop-shop"

      val exclusionViewType: ExclusionViewType = ExclusionViewType.ReversalEligible

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult: Option[ExclusionLinkView] =
          Some(ExclusionLinkView(
            displayText = msgs("index.details.cancelRequestToLeave"),
            id = "cancel-request-to-leave",
            href = "/leave-one-stop-shop/cancel-leave-scheme"
          ))

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }

    "must return a leave this service exclusion view type when trader is eligible to leave the service" in {

      when(mockFrontendAppConfig.leaveOneStopShopUrl) thenReturn "/leave-one-stop-shop"

      val exclusionViewType: ExclusionViewType = ExclusionViewType.Default

      val application = applicationBuilder().build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val expectedResult: Option[ExclusionLinkView] =
          Some(ExclusionLinkView(
            displayText = msgs("index.details.leaveThisService"),
            id = "leave-this-service",
            href = "/leave-one-stop-shop"
          ))

        val result = exclusionService.getLink(exclusionViewType)

        result mustBe expectedResult
      }
    }
  }
}

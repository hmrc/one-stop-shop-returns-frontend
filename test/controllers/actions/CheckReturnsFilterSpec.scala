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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import connectors.VatReturnConnector
import controllers.routes
import models.etmp.EtmpObligationDetails
import models.etmp.EtmpObligationsFulfilmentStatus.{Fulfilled, Open}
import models.requests.OptionalDataRequest
import models.responses.{NotFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_GATEWAY
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import repositories.{CachedVatReturnRepository, CachedVatReturnWrapper}
import services.ObligationsService
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(
                 repository: CachedVatReturnRepository,
                 connector: VatReturnConnector,
                 obligationsService: ObligationsService,
                 frontendAppConfig: FrontendAppConfig
               ) extends CheckReturnsFilterImpl(period, repository, connector, obligationsService, frontendAppConfig) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[VatReturnConnector]
  private val mockRepository = mock[CachedVatReturnRepository]
  private val mockObligationService = mock[ObligationsService]
  private val mockFrontendAppConfig = mock[FrontendAppConfig]
  private val cachedVatReturnWrapper = CachedVatReturnWrapper(userAnswersId, period, Some(completeVatReturn), arbitraryInstant)
  private val emptyCachedVatReturnWrapper = CachedVatReturnWrapper(userAnswersId, period, None, arbitraryInstant)

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
    Mockito.reset(mockRepository)
    Mockito.reset(mockObligationService)
    Mockito.reset(mockFrontendAppConfig)
  }

  ".filter" - {

    "when strategic returns enabled" - {

      val periodKey: String = s"${period.year.toString.substring(2, 4)}${period.quarter}"

      val obligationDetails: Seq[EtmpObligationDetails] =
        Seq(
          EtmpObligationDetails(
            status = Fulfilled,
            periodKey = periodKey
          ),
          EtmpObligationDetails(
            status = Fulfilled,
            periodKey = "21Q4"
          ),
          EtmpObligationDetails(
            status = Open,
            periodKey = "22Q1"
          )
        )

      "must redirect to Previous Return page when an existing return for the period is found" in {

        when(mockFrontendAppConfig.strategicReturnApiEnabled) thenReturn true
        when(mockObligationService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture

        val app = applicationBuilder(None)
          .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
          .build()

        running(app) {
          val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
          val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Redirect(routes.PreviousReturnController.onPageLoad(period))

          verify(mockObligationService, times(1)).getFulfilledObligations(any())(any())
        }
      }

      "must return None when an existing return for the period is not found" in {

        when(mockFrontendAppConfig.strategicReturnApiEnabled) thenReturn true
        when(mockObligationService.getFulfilledObligations(any())(any())) thenReturn Seq.empty.toFuture

        val app = applicationBuilder(None)
          .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
          .build()

        running(app) {
          val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
          val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

          val result = controller.callFilter(request).futureValue

          result must not be defined

          verify(mockObligationService, times(1)).getFulfilledObligations(any())(any())
        }
      }
    }

    "when strategic returns disabled" - {

      "when there is no vat return in the repository" - {

        "and a vat return can be retrieved from the backend" - {

          "must save the vat return to the repository and return Right" in {

            when(mockRepository.get(userAnswersId, period)) thenReturn None.toFuture
            when(mockRepository.set(any(), any(), any())) thenReturn true.toFuture
            when(mockConnector.get(any())(any())) thenReturn Right(completeVatReturn).toFuture

            val app = applicationBuilder(None)
              .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
              .overrides(bind[VatReturnConnector].toInstance(mockConnector))
              .build()

            running(app) {
              val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
              val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

              val result = controller.callFilter(request).futureValue

              result.value mustBe Redirect(routes.PreviousReturnController.onPageLoad(period))

              verify(mockRepository, times(1)).set(eqTo(userAnswersId), eqTo(period), eqTo(Some(completeVatReturn)))
              verify(mockRepository, times(1)).get(eqTo(userAnswersId), eqTo(period))
            }
          }
        }

        "and a vat return cannot be retrieved from the backend" - {

          "must return an empty Cached Vat Return Wrapper when an existing vat return is not found in the backend" in {

            when(mockConnector.get(any())(any())) thenReturn Left(NotFound).toFuture
            when(mockRepository.set(any(), any(), any())) thenReturn true.toFuture
            when(mockRepository.get(userAnswersId, period)) thenReturn Some(emptyCachedVatReturnWrapper).toFuture

            val app = applicationBuilder(None)
              .overrides(bind[VatReturnConnector].toInstance(mockConnector))
              .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
              .build()

            running(app) {
              val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
              val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

              val result = controller.callFilter(request).futureValue

              result must not be defined

              verify(mockRepository, times(1)).get(eqTo(userAnswersId), eqTo(period))
            }
          }

          "must throw an Exception when any other error is returned from the backend" in {

            when(mockConnector.get(any())(any())) thenReturn Left(UnexpectedResponseStatus(BAD_GATEWAY, "ERROR")).toFuture
            when(mockRepository.set(any(), any(), any())) thenReturn false.toFuture
            when(mockRepository.get(userAnswersId, period)) thenReturn None.toFuture

            val app = applicationBuilder(None)
              .overrides(bind[VatReturnConnector].toInstance(mockConnector))
              .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
              .build()

            running(app) {
              val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
              val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

              val result = controller.callFilter(request).failed

              whenReady(result) { exp =>
                exp mustBe a[Exception]
                exp.getMessage mustBe "Error when getting vat return Left(UnexpectedResponseStatus(502,ERROR))"
              }

              verify(mockRepository, times(1)).get(eqTo(userAnswersId), eqTo(period))
            }
          }
        }
      }

      "when there is a vat return in the repository" - {

        "must redirect to Vat Return Details page when an existing return is found" in {

          when(mockRepository.get(userAnswersId, period)) thenReturn Some(cachedVatReturnWrapper).toFuture

          val app = applicationBuilder(None)
            .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
            .build()

          running(app) {
            val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
            val controller = new Harness(mockRepository, mockConnector, mockObligationService, mockFrontendAppConfig)

            val result = controller.callFilter(request).futureValue

            result.value mustBe Redirect(routes.PreviousReturnController.onPageLoad(period))
          }
        }
      }
    }
  }
}

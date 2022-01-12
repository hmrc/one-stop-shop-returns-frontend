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

package controllers.actions

import base.SpecBase
import connectors.VatReturnConnector
import controllers.routes
import models.requests.OptionalDataRequest
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import repositories.{CachedVatReturnRepository, CachedVatReturnWrapper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(repository: CachedVatReturnRepository, connector: VatReturnConnector) extends CheckReturnsFilterImpl(period, repository, connector) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[VatReturnConnector]
  private val mockRepository = mock[CachedVatReturnRepository]
  private val cachedVatReturnWrapper = CachedVatReturnWrapper(userAnswersId, period, Some(completeVatReturn), arbitraryInstant)
  private val emptyCachedVatReturnWrapper = CachedVatReturnWrapper(userAnswersId, period, None, arbitraryInstant)


  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector, mockRepository)
  }

  ".filter" - {

    "when there is no vat return in the repository" - {

      "and a vat return can be retrieved from the backend" - {

        "must save the vat return to the repository and return Right" in {

          when(mockRepository.get(userAnswersId, period)) thenReturn Future.successful(None)
          when(mockRepository.set(any(), any(), any())) thenReturn Future.successful(true)
          when(mockConnector.get(any())(any())) thenReturn Future.successful(Right(completeVatReturn))

          val app = applicationBuilder(None)
            .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
            .overrides(bind[VatReturnConnector].toInstance(mockConnector))
            .build()

          running(app) {
            val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
            val controller = new Harness(mockRepository, mockConnector)

            val result = controller.callFilter(request).futureValue

            result.value mustEqual Redirect(routes.PreviousReturnController.onPageLoad(period))

            verify(mockRepository, times(1)).set(eqTo(userAnswersId), eqTo(period), eqTo(Some(completeVatReturn)))
            verify(mockRepository, times(1)).get(eqTo(userAnswersId), eqTo(period))
          }
        }

      }

      "and a vat return cannot be retrieved from the backend" - {

        "must return an empty Cached Vat Return Wrapper when an existing vat return is not found in the backend" in {

          when(mockConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))
          when(mockRepository.set(any(), any(), any())) thenReturn Future.successful(true)
          when(mockRepository.get(userAnswersId, period)) thenReturn Future.successful(Some(emptyCachedVatReturnWrapper))


          val app = applicationBuilder(None)
            .overrides(bind[VatReturnConnector].toInstance(mockConnector))
            .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
            .build()

          running(app) {
            val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
            val controller = new Harness(mockRepository, mockConnector)

            val result = controller.callFilter(request).futureValue

            result must not be defined

            verify(mockRepository, times(1)).get(eqTo(userAnswersId), eqTo(period))
          }
        }
      }
    }

    "when there is a vat return in the repository" - {

      "must redirect to Vat Return Details page when an existing return is found" in {

        when(mockRepository.get(userAnswersId, period)) thenReturn Future.successful(Some(cachedVatReturnWrapper))

        val app = applicationBuilder(None)
          .overrides(bind[CachedVatReturnRepository].toInstance(mockRepository))
          .build()

        running(app) {
          val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, registration, Some(emptyUserAnswers))
          val controller = new Harness(mockRepository, mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(routes.PreviousReturnController.onPageLoad(period))
        }
      }
    }
  }
}

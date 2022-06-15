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

package controllers.external

import base.SpecBase
import models.external.{ExternalRequest, ExternalResponse}
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.inject
import play.api.libs.json.{JsNull, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.external.ExternalService

class ExternalControllerSpec extends SpecBase {

  private val yourAccount = "your-account"
  private val returnsHistory = "returns-history"
  private val startReturn = "start-your-return"
  private val continueReturn = "continue-your-return"
  private val payment = "make-payment"
  private val externalRequest = ExternalRequest("BTA", "exampleurl")


  ".onExternal" - {

    "when correct ExternalRequest is posted" - {
      "must return OK" in {
        val mockExternalService = mock[ExternalService]

        when(mockExternalService.getExternalResponse(any(), any(), any(), any(), any())) thenReturn Right(ExternalResponse("url"))

        val application = applicationBuilder()
          .overrides(inject.bind[ExternalService].toInstance(mockExternalService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ExternalController.onExternal(yourAccount).url).withJsonBody(
            Json.toJson(externalRequest)
          )

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse("url")
        }
      }

      "when navigating to payment page must return OK" in {
        val mockExternalService = mock[ExternalService]

        when(mockExternalService.getExternalResponse(any(), any(), any(), any(), any())) thenReturn Right(ExternalResponse("url"))

        val application = applicationBuilder()
          .overrides(inject.bind[ExternalService].toInstance(mockExternalService))
          .build()

        running(application) {
          val request = FakeRequest(
            POST,
            routes.ExternalController.onExternal(payment).url).withJsonBody(
            Json.toJson(externalRequest)
          )

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse("url")
        }
      }

      "must respond with NotFound and not save return url if service responds with NotFound" - {
        "because no period provided where needed" in {
          val mockExternalService = mock[ExternalService]

          when(mockExternalService.getExternalResponse(any(), any(), any(), any(), any())) thenReturn Left(NotFound)

          val application = applicationBuilder()
            .overrides(inject.bind[ExternalService].toInstance(mockExternalService))
            .build()

          running(application) {
            val request = FakeRequest(POST, routes.ExternalController.onExternal(startReturn, None).url).withJsonBody(
              Json.toJson(externalRequest)
            )

            val result = route(application, request).value
            status(result) mustBe NOT_FOUND
          }
        }
      }
    }

    "must respond with BadRequest" - {
      "when no body provided" in {
        val application = applicationBuilder()
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ExternalController.onExternal(startReturn, Some(period)).url).withJsonBody(JsNull)

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }

      "when malformed body provided" in {
        val application = applicationBuilder()
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.ExternalController.onExternal(startReturn, Some(period)).url).withJsonBody(Json.toJson("wrong body"))

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }
    }

  }
}

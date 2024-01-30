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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import generators.Generators
import models.registration.Registration
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class RegistrationConnectorSpec
  extends SpecBase
    with WireMockHelper
    with ScalaCheckPropertyChecks
    with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-registration.port" -> server.port)
      .build()
  }

  ".get" - {

    "must return a registration when the server provides one" in {

      val url = s"/one-stop-shop-registration/registration"
      val app = application

      running(app) {
        val connector    = app.injector.instanceOf[RegistrationConnector]
        val registration = arbitrary[Registration].sample.value

        val responseBody = Json.toJson(registration).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.get().futureValue

        result.value mustEqual registration
      }
    }

    "must return None when the server responds with NOT_FOUND" in {

      val url = s"/one-stop-shop-registration/registration"
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]

        server.stubFor(get(urlEqualTo(url)).willReturn(notFound()))

        val result = connector.get().futureValue

        result must not be defined
      }
    }
  }

  ".enrolUser" - {

    "must return 204 when successful response" in {

      val url = s"/one-stop-shop-registration/confirm-enrolment"
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]

        server.stubFor(post(urlEqualTo(url)).willReturn(noContent()))

        val result = connector.enrolUser().futureValue

        result.status mustEqual NO_CONTENT
      }

    }

  }
}

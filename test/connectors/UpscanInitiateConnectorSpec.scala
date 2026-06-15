/*
 * Copyright 2026 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class UpscanInitiateConnectorSpec extends SpecBase with WireMockHelper with Matchers {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure(
        "microservice.services.upscan-initiate.host" -> "localhost",
        "microservice.services.upscan-initiate.port" -> server.port,
        "microservice.services.upscan-initiate.protocol" -> "http",
        "upscan.maxUploadFileSizeMb" -> 4,
        "upscan.callback-url" -> "http://localhost:9999/upscan/callback"
      ).build()
  }

  private val upscanInitiateResponseJson = {
    """
      |{
      |  "reference": "reference-1234",
      |  "uploadRequest": {
      |    "href": "https://s3-upload-url",
      |    "fields": {
      |      "key": "value"
      |    }
      |  }
      |}
      |""".stripMargin
  }

  "UpscanInitiateConnector.initiateV2" - {

    "return a successful UpscanInitiateResponse" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[UpscanInitiateConnector]

        server.stubFor(
          post(urlEqualTo("/upscan/v2/initiate"))
            .willReturn(aResponse().withStatus(OK).withBody(upscanInitiateResponseJson))
        )

        val result = connector.initiateV2(
          redirectOnSuccess = Some("http://success"),
          redirectOnError = Some("http://error")
        ).futureValue

        result.fileReference.reference mustBe "reference-1234"
        result.postTarget mustBe "https://s3-upload-url"
        result.formFields mustBe Map("key" -> "value")
      }
    }

    "fail when upscan returns an error" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[UpscanInitiateConnector]

        server.stubFor(
          post(urlEqualTo("/upscan/v2/initiate"))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error"))
        )

        val result = connector.initiateV2(
          redirectOnSuccess = Some("http://success"),
          redirectOnError = Some("http://error")
        )

        whenReady(result.failed) { ex =>
          ex mustBe a[uk.gov.hmrc.http.UpstreamErrorResponse]

          val upstream = ex.asInstanceOf[uk.gov.hmrc.http.UpstreamErrorResponse]
          upstream.statusCode mustBe INTERNAL_SERVER_ERROR
        }
      }

    }
  }

}

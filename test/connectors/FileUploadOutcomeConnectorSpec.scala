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
import models.responses.{ConflictFound, InternalServerError, NotFound}
import models.upscan.FileUploadOutcome
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets

class FileUploadOutcomeConnectorSpec extends SpecBase with WireMockHelper with Matchers {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.one-stop-shop-returns.port" -> server.port)
      .build()
  }

  private val fileUploadOutcomeJson =
    """
      |{
      |   "fileName": "test.csv",
      |   "status": "READY"
      |}
      |""".stripMargin


  "FileUploadOutcomeConnector.get" - {

    def url = s"/one-stop-shop-returns/file-upload-outcome/fake-ref"

    "must return Some(fileName) when backend returns 200" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(fileUploadOutcomeJson)
            )
        )

        val result = connector.getOutcome("fake-ref").futureValue

        result mustBe Some(
          FileUploadOutcome(
            fileName = Some("test.csv"),
            status = "READY",
            failureReason = None
          )
        )
      }
    }

    "must return None when backend returns 404" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/one-stop-shop-returns/file-upload-outcome/unknown-ref"))
            .willReturn(aResponse()
              .withStatus(404)
            )
        )

        val result = connector.getOutcome("unknown-ref").futureValue

        result mustBe None
      }
    }

    "must return None when backend returns error" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/one-stop-shop-returns/file-upload-outcome/fail-ref"))
            .willReturn(aResponse()
              .withStatus(500)
            )
        )

        val result = connector.getOutcome("fail-ref").futureValue

        result mustBe None
      }
    }
  }

  "FileUploadOutcomeConnector.getCsv" - {

    def url = s"/one-stop-shop-returns/file-upload-csv/fake-ref"

    "must return a csv when backend returns 200" in {
      val app = application

      val validCSVContent: String =
        """"HM Revenue and Customs logo","","",""
          |"One Stop Shop VAT return","","",""
          |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
          |"Northern Ireland","Germany","12.50%","£1200","£140"
          |"Northern Ireland","France","15","33,333","£4423"
          |"Austria","France","10%","150.01","£15"
          |"France","Austria","12.50%","£1200","£140"
          |""".stripMargin

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "text/csv; charset=utf-8")
              .withBody(validCSVContent.getBytes(StandardCharsets.UTF_8))
            )
        )

        val result = connector.getCsv("fake-ref").futureValue

        result mustBe Right(validCSVContent)
      }
    }

    "must return Left(NotFound) when backend returns 404" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/one-stop-shop-returns/file-upload-csv/fail-ref"))
            .willReturn(aResponse().withStatus(404).withBody("Server error"))
        )

        val result = connector.getCsv("fail-ref").futureValue

        result mustBe Left(NotFound)
      }

    }

    "must return Left(ConflictFound) when backend returns 409" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/one-stop-shop-returns/file-upload-csv/fail-ref"))
            .willReturn(aResponse().withStatus(409).withBody("Server error"))
        )

        val result = connector.getCsv("fail-ref").futureValue

        result mustBe Left(ConflictFound)
      }

    }

    "must return Left(InternalServerError) when backend returns 500" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/one-stop-shop-returns/file-upload-csv/fail-ref"))
            .willReturn(aResponse().withStatus(500).withBody("Server error"))
        )

        val result = connector.getCsv("fail-ref").futureValue

        result mustBe Left(InternalServerError)
      }
    }
  }

}

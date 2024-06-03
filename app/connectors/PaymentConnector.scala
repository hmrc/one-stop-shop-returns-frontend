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

import config.Service
import connectors.PaymentHttpParser.{ReturnPaymentReads, ReturnPaymentResponse}
import logging.Logging
import models.requests.PaymentRequest
import models.responses.UnexpectedResponseStatus
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                (implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val baseUrl = config.get[Service]("microservice.services.pay-api")

  def submit(paymentRequest: PaymentRequest)(implicit hc: HeaderCarrier): Future[ReturnPaymentResponse] = {
    val url: URL = url"$baseUrl/vat-oss/ni-eu-vat-oss/journey/start"

    httpClientV2.post(url).withBody(Json.toJson(paymentRequest)).execute[ReturnPaymentResponse].recover {
      case e: HttpException =>
        logger.error(s"PaymentResponse received unexpected error with status: ${e.responseCode}")
        Left(UnexpectedResponseStatus(e.responseCode, s"Unexpected response, status ${e.responseCode} returned"))
    }
  }
}

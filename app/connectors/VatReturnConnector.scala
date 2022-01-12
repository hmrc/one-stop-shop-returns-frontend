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

package connectors

import config.Service
import connectors.VatReturnHttpParser._
import connectors.VatReturnWithCorrectionHttpParser._
import models.Period
import models.requests.{VatReturnRequest, VatReturnWithCorrectionRequest}
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                  (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def submit(vatReturnRequest: VatReturnRequest)(implicit hc: HeaderCarrier): Future[VatReturnResponse] = {
    val url = s"$baseUrl/vat-returns"

    httpClient.POST[VatReturnRequest, VatReturnResponse](url, vatReturnRequest)
  }

  def submitWithCorrection(vatReturnRequest: VatReturnWithCorrectionRequest)(implicit hc: HeaderCarrier): Future[VatReturnWithCorrectionResponse] = {
    val url = s"$baseUrl/vat-return-with-corrections"

    httpClient.POST[VatReturnWithCorrectionRequest, VatReturnWithCorrectionResponse](url, vatReturnRequest)
  }

  def get(period: Period)(implicit hc: HeaderCarrier): Future[VatReturnResponse] = {
    val url = s"$baseUrl/vat-returns/${period.toString}"

    httpClient.GET[VatReturnResponse](url)
  }
}

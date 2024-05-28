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
import connectors.ExternalEntryUrlHttpParser.{ExternalEntryUrlResponse, _}
import connectors.VatReturnHttpParser._
import connectors.VatReturnWithCorrectionHttpParser._
import models.Period
import models.domain.VatReturn
import models.requests.{VatReturnRequest, VatReturnWithCorrectionRequest}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                  (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def submit(vatReturnRequest: VatReturnRequest)(implicit hc: HeaderCarrier): Future[VatReturnResponse] =
    httpClientV2.post(url"$baseUrl/vat-returns").withBody(Json.toJson(vatReturnRequest)).execute[VatReturnResponse]

  def submitWithCorrections(vatReturnRequest: VatReturnWithCorrectionRequest)(implicit hc: HeaderCarrier): Future[VatReturnWithCorrectionResponse] =
    httpClientV2.post(url"$baseUrl/vat-return-with-corrections").withBody(Json.toJson( vatReturnRequest)).execute[VatReturnWithCorrectionResponse]

  def get(period: Period)(implicit hc: HeaderCarrier): Future[VatReturnResponse] =
    httpClientV2.get(url"$baseUrl/vat-returns/period/$period").execute[VatReturnResponse]

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] =
    httpClientV2.get(url"$baseUrl/external-entry").execute[ExternalEntryUrlResponse]

  def getSubmittedVatReturns()(implicit hc: HeaderCarrier): Future[Seq[VatReturn]] = {
    httpClient.GET[Seq[VatReturn]](s"$baseUrl/vat-returns")
  }
}

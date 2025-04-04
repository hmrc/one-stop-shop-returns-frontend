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
import models.etmp.EtmpObligations
import models.requests.{VatReturnRequest, VatReturnWithCorrectionRequest}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HttpReads.Implicits._
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
    httpClientV2.post(url"$baseUrl/vat-return-with-corrections").withBody(Json.toJson(vatReturnRequest)).execute[VatReturnWithCorrectionResponse]

  def get(period: Period)(implicit hc: HeaderCarrier): Future[VatReturnResponse] =
    httpClientV2.get(url"$baseUrl/vat-returns/period/$period").execute[VatReturnResponse]

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] =
    httpClientV2.get(url"$baseUrl/external-entry").execute[ExternalEntryUrlResponse]

  def getSubmittedVatReturns()(implicit hc: HeaderCarrier): Future[VatReturnMultipleResponse] = {
    httpClientV2.get(url"$baseUrl/vat-returns").execute[VatReturnMultipleResponse]
  }

  def getObligations(vrn: Vrn)(implicit hc: HeaderCarrier): Future[EtmpObligations] =
    httpClientV2.get(url"$baseUrl/obligations/$vrn").execute[EtmpObligations]

  def getEtmpVatReturn(period: Period)(implicit hc: HeaderCarrier): Future[EtmpVatReturnResponse] =
    httpClientV2.get(url"$baseUrl/etmp-vat-returns/period/$period").execute[EtmpVatReturnResponse]
}

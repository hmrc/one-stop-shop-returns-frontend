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
import connectors.SaveForLaterHttpParser.{DeleteSaveForLaterReads, DeleteSaveForLaterResponse, SaveForLaterReads, SaveForLaterResponse}
import models.Period
import models.requests.SaveForLaterRequest
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def submit(s4lRequest: SaveForLaterRequest)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] =
    httpClientV2.post(url"$baseUrl/save-for-later").withBody(Json.toJson(s4lRequest)).execute[SaveForLaterResponse]

  def get()(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] =
    httpClientV2.get(url"$baseUrl/save-for-later").execute[SaveForLaterResponse]

  def delete(period: Period)(implicit hc: HeaderCarrier): Future[DeleteSaveForLaterResponse] =
    httpClientV2.get(url"$baseUrl/save-for-later/delete/$period").execute[DeleteSaveForLaterResponse]
}

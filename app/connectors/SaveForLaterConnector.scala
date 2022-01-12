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
import connectors.SaveForLaterHttpParser.{SaveForLaterReads, SaveForLaterResponse}
import models.requests.SaveForLaterRequest
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                     (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def submit(s4lRequest: SaveForLaterRequest)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    val url = s"$baseUrl/save-for-later"

    httpClient.POST[SaveForLaterRequest, SaveForLaterResponse](url, s4lRequest)
  }
}

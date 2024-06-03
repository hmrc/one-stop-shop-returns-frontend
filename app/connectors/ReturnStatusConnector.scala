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
import connectors.CurrentReturnsHttpParser._
import connectors.ReturnStatusesHttpParser._
import formats.Format
import play.api.Configuration
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnStatusConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def listStatuses(commencementDate: LocalDate)(implicit hc: HeaderCarrier): Future[ReturnStatusesResponse] =
    httpClientV2.get(url"$baseUrl/vat-returns/statuses/${Format.dateTimeFormatter.format(commencementDate)}").execute[ReturnStatusesResponse]

  def getCurrentReturns(vrn: Vrn)(implicit hc: HeaderCarrier): Future[CurrentReturnsResponse] =
    httpClientV2.get(url"$baseUrl/vat-returns/current-returns/${vrn.vrn}").execute[CurrentReturnsResponse]
}

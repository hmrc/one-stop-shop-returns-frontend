/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.corrections

import config.Service
import connectors.corrections.CorrectionHttpParser._
import models.Period
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                   (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def get(period: Period)(implicit hc: HeaderCarrier): Future[CorrectionResponse] = {
    val url = s"$baseUrl/corrections/${period.toString}"

    httpClient.GET[CorrectionResponse](url)
  }

  def getForCorrectionPeriod(period: Period)(implicit hc: HeaderCarrier): Future[CorrectionsForPeriodResponse] = {
    val url = s"$baseUrl/corrections-for-period/${period.toString}"

    httpClient.GET[CorrectionsForPeriodResponse](url)
  }
}

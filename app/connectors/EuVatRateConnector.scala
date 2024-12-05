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

import config.{FrontendAppConfig, Service}
import models.{Country, EuVatRate}
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.HeaderNames.AUTHORIZATION

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuVatRateConnector @Inject()(
                                    config: Configuration,
                                    appConfig: FrontendAppConfig,
                                    httpClientV2: HttpClientV2,
                                  )(implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.eu-vat-rates")

  def getEuVatRates(country: Country, fromDate: LocalDate, toDate: LocalDate)(implicit hc: HeaderCarrier): Future[Seq[EuVatRate]] = {
    httpClientV2.get(url"$baseUrl/vat-rate/${country.code}?startDate=$fromDate&endDate=$toDate")
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[Seq[EuVatRate]]
  }

}

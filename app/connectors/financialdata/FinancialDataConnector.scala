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

package connectors.financialdata

import config.Service
import connectors.financialdata.ChargeHttpParser._
import connectors.financialdata.FinancialDataHttpParser._
import models.Period
import play.api.Configuration
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                      (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def getCharge(period: Period)(implicit hc: HeaderCarrier): Future[ChargeResponse] =
    httpClientV2.get(url"$baseUrl/financial-data/charge/$period").execute[ChargeResponse]

  def getPeriodsAndOutstandingAmounts()(implicit hc: HeaderCarrier): Future[OutstandingPaymentsResponse] =
    httpClientV2.get(url"$baseUrl/financial-data/outstanding-payments").execute[OutstandingPaymentsResponse]

  def getFinancialData(vrn: Vrn)(implicit hc: HeaderCarrier): Future[FinancialDataResponse] =
    httpClientV2.get(url"$baseUrl/financial-data/prepare/${vrn.vrn}").execute[FinancialDataResponse]
}

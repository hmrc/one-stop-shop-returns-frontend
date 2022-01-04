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

package connectors.financialdata

import config.Service
import connectors.financialdata.FinancialDataHttpParser._
import connectors.financialdata.VatReturnWithFinancialDataHttpParser._
import formats.Format
import models.Period
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                      (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.one-stop-shop-returns")

  def getCharge(period: Period)(implicit hc: HeaderCarrier): Future[ChargeResponse] = {
    val url = s"$baseUrl/financial-data/charge/$period"
    httpClient.GET[ChargeResponse](url)
  }

  def getPeriodsAndOutstandingAmounts()(implicit hc: HeaderCarrier): Future[OutstandingPaymentsResponse] = {
    val url = s"$baseUrl/financial-data/outstanding-payments"
    httpClient.GET[OutstandingPaymentsResponse](url)
  }

  def getVatReturnWithFinancialData(commencementDate: LocalDate)(implicit hc: HeaderCarrier): Future[VatReturnWithFinancialDataResponse] = {
    val url = s"$baseUrl/financial-data/charge-history/${Format.dateTimeFormatter.format(commencementDate)}"
    httpClient.GET[VatReturnWithFinancialDataResponse](url)
  }
}

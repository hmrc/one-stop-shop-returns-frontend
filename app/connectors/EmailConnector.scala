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

import com.google.inject.Inject
import config.Service
import connectors.EmailHttpParser._
import models.emails.EmailSendingResult.EMAIL_NOT_SENT
import models.emails.{EmailSendingResult, EmailToSendRequest}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HeaderCarrier, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject()(
                                config: Configuration,
                                httpClientV2: HttpClientV2
                              ) {

  private val baseUrl = config.get[Service]("microservice.services.email")

  def send(email: EmailToSendRequest)
          (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[EmailSendingResult] = {
    httpClientV2.post(url"${baseUrl}hmrc/email").withBody(Json.toJson(email)).execute[EmailSendingResult].recover {
      case _: BadGatewayException => EMAIL_NOT_SENT
      case _: GatewayTimeoutException => EMAIL_NOT_SENT
    }
  }

}

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

import logging.Logging
import models.financialdata.CurrentPayments
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CurrentPaymentsHttpParser extends Logging {

  type CurrentPaymentsResponse = Either[ErrorResponse, CurrentPayments]

  implicit object CurrentPaymentsReads extends HttpReads[CurrentPaymentsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CurrentPaymentsResponse = {
      response.status match {
        case OK =>
          response.json.validate[CurrentPayments] match {
            case JsSuccess(currentPayments, _) => Right(currentPayments)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case _ =>
          logger.warn("Failed to retrieve current payments data")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }
}

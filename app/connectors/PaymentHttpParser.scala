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

package connectors

import logging.Logging
import models.requests.PaymentResponse
import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.CREATED
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object PaymentHttpParser extends Logging {

  type ReturnPaymentResponse = Either[ErrorResponse, PaymentResponse]

  implicit object ReturnPaymentReads extends HttpReads[ReturnPaymentResponse] {

    override def read(method: String, url: String, response: HttpResponse): ReturnPaymentResponse = {
      response.status match {
        case CREATED =>
          response.json.validate[PaymentResponse] match {
            case JsSuccess(r, _) => Right(r)
            case JsError(errors) =>
              logger.warn(s"PaymentResponse failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case status =>
          logger.warn("PaymentResponse received unexpected error")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }
}

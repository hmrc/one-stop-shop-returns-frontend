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

import models.domain.VatReturn
import models.responses._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import logging.Logging
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object VatReturnHttpParser extends Logging {

  type SubmittedVatReturnResponse = Either[ErrorResponse, Unit]

  implicit object SubmittedVatReturnReads extends HttpReads[SubmittedVatReturnResponse] {
    override def read(method: String, url: String, response: HttpResponse): SubmittedVatReturnResponse = {
      response.status match {
        case CREATED  => Right(())
        case CONFLICT => Left(ConflictFound)
        case status   => Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  type VatReturnResponse = Either[ErrorResponse, VatReturn]

  implicit object VatReturnReads extends HttpReads[VatReturnResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatReturnResponse = {
      response.status match {
        case OK =>
          response.json.validate[VatReturn] match {
            case JsSuccess(model, _) => Right(model) // TODO remove head
            case JsError(errors) =>
              logger.warn("Failed trying to parse JSON", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn("Received not found from vat return")
          Left(NotFound)
        case status   => Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }
}

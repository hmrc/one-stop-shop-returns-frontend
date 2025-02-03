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

import logging.Logging
import models.domain.VatReturn
import models.etmp.EtmpVatReturn
import models.responses._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object VatReturnHttpParser extends Logging {

  type VatReturnResponse = Either[ErrorResponse, VatReturn]
  type VatReturnMultipleResponse = Seq[VatReturn]
  type EtmpVatReturnResponse = Either[ErrorResponse, EtmpVatReturn]

  implicit object VatReturnReads extends HttpReads[VatReturnResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatReturnResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[VatReturn] match {
            case JsSuccess(vatReturn, _) => Right(vatReturn)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}")
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          if(response.body.contains(CoreErrorResponse.REGISTRATION_NOT_FOUND)){
            logger.warn("Received Registration NotFound from core")
            Left(RegistrationNotFound)
          } else {
            logger.warn("Received NotFound for vat return")
            Left(NotFound)
          }
        case SERVICE_UNAVAILABLE if(response.json.validate[CoreErrorResponse].isSuccess) =>
          logger.warn("Received error when submitting to core")
          Left(ReceivedErrorFromCore)
        case CONFLICT =>
          logger.warn("Received Conflict found for vat return")
          Left(ConflictFound)
        case status   =>
          logger.warn("Received unexpected error from vat return")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  implicit object VatReturnMultipleReads extends HttpReads[VatReturnMultipleResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatReturnMultipleResponse = {
      response.status match {
        case OK =>
          response.json.validate[Seq[VatReturn]] match {
            case JsSuccess(vatReturns, _) => vatReturns
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. JSON was ${response.json}")
              Seq.empty
          }

        case NOT_FOUND =>
          logger.warn(s"Received NotFound from vat returns")
          Seq.empty

        case status =>
          val message: String = s"Received unexpected error from vat returns with status: $status"
          logger.warn(message)
          throw new Exception(message)
      }
    }
  }

  implicit object EtmpVatReturnReads extends HttpReads[EtmpVatReturnResponse] {

    override def read(method: String, url: String, response: HttpResponse): EtmpVatReturnResponse = {
      response.status match {
        case OK =>
          response.json.validate[EtmpVatReturn] match {
            case JsSuccess(etmpVatReturn, _) => Right(etmpVatReturn)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse EtmpVatReturn JSON $errors. Json was ${response.json}")
              Left(InvalidJson)
          }

        case _ =>
          logger.error(s"Failed to retrieve EtmpVatReturn from server")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }
}
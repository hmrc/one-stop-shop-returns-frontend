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

import connectors.VatReturnHttpParser.logger
import logging.Logging
import models.corrections.CorrectionPayload
import models.domain.VatReturn
import models.responses.*
import play.api.http.Status.*
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.{Failure, Success, Try}

object VatReturnWithCorrectionHttpParser extends Logging {

  type VatReturnWithCorrectionResponse = Either[ErrorResponse, (VatReturn, CorrectionPayload)]
  private val REGISTRATION_NOT_FOUND = "OSS_009"
  implicit object VatReturnWithCorrectionReads extends HttpReads[VatReturnWithCorrectionResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatReturnWithCorrectionResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[(VatReturn, CorrectionPayload)] match {
            case JsSuccess(vatReturnWithCorrection, _) => Right(vatReturnWithCorrection)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          if(response.body.contains(REGISTRATION_NOT_FOUND)){
            logger.warn("Received Registration NotFound from core")
            Left(RegistrationNotFound)
          } else {
            logger.warn("Received NotFound from vat return")
            Left(NotFound)
          }
        case CONFLICT =>
          logger.warn("Received Conflict from vat return")
          Left(ConflictFound)
        case status =>
          Try(response.json.validate[CoreErrorResponse] match {
            case JsSuccess(value, path) =>
              logger.warn("Received error when submitting to core")
              Left(ReceivedErrorFromCore)
            case JsError(errors) =>
              logger.warn(s"Error parsing error ${response.body}")
              Left(UnexpectedResponseStatus(response.status, s"Unexpected response, unable to parse json ${response.json}"))
          }) match {
            case Success(value) => value
            case Failure(exception) =>
              logger.warn(s"Error with json ${response.body}")
              Left(UnexpectedResponseStatus(response.status, s"Unexpected response, unable to read json ${response.body}"))
          }
      }
    }
  }
}

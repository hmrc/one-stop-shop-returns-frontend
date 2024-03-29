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

package connectors.corrections

import logging.Logging
import models.corrections.CorrectionPayload
import models.responses._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CorrectionHttpParser extends Logging {

  type CorrectionResponse = Either[ErrorResponse, CorrectionPayload]
  type CorrectionsForPeriodResponse = Either[ErrorResponse, Seq[CorrectionPayload]]

  implicit object CorrectionReads extends HttpReads[CorrectionResponse] {
    override def read(method: String, url: String, response: HttpResponse): CorrectionResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[CorrectionPayload] match {
            case JsSuccess(correctionPayload, _) => Right(correctionPayload)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn("Received NotFound from correction")
          Left(NotFound)
        case CONFLICT =>
          logger.warn("Received NotFound from correction")
          Left(ConflictFound)
        case status =>
          logger.warn("Received unexpected error from correction")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  implicit object CorrectionsForPeriod extends HttpReads[CorrectionsForPeriodResponse] {
    override def read(method: String, url: String, response: HttpResponse): CorrectionsForPeriodResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[Seq[CorrectionPayload]] match {
            case JsSuccess(correctionPayload, _) => Right(correctionPayload)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          Right(Seq.empty)
        case CONFLICT =>
          logger.warn("Received NotFound from correction")
          Left(ConflictFound)
        case status =>
          logger.warn("Received unexpected error from correction")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

}

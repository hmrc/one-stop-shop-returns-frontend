/*
 * Copyright 2023 HM Revenue & Customs
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
import models.PeriodWithStatus
import models.responses._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ReturnStatusesHttpParser extends Logging {

  type ReturnStatusesResponse = Either[ErrorResponse, Seq[PeriodWithStatus]]

  implicit object ReturnStatusesReads extends HttpReads[ReturnStatusesResponse] {
    override def read(method: String, url: String, response: HttpResponse): ReturnStatusesResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[Seq[PeriodWithStatus]] match {
            case JsSuccess(periodWithStatuses, _) => Right(periodWithStatuses)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case status   =>
          logger.warn("Received unexpected error from return statuses")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }
}

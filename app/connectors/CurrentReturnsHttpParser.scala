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

package connectors

import logging.Logging
import models.responses._
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import viewmodels.yourAccount.OpenReturns

object CurrentReturnsHttpParser extends Logging {

  type CurrentReturnsResponse = Either[ErrorResponse, OpenReturns]

  implicit object CurrentReturnsReads extends HttpReads[CurrentReturnsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CurrentReturnsResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[OpenReturns] match {
            case JsSuccess(returnsModel, _) => Right(returnsModel)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case status   =>
          logger.warn("Received unexpected error from current returns")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }
}

/*
 * Copyright 2026 HM Revenue & Customs
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
import models.responses.*
import play.api.http.Status.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CsvHttpParser extends Logging {
  
  type FileUploadCsvResponse = Either[ErrorResponse, String]
  
  implicit object FileUploadCsvReads extends HttpReads[FileUploadCsvResponse] {

    override def read(method: String, url: String, response: HttpResponse): FileUploadCsvResponse = {
      response.status match {
        case OK =>
          Right(response.body)

        case NOT_FOUND =>
          Left(NotFound)

        case CONFLICT =>
          Left(ConflictFound)

        case BAD_REQUEST =>
          Left(UnexpectedResponseStatus(response.status, response.body))

        case INTERNAL_SERVER_ERROR =>
          Left(InternalServerError)

        case status =>
          logger.error(s"FileUploadCsvResponse recieved unexpected statu = $status, body = ${response.body}")
          Left(UnexpectedResponseStatus(status, response.body))
      }
    }
    
  }

}

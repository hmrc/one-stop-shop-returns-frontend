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
import models.emails.EmailSendingResult
import models.emails.EmailSendingResult.{EMAIL_ACCEPTED, EMAIL_NOT_SENT, EMAIL_UNSENDABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EmailHttpParser extends Logging {

  implicit object EmailResponseReads extends HttpReads[EmailSendingResult] {

    override def read(method: String, url: String, response: HttpResponse): EmailSendingResult = {
      response match {
        case r if r.status >= 200 && r.status < 300 =>
          EMAIL_ACCEPTED
        case r if r.status >= 400 && r.status < 500 =>
          EMAIL_UNSENDABLE
        case r if r.status >= 500 && r.status < 600 =>
          EMAIL_NOT_SENT
        case r =>
          EMAIL_ACCEPTED
      }
    }
  }
}

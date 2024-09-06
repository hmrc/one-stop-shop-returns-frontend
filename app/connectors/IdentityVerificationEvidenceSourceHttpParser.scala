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
import models.iv.{IdentityVerificationEvidenceSource, UnrecognisedSource}
import play.mvc.Http.Status.OK
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object IdentityVerificationEvidenceSourceHttpParser extends Logging {

  implicit object IdentityVerificationEvidenceSourcesReads extends HttpReads[List[IdentityVerificationEvidenceSource]] {

    override def read(method: String, url: String, response: HttpResponse): List[IdentityVerificationEvidenceSource] = {
      if (response.status == OK) {
        (response.json \ "disabled-evidences")
          .as[List[IdentityVerificationEvidenceSource]]
          .filterNot(x => x.isInstanceOf[UnrecognisedSource])
      } else {
        logger.error(s"Received HTTP status ${response.status} trying to get evidence sources that are off.")
        List.empty
      }
    }
  }
}

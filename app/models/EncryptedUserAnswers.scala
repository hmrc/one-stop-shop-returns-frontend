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

package models

import play.api.libs.json.*
import queries.{Derivable, Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

case class EncryptedUserAnswers(
                              userId: String,
                              period: Period,
                              data: String,
                              lastUpdated: Instant = Instant.now
                            )

object EncryptedUserAnswers {

  val reads: Reads[EncryptedUserAnswers] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "userId").read[String] and
      (__ \ "period").read[Period] and
      (__ \ "data").read[String] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (EncryptedUserAnswers.apply _)
  }

  val writes: OWrites[EncryptedUserAnswers] = {

    import play.api.libs.functional.syntax.*

    (
      (__ \ "userId").write[String] and
      (__ \ "period").write[Period] and
      (__ \ "data").write[String] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (userAnswers => Tuple.fromProductTyped(userAnswers))
  }

  implicit val format: OFormat[EncryptedUserAnswers] = OFormat(reads, writes)
}

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

package repositories

import models.registration.Registration
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class RegistrationWrapper(
                                      userId: String,
                                      registration: Registration,
                                      lastUpdated: Instant
                                    )

object RegistrationWrapper {

  val reads: Reads[RegistrationWrapper] =
    (
      (__ \ "_id").read[String] and
      (__ \ "registration").read[Registration] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (RegistrationWrapper.apply _)

  val writes: OWrites[RegistrationWrapper] =
    (
      (__ \ "_id").write[String] and
      (__ \ "registration").write[Registration] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (unlift(RegistrationWrapper.unapply))

  implicit val format: OFormat[RegistrationWrapper] = OFormat(reads, writes)
}

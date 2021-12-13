/*
 * Copyright 2021 HM Revenue & Customs
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

import models.domain.VatReturn
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, OWrites, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class CachedVatReturnWrapper(
                                      userId: String,
                                      vatReturn: VatReturn,
                                      lastUpdated: Instant
                                    )

object CachedVatReturnWrapper {

  val reads: Reads[CachedVatReturnWrapper] =
    (
      (__ \ "_id").read[String] and
      (__ \ "vatReturn").read[VatReturn] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    ) (CachedVatReturnWrapper.apply _)

  val writes: OWrites[CachedVatReturnWrapper] =
    (
      (__ \ "_id").write[String] and
      (__ \ "vatReturn").write[VatReturn] and
      (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    ) (unlift(CachedVatReturnWrapper.unapply))

  implicit val format: OFormat[CachedVatReturnWrapper] = OFormat(reads, writes)
}



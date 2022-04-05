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

package models

import play.api.libs.json._
import queries.{Derivable, Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class SessionData(
                              userId: String,
                              data: JsObject = Json.obj(),
                              lastUpdated: Instant = Instant.now
                            ) {

  def get[A](key: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(key)).reads(data).getOrElse(None)

  def set[A](key: JsPath, value: A)(implicit writes: Writes[A]): Try[SessionData] = {

    val updatedData = data.setObject(key, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors) =>
        Failure(JsResultException(errors))
    }

    updatedData.map {
      d => copy (data = d)
    }
  }

  def remove[A](key: JsPath): Try[SessionData] = {

    val updatedData = data.removeObject(key) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_) =>
        Success(data)
    }

    updatedData.map {
      d => copy (data = d)
    }
  }
}

object SessionData {

  val reads: Reads[SessionData] = {

    import play.api.libs.functional.syntax._

    (
        (__ \ "userId").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      ) (SessionData.apply _)
  }

  val writes: OWrites[SessionData] = {

    import play.api.libs.functional.syntax._

    (
        (__ \ "userId").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      ) (unlift(SessionData.unapply))
  }

  implicit val format: OFormat[SessionData] = OFormat(reads, writes)
}


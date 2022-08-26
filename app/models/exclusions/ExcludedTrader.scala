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

package models.exclusions

import com.typesafe.config.Config
import play.api.{ConfigLoader, Configuration}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.format.DateTimeFormatter

case class ExcludedTrader(
                           vrn: Vrn,
                           exclusionSource: String,
                           exclusionReason: Int,
                           effectiveDate: String
                         )

object ExcludedTrader {

  implicit val format: OFormat[ExcludedTrader] = Json.format[ExcludedTrader]

  implicit lazy val configLoader: ConfigLoader[ExcludedTrader] = ConfigLoader {
    config =>
      prefix =>

        val excludedTrader = Configuration(config).get[Configuration](prefix)
        val vrn = excludedTrader.get[String]("vrn")
        val exclusionSource = excludedTrader.get[String]("exclusionSource")
        val exclusionReason = excludedTrader.get[Int]("exclusionReason")
        val effectiveDate = excludedTrader.get[String]("effectiveDate")

        ExcludedTrader(Vrn(vrn), exclusionSource, exclusionReason, effectiveDate)
  }

  implicit val seqExcludedTrader: ConfigLoader[Seq[ExcludedTrader]] = new ConfigLoader[Seq[ExcludedTrader]] {
    override def load(rootConfig: Config, path: String): Seq[ExcludedTrader] = {
      import scala.collection.JavaConverters._

      val config = rootConfig.getConfig(path)

      rootConfig.getObject(path).keySet().asScala.map { key =>
        val value = config.getConfig(key)
        ExcludedTrader(
          Vrn(value.getString("vrn")),
          value.getString("exclusionSource"),
          value.getInt("exclusionReason"),
          value.getString("effectiveDate")
        )
      }.toSeq
    }
  }

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
}
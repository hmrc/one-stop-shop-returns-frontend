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
import logging.Logging
import models.Period
import play.api.{ConfigLoader, Configuration}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn


case class ExcludedTrader(
                           vrn: Vrn,
                           exclusionSource: String,
                           exclusionReason: Int,
                           effectivePeriod: Period
                         )

object ExcludedTrader extends Logging {

  implicit val format: OFormat[ExcludedTrader] = Json.format[ExcludedTrader]

  implicit lazy val configLoader: ConfigLoader[ExcludedTrader] = ConfigLoader {
    config =>
      prefix =>

        val excludedTrader = Configuration(config).get[Configuration](prefix)
        val vrn = excludedTrader.get[String]("vrn")
        val exclusionSource = excludedTrader.get[String]("exclusionSource")
        val exclusionReason = excludedTrader.get[Int]("exclusionReason")
        val effectivePeriod = excludedTrader.get[String]("effectivePeriod")

        Period.fromString(effectivePeriod) match {
          case Some(excludedPeriod) =>
            ExcludedTrader(Vrn(vrn), exclusionSource, exclusionReason, excludedPeriod)
          case _ =>
            logger.error("Unable to parse period")
            throw new Exception("Unable to parse period")
        }


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
          Period.fromString(value.getString("effectivePeriod")) match {
            case Some(excludedPeriod) =>
                excludedPeriod
            case _ =>
              logger.error("Unable to parse period")
              throw new Exception("Unable to parse period")
          }
        )
      }.toSeq
    }
  }

}

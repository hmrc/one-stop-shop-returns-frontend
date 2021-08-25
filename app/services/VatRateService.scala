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

package services

import models.{Country, Period, VatRate}
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

import javax.inject.Inject
import scala.io.Source

class VatRateService @Inject()(env: Environment, config: Configuration) {

  private val vatRateFile = config.get[String]("vat-rates-file")

  private val vatRates: Map[Country, Seq[VatRate]] = {

    val json = env.resourceAsStream(vatRateFile)
      .fold(throw new Exception("Could not open VAT Rate file"))(Source.fromInputStream).mkString

    val parsedRates = Json.parse(json).as[Map[String, Seq[VatRate]]]

    parsedRates.map {
      case(countryCode, rates) =>
        val country =
          Country.euCountriesWithNI
            .find(_.code == countryCode)
            .getOrElse(throw new Exception(s"VAT rates file contained entry $countryCode that is not recognised"))

        country -> rates
    }
  }

  def vatRates(period: Period, country: Country): Seq[VatRate] =
    vatRates
      .getOrElse(country, Seq.empty)
      .filter(_.validFrom isBefore period.lastDay.plusDays(1))
      .filter(rate => rate.validUntil.fold(true)(_.isAfter(period.firstDay.minusDays(1))))
}

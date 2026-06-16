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

package services.fileUpload

import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import models.VatRateType.Standard
import models.{Country, Index, UserAnswers, VatOnSales, VatOnSalesChoice, VatRate, VatRateType}
import pages.*

import java.io.*
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

object CsvParserService {

  def split(content: String): Seq[Array[String]] = {
    val settings = new CsvParserSettings()
    settings.setNullValue("")
    settings.setEmptyValue("")
    settings.setSkipEmptyLines(true)
    settings.setIgnoreLeadingWhitespaces(true)
    settings.setIgnoreTrailingWhitespaces(true)

    val parser = new CsvParser(settings)

    val rows:Seq[Array[String]] =
      parser.parseAll(new StringReader(content)).asScala.toSeq

    removeNonPrintableChars(rows)
  }

  private def removeNonPrintableChars(csvContent: Seq[Array[String]]): Seq[Array[String]] = {
    csvContent.map(_.map(_.replaceAll("\\p{C}", "")))
  }
}

@Singleton

class CsvParserService @Inject()() {

  private final case class VatRow(countryFrom: String, countryTo: String, vatRate: String, salesToCountry: BigDecimal, vatOnSales: BigDecimal)

  def populateUserAnswersFromCsv(userAnswers: UserAnswers, csvContent: String): Try[UserAnswers] = {
    val rawRows: Seq[Array[String]] = CsvParserService.split(csvContent)
    val parsedRows: Seq[VatRow] = extractVatRows(rawRows)

    val niRows = parsedRows.filter(_.countryFrom == "Northern Ireland")
    val euRows = parsedRows.filterNot(_.countryFrom == "Northern Ireland")

    for {
      ua1 <- userAnswers.set(SoldGoodsFromNiPage, niRows.nonEmpty)
      ua2 <- ua1.set(SoldGoodsFromEuPage, euRows.nonEmpty)
      ua3 <- populateNiSales(ua2, niRows)
      ua4 <- populateEuSales(ua3, euRows)
    } yield ua4
  }

  private def populateNiSales(userAnswers: UserAnswers, rows: Seq[VatRow]): Try[UserAnswers] = {
    val groupedByCountry: Seq[(String, Seq[VatRow])] = rows.groupBy(_.countryTo).toSeq.sortBy(_._1)

    groupedByCountry.zipWithIndex.foldLeft(Try(userAnswers)) {
      case (accTry, ((countryTo, vatRows), countryIndexValue)) =>
        val countryIndex = Index(countryIndexValue)

        accTry.flatMap { ua =>
          val rates: List[VatRate] = vatRows.map(r => vatRateFrom(r.vatRate)).toList

          val withCountryAndRates = ua.set(CountryOfConsumptionFromNiPage(countryIndex), countryToName(countryTo))
            .flatMap(_.set(VatRatesFromNiPage(countryIndex), rates))

          vatRows.zipWithIndex.foldLeft(withCountryAndRates) {
            case (uaTry, (row, vatIndex)) =>
              val vatRateIndex = Index(vatIndex)

              uaTry
                .flatMap(_.set(NetValueOfSalesFromNiPage(countryIndex, vatRateIndex), row.salesToCountry))
                .flatMap(_.set(VatOnSalesFromNiPage(countryIndex, vatRateIndex), vatOnSalesFrom(row.vatOnSales)))
          }
        }
    }
  }

  private def populateEuSales(userAnswers: UserAnswers, rows: Seq[VatRow]): Try[UserAnswers] = {
    val groupedByCountry: Seq[(String, Seq[VatRow])] = rows.groupBy(_.countryFrom).toSeq.sortBy(_._1)

    groupedByCountry.zipWithIndex.foldLeft(Try(userAnswers)) {
      case (acc, ((countryFrom, fromRows), countryFromIndexValue)) =>
        val countryFromIndex = Index(countryFromIndexValue)

        acc.flatMap { ua =>
          val withCountryFrom = ua.set(CountryOfSaleFromEuPage(countryFromIndex), countryFromName(countryFrom))
          val groupedByCountryTo: Seq[(String, Seq[VatRow])] = fromRows.groupBy(_.countryTo).toSeq.sortBy(_._1)

          groupedByCountryTo.zipWithIndex.foldLeft(withCountryFrom) {
            case (uaTry, ((countryTo, toRows), toCountryIndex)) =>
              val countryToIndex = Index(toCountryIndex)
              val rates = toRows.map(r => vatRateFrom(r.vatRate)).toList

              val withCountryAndRates = uaTry
                .flatMap(_.set(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex), countryToName(countryTo)))
                .flatMap(_.set(VatRatesFromEuPage(countryFromIndex, countryToIndex), rates))

              toRows.zipWithIndex.foldLeft(withCountryAndRates) {
                case (innerTry, (row, vatIndexValue)) =>
                  val vatIndex = Index(vatIndexValue)

                  innerTry
                    .flatMap(_.set(NetValueOfSalesFromEuPage(countryFromIndex, countryToIndex, vatIndex), row.salesToCountry))
                    .flatMap(_.set(VatOnSalesFromEuPage(countryFromIndex, countryToIndex, vatIndex), vatOnSalesFrom(row.vatOnSales)))
              }
          }
        }
    }
  }


  private def extractVatRows(rows: Seq[Array[String]]): Seq[VatRow] = {

    val headerIndex = {
      rows.indexWhere(_.headOption.exists { c =>
        val cleaned = c.replace("\"", "").trim.toLowerCase
        cleaned.startsWith("country your goods sold from")
      })
    }

    if (headerIndex < 0) {
      Seq.empty
    } else {
      rows
        .drop(headerIndex + 1)
        .filterNot(isRowEmpty)
        .collect {
          case Array(countryFrom, countryTo, vatRate, sales, vat) =>
            VatRow(
              countryFrom = aliasCountry(countryFrom),
              countryTo = aliasCountry(countryTo),
              vatRate = parseVatRate(vatRate),
              salesToCountry = parseValue(sales),
              vatOnSales = parseValue(vat)
            )
        }
    }
  }

  private def isRowEmpty(row: Array[String]): Boolean = {
    row.forall(_.trim.isEmpty)
  }

  private def parseVatRate(vatRateFromCsv: String): String = {
    vatRateFromCsv.replace("%", "").trim
  }

  private def parseValue(valueFromCsv: String): BigDecimal = {
    val cleaned = valueFromCsv
      .replace("Â", "")
      .replace("£", "")
      .replace(",", "")
      .trim

    BigDecimal(cleaned)
  }

  private def countryFromName(countryName: String): Country = {
    Country.euCountriesWithNI.find(_.name == countryName).getOrElse(throw new IllegalArgumentException(s" Unknown country: '$countryName''"))
  }

  private def countryToName(countryName: String): Country = {
    Country.euCountriesWithNI.find(_.name == countryName).getOrElse(throw new IllegalArgumentException(s" Unknown country: '$countryName''"))
  }

  private def vatRateFrom(vatRate: String): VatRate = {
    VatRate(
      rate = BigDecimal(vatRate),
      rateType = Standard,
      validFrom = LocalDate.parse("2021-01-01")
    )
  }

  private def vatOnSalesFrom(amount: BigDecimal): VatOnSales = {
    VatOnSales(
      choice = VatOnSalesChoice.Standard,
      amount = amount
    )
  }

  private def aliasCountry(raw: String): String = {
    val cleaned = raw.replace("\"", "").trim
    if (cleaned.equalsIgnoreCase("Czechia")) {
      "Czech Republic"
    } else {
      cleaned
    }
  }
}

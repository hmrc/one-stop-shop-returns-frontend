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

import models.{Country, Period}
import models.upscan.CsvColumn.*
import models.upscan.CsvError.{DuplicateVatRate, InvalidNumberFormat}
import models.upscan.{CsvColumn, CsvError, CsvValidationException}
import services.VatRateService
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CsvValidator @Inject()(vatRateService: VatRateService)(implicit ec: ExecutionContext) {

  private val VatRateRegex = """^\s*-?\d+(?:\.\d{1,2})?\s*%?\s*$""".r
  private val CountryAllowedCharsRegex = """^[\p{L}\p{M}\s'.-]+$""".r
  private val MaxColumns = 5

  def validateOrThrow(rows: Seq[Array[String]], period: Period, isOnlineMarketPlace: Boolean)(implicit hs: HeaderCarrier): Future[Unit] = {
    validate(rows, period, isOnlineMarketPlace).map { errors =>
      if (errors.nonEmpty) throw CsvValidationException(errors)
    }
  }
  
  private def validate(rows: Seq[Array[String]], period: Period, isOnlineMarketPlace: Boolean)(implicit hc: HeaderCarrier): Future[Seq[CsvError]] = {
    
    val headerIndex = rows.indexWhere { row =>
      row.headOption.exists { c =>
        val cleaned = c.replace("\"", "").trim
        
        cleaned.toLowerCase.startsWith("country your goods sold from")
      }
    }
    
    if (headerIndex < 0) {
      Seq(CsvError.InvalidCountry(row = 1, column = A, value = "Missing header 'Country From'")).toFuture
    } else {
      val dataRows = rows.drop(headerIndex + 1).filterNot(isRowEmpty)
      
      val basicErrors: Seq[CsvError] = dataRows.zipWithIndex.flatMap {
        case (row, idx0) =>
          val csvRowNumber = (headerIndex + 2) + idx0
          validateRow(csvRowNumber, row, isOnlineMarketPlace)
      }
      
      val duplicateErrors: Seq[CsvError] = validateDuplicateCountryVatRate(dataRows, headerIndex)
      
      validateVatRatesAllowed(dataRows, headerIndex, period).map { vatRateErrors =>
        basicErrors ++ duplicateErrors ++ vatRateErrors
      }
    }
  }
  
  private def validateRow(csvRowNumber: Int, row: Array[String], isOnlineMarketPlace: Boolean): Seq[CsvError] = {
    
    val countryFromRaw = cell(row, 0)
    val countryToRaw   = cell(row, 1)
    val vatRateRaw     = cell(row, 2)
    val salesRaw       = cell(row, 3)
    val vatRaw         = cell(row, 4)
    
    validateColumnCount(csvRowNumber, row) ++
      validateCountryFrom(csvRowNumber, countryFromRaw) ++
      validateCountryTo(csvRowNumber, countryToRaw) ++
      validateCountryFromAndToAreDifferent(csvRowNumber, countryFromRaw, countryToRaw, isOnlineMarketPlace) ++
      validateVatRate(csvRowNumber, vatRateRaw) ++
      validateMoney(csvRowNumber, CsvColumn.D, salesRaw) ++
      validateMoney(csvRowNumber, CsvColumn.E, vatRaw)
  }
  
  private def validateCountryFrom(row: Int, countryRaw: String): Seq[CsvError] = {
    
    val aliasCountryName = aliasCountry(countryRaw)
    
    if (aliasCountryName.isEmpty) {
      Seq(CsvError.BlankCell(row, CsvColumn.A))
    } else if (!CountryAllowedCharsRegex.matches(aliasCountryName)) {
      Seq(CsvError.InvalidCharacter(row, CsvColumn.A, countryRaw))
    } else {
      val exists = Country.euCountriesWithNI.exists(_.name.equalsIgnoreCase(aliasCountryName))
      if (!exists) Seq(CsvError.InvalidCountry(row, CsvColumn.A, countryRaw)) else Nil
    }
  }

  private def validateCountryTo(row: Int, countryRaw: String): Seq[CsvError] = {

    val aliasCountryName = aliasCountry(countryRaw)

    if (aliasCountryName.isEmpty) {
      Seq(CsvError.BlankCell(row, CsvColumn.B))
    } else if (!CountryAllowedCharsRegex.matches(aliasCountryName)) {
      Seq(CsvError.InvalidCharacter(row, CsvColumn.B, countryRaw))
    } else {
      val exists = Country.euCountriesWithNI.exists(_.name.equalsIgnoreCase(aliasCountryName))
      if (!exists) Seq(CsvError.InvalidCountry(row, CsvColumn.B, countryRaw)) else Nil
    }
  }
  
  private def validateCountryFromAndToAreDifferent(row: Int, countryFromRaw: String, countryToRaw: String, isOnlineMarketPlace: Boolean): Seq[CsvError] = {
    
    val countryFrom = aliasCountry(countryFromRaw)
    val countryTo = aliasCountry(countryToRaw)
    
    if (!isOnlineMarketPlace && countryFrom.nonEmpty && countryFrom.equalsIgnoreCase(countryTo)) {
      Seq(CsvError.SameToAndFromCountry(row, CsvColumn.B, countryFrom, countryTo, countryToRaw))
    } else {
      Nil
    }
  }
  
  
  private def validateVatRate(row: Int, vatRateRaw: String): Seq[CsvError] = {
    if (vatRateRaw.isEmpty) {
      Seq(CsvError.BlankCell(row, CsvColumn.C))
    } else  if (!VatRateRegex.matches(vatRateRaw)) {
      Seq(CsvError.InvalidNumberFormat(row, CsvColumn.C, vatRateRaw))
    } else {
      val cleaned = vatRateRaw.replace("%", "").trim
      Try(BigDecimal(cleaned)).toEither match {
        case Left(_) =>
          Seq(InvalidNumberFormat(row, CsvColumn.C, vatRateRaw))
        case Right(rate) =>
          if (rate < 0) Seq(CsvError.NegativeNumber(row, CsvColumn.C, rate)) else Nil
      }
    }
  }
  
  private def validateMoney(row: Int, col: CsvColumn, raw: String): Seq[CsvError] = {
    
    if (raw.isEmpty) {
      Seq(CsvError.BlankCell(row, col))
    } else {
      val cleaned = raw
        .replace("Â", "")
        .replace("£", "")
        .replace(",", "")
        .trim
      
      Try(BigDecimal(cleaned)).toEither match {
        case Left(_) =>
          Seq(InvalidNumberFormat(row, col, raw))
        
        case Right(n) =>
          if (n < 0) {
            Seq(CsvError.NegativeNumber(row, col, n))
          } else {
            if (n.scale > 2) {
              Seq(CsvError.TooMayDecimals(row, col, n))
            } else {
              Nil
            }
          }
          
      }
    }
  }
  
  private def validateVatRatesAllowed(dataRows: Seq[Array[String]], headerIndex: Int, period: Period)(implicit hc: HeaderCarrier): Future[Seq[CsvError]] = {
    
    val rowsByCountry = {
      dataRows.zipWithIndex.flatMap {
        case (row, index0) =>
          val countryRaw = cell(row, 1)
          val aliasCountryName = aliasCountry(countryRaw)
          
          Country.euCountriesWithNI.find(_.name.equalsIgnoreCase(aliasCountryName)).map { country =>
            val csvRowNumber = (headerIndex + 2) + index0
            country -> (csvRowNumber -> row)
          }
      }.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    }
    
    Future.traverse(rowsByCountry.toSeq) {
      case (country, rows) =>
        vatRateService.vatRates(period, country).map { allowed =>
          val allowedRates = allowed.map(_.rate).toSet
          
          rows.flatMap {
            case (rowNum, row) =>
              val vatRateRaw = cell(row, 2)
              
              parseRate(vatRateRaw).toOption match {
                case Some(rate) if !allowedRates.contains(rate) =>
                  Seq(CsvError.VatRateNotAllowed(rowNum, CsvColumn.C, country.name, vatRateRaw))
                case _ =>
                  Nil
              }
          }
        }
    }.map(_.flatten)
  }
  
  private def validateDuplicateCountryVatRate(dataRows: Seq[Array[String]], headerIndex: Int): Seq[CsvError] = {
    
    type CountryWithRate = (String, String, BigDecimal)
    
    val (_, errors) = dataRows.zipWithIndex.foldLeft(Map.empty[CountryWithRate, Int] -> Vector.empty[CsvError]) {
      case ((seen, errs), (row, index0)) =>
        val csvRowNumber = (headerIndex +2) + index0
        val countryFromRaw = cell(row, 0)
        val countryToRaw = cell(row, 1)
        val aliasCountryFromName = aliasCountry(countryFromRaw)
        val aliasCountryToName = aliasCountry(countryToRaw)
        val vatRateRaw = cell(row, 2)
        val countryFromOpt = Country.euCountriesWithNI.find(_.name.equalsIgnoreCase(aliasCountryFromName)).map(_.name)
        val countryToOpt = Country.euCountriesWithNI.find(_.name.equalsIgnoreCase(aliasCountryToName)).map(_.name)
        val vatRateOpt = parseRate(vatRateRaw)
          .toOption
          .filter(_ >= 0)
          .map(_.bigDecimal.stripTrailingZeros())
          .map(BigDecimal(_))

        (countryFromOpt, countryToOpt, vatRateOpt) match {
          case (Some(countryFrom), Some(countryTo), Some(vatRate)) =>
            val countryRow: CountryWithRate = (countryFrom.toLowerCase, countryTo.toLowerCase, vatRate)
            
            if (seen.contains(countryRow)) {
              seen -> (errs :+ DuplicateVatRate(csvRowNumber, CsvColumn.C, countryFrom, countryTo, vatRateRaw))
            } else {
              seen.updated(countryRow, csvRowNumber) -> errs
            }

          case _ =>
            seen -> errs
        }
    }
    errors
  }
  
  private def validateColumnCount(rowNum: Int, row: Array[String]): Seq[CsvError] = {
    val hasExtraNonEmpty = row.drop(MaxColumns).exists(cell => cell.replace("\"", "").trim.nonEmpty)
    
    if (hasExtraNonEmpty) {
      Seq(CsvError.TooManyColumns(row = rowNum, column = D, actualColumns = row.length))
    } else {
      Nil
    }
  }
  
  private def aliasCountry(raw: String): String = {
    val cleaned = raw.replace("\"", "").trim
    
    if (cleaned.equalsIgnoreCase("Czechia")) {
      "Czech republic"
    } else {
      cleaned
    }
  }
  
  private def parseRate(raw: String): Either[Unit, BigDecimal] = {
    if (raw.trim.isEmpty) {
      Left(())
    } else if (!VatRateRegex.matches(raw.trim)) {
      Left(())
    } else {
      Try(BigDecimal(raw.replace("%", "").trim)).toEither match {
        case Right(rate) if rate >= 0 =>
          Right(rate)
        case _ =>
          Left(())
      }
    }
  }
  
  private def isRowEmpty(row: Array[String]): Boolean = {
    row.forall(_.trim.isEmpty)
  }
  
  private def cell(row: Array[String], i: Int): String = row.lift(i).map(_.replace("\"", "").trim).getOrElse("")
}

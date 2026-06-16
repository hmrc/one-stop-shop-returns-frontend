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

package models.upscan

import play.api.libs.json.*

sealed trait CsvColumn { def letter: String; def index: Int}

object CsvColumn {
  case object A extends CsvColumn { val letter = "A"; val index = 1 } // country from
  case object B extends CsvColumn { val letter = "B"; val index = 2 } // country to
  case object C extends CsvColumn { val letter = "C"; val index = 3 } // VAT rate
  case object D extends CsvColumn { val letter = "D"; val index = 4 } // Sales
  case object E extends CsvColumn { val letter = "E"; val index = 5 } // Total VAT due
}

sealed trait CsvError { def row: Int; def column: CsvColumn; def cellRef: String = s"${column.letter}$row"}

object CsvError {
  final case class  InvalidCountry(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class  InvalidCharacter(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class  InvalidNumberFormat(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class  NegativeNumber(row: Int, column: CsvColumn, value: BigDecimal) extends CsvError
  final case class  TooMayDecimals(row: Int, column: CsvColumn, value: BigDecimal) extends CsvError
  final case class  BlankCell(row: Int, column: CsvColumn) extends CsvError
  final case class  VatRateNotAllowed(row: Int, column: CsvColumn, country: String, value: String) extends CsvError
  final case class  DuplicateVatRate(row: Int, column: CsvColumn, countryFrom: String, countryTo: String, value: String) extends CsvError
  final case class  TooManyColumns(row: Int, column: CsvColumn, actualColumns: Int) extends CsvError
  final case class  SameToAndFromCountry(row: Int, column: CsvColumn, countryFrom: String, countryTo: String, value: String) extends CsvError
}

final case class CsvValidationException(errors: Seq[CsvError]) extends RuntimeException(s"CSV validation error at ${errors.map(_.cellRef).mkString(", ")}")

implicit val csvColumnFormat: Format[CsvColumn] = new Format[CsvColumn] {
  def writes(o: CsvColumn): JsValue = JsString(o.letter)
  def reads(json: JsValue): JsResult[CsvColumn] = json match {
    case JsString("A") => JsSuccess(CsvColumn.A)
    case JsString("B") => JsSuccess(CsvColumn.B)
    case JsString("C") => JsSuccess(CsvColumn.C)
    case JsString("D") => JsSuccess(CsvColumn.D)
    case JsString("E") => JsSuccess(CsvColumn.E)
    case _ => JsError("Unknown column")
  }
}

implicit val csvErrorFormat: Format[CsvError] = {
  val invalidCountry = Json.format[CsvError.InvalidCountry]
  val invalidCharacter = Json.format[CsvError.InvalidCharacter]
  val invalidNumberFormat = Json.format[CsvError.InvalidNumberFormat]
  val negativeNumber = Json.format[CsvError.NegativeNumber]
  val tooMayDecimals = Json.format[CsvError.TooMayDecimals]
  val blankCell = Json.format[CsvError.BlankCell]
  val vatRateNotAllowed = Json.format[CsvError.VatRateNotAllowed]
  val duplicateVatRate = Json.format[CsvError.DuplicateVatRate]
  val tooManyColumns = Json.format[CsvError.TooManyColumns]
  val sameToAndFromCountry = Json.format[CsvError.SameToAndFromCountry]
  
  new Format[CsvError] {
    
    def writes(o: CsvError): JsObject = o match {
      case e: CsvError.InvalidCountry       => invalidCountry.writes(e) + ("type" -> JsString("InvalidCountry"))
      case e: CsvError.InvalidCharacter     => invalidCharacter.writes(e) + ("type" -> JsString("InvalidCharacter"))
      case e: CsvError.InvalidNumberFormat  => invalidNumberFormat.writes(e) + ("type" -> JsString("InvalidNumberFormat"))
      case e: CsvError.NegativeNumber       => negativeNumber.writes(e) + ("type" -> JsString("NegativeNumber"))
      case e: CsvError.TooMayDecimals       => tooMayDecimals.writes(e) + ("type" -> JsString("TooMayDecimals"))
      case e: CsvError.BlankCell            => blankCell.writes(e) + ("type" -> JsString("BlankCell"))
      case e: CsvError.VatRateNotAllowed    => vatRateNotAllowed.writes(e) + ("type" -> JsString("VatRateNotAllowed"))
      case e: CsvError.DuplicateVatRate     => duplicateVatRate.writes(e) + ("type" -> JsString("DuplicateVatRate"))
      case e: CsvError.TooManyColumns       => tooManyColumns.writes(e) + ("type" -> JsString("TooManyColumns"))
      case e: CsvError.SameToAndFromCountry => sameToAndFromCountry.writes(e) + ("type" -> JsString("SameToAndFromCountry"))
    }
    
    def reads(json: JsValue): JsResult[CsvError] = (json \ "type").validate[String].flatMap {
      case "InvalidCountry"       => invalidCountry.reads(json)
      case "InvalidCharacter"     => invalidCharacter.reads(json)
      case "InvalidNumberFormat"  => invalidNumberFormat.reads(json)
      case "NegativeNumber"       => negativeNumber.reads(json)
      case "TooMayDecimals"       => tooMayDecimals.reads(json)
      case "BlankCell"            => blankCell.reads(json)
      case "VatRateNotAllowed"    => vatRateNotAllowed.reads(json)
      case "DuplicateVatRate"     => duplicateVatRate.reads(json)
      case "TooManyColumns"       => tooManyColumns.reads(json)
      case "SameToAndFromCountry" => sameToAndFromCountry.reads(json)
      case other                  => JsError(s"Unknown type $other")
    }
  }
}
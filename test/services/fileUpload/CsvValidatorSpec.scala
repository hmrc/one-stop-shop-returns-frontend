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

import base.SpecBase
import models.VatRateType.Standard
import models.upscan.{CsvError, CsvValidationException}
import models.{Country, VatRate}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import services.VatRateService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class CsvValidatorSpec extends SpecBase with MockitoSugar with Matchers with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockVatRateService = mock[VatRateService]
  private val validator = new CsvValidator(mockVatRateService)

  private def rows(csv: String): Try[Seq[Array[String]]] = CsvParserService.split(csv)
  private def rowsAndValidateAndThrow(csv: String) = for {
    x <- Future.fromTry(rows(csv))
    y <- validator.validateOrThrow(x, period, false)
  } yield y

  private val validCSVContent: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidCSVCountryFrom: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"Frunce","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidCSVCountryTo: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","Frunce","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidCharachterCsv: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","@"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidNumberCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01.098","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val negativeNumberCsv: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","-£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val tooManyDecimalsCsv: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.014","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidEmptyCellCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidVatRateCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","12.50%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidMultipleCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","12.50%","£1200","£140"
      |"Northern Ireland","France","20","33,333",""
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidDuplicateVAtRate: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidContentWrongPlace: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","13%","France","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidColumnSize: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140", "36"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidVatRateWithTooDecimalPlaces: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19.00567%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","Austria","19%","£1200","£140"
      |""".stripMargin
  }

  private val invalidCountryFromAndToAreSameNotOMP: String = {
    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"Country your goods sold from", "EU country you sold those goods to","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","19%","£1200","£140"
      |"Northern Ireland","France","20","33,333","£4423"
      |"Austria","France","13%","150.01","£15"
      |"France","France","20%","£1200","£140"
      |""".stripMargin
  }

  override def beforeEach(): Unit = {
    reset(mockVatRateService)
    super.beforeEach()


    when(mockVatRateService.vatRates(eqTo(period), argThat[Country](_.name.equalsIgnoreCase("Germany")))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq(
        VatRate(BigDecimal(19), Standard, period.firstDay),
        VatRate(BigDecimal(7), Standard, period.firstDay)
      )))

    when(mockVatRateService.vatRates(eqTo(period), argThat[Country](_.name.equalsIgnoreCase("France")))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq(
        VatRate(BigDecimal(20), Standard, period.firstDay),
        VatRate(BigDecimal(13), Standard, period.firstDay),
        VatRate(BigDecimal(10), Standard, period.firstDay),
        VatRate(BigDecimal(5.5), Standard, period.firstDay)
      )))

    when(mockVatRateService.vatRates(eqTo(period), argThat[Country](_.name.equalsIgnoreCase("Austria")))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq(
        VatRate(BigDecimal(20), Standard, period.firstDay),
        VatRate(BigDecimal(19), Standard, period.firstDay),
        VatRate(BigDecimal(13), Standard, period.firstDay),
        VatRate(BigDecimal(10), Standard, period.firstDay)
      )))
  }

  "CSV Validator must" - {

    "CsvValidator.validateOrThrow" - {

      "succeed for a valid CSV" in {

        whenReady(rowsAndValidateAndThrow(validCSVContent)) { _ =>
          succeed
        }
      }

      "fail with InvalidCountry for a unknown countryFrom" in {

        whenReady(rowsAndValidateAndThrow(invalidCSVCountryFrom).failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidCountry => e.cellRef } must contain("A7")
        }
      }

      "fail with InvalidCountry for a unknown countryTo" in {

        val validatorError = rowsAndValidateAndThrow(invalidCSVCountryTo)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidCountry => e.cellRef } must contain("B5")
        }
      }

      "fail with InvalidNumberFormat for an invalid money value" in {

        val validatorError = rowsAndValidateAndThrow(invalidNumberCSV)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("D6")
        }

      }

      "fail with NegativeNumber for a negative number value" in {

        val validatorError = rowsAndValidateAndThrow(negativeNumberCsv)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.NegativeNumber => e.cellRef } must contain("D4")
        }
      }

      "fail with TooManyDecimals for a number with too many decimals" in {

        val validatorError = rowsAndValidateAndThrow(tooManyDecimalsCsv)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.TooMayDecimals => e.cellRef } must contain("D6")
        }
      }

      "fail with BlankCell for an empty cell" in {

        val validatorError = rowsAndValidateAndThrow(invalidEmptyCellCSV)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.BlankCell => e.cellRef } must contain("D5")
        }
      }

      "fail with InvalidNumberFormat for an invalid symbol in a value" in {

        val validatorError = rowsAndValidateAndThrow(invalidCharachterCsv)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("E4")
        }
      }

      "fail with VatRateNotAllowed for when a VAT rate is not available for that country and period" in {

        val validatorError = rowsAndValidateAndThrow(invalidVatRateCSV)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.VatRateNotAllowed => e.cellRef } must contain("C4")
        }
      }

      "fail with DuplicateVatRate for when a VAT rate is duplicated for the same country" in {

        val validatorError = rowsAndValidateAndThrow(invalidDuplicateVAtRate)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.DuplicateVatRate => e.cellRef } must contain("C7")
        }
      }

      "fail with multiple errors for when data is in the wrong position" in {

        val validatorError = rowsAndValidateAndThrow(invalidContentWrongPlace)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.map(_.cellRef) must contain allOf("B6", "C6")
        }
      }

      "fail with TooManyColumns when there is more than 5 columns" in {

        val validatorError = rowsAndValidateAndThrow(invalidColumnSize)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.TooManyColumns => e.cellRef } must contain("D4")
        }
      }

      "fail with InvalidNumberFormat for VAT rates with more than two decimal places" in {

        val validatorError = rowsAndValidateAndThrow(invalidVatRateWithTooDecimalPlaces)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("C4")
        }
      }

      "fail with multiple errors when multiple things are wrong" in {

        val validatorError = rowsAndValidateAndThrow(invalidMultipleCSV)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.map(_.cellRef) must contain allOf("C4", "E5")
        }
      }

      "fail with SameToAndFromCountry for when countryFrom is same as countryTo and not an OMP" in {

        val validatorError = rowsAndValidateAndThrow(invalidCountryFromAndToAreSameNotOMP)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.SameToAndFromCountry => e.cellRef } must contain("B7")
        }
      }
    }
  }
}

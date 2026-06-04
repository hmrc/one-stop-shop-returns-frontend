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
import models.Index
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import pages.{CountryOfConsumptionFromEuPage, CountryOfConsumptionFromNiPage, CountryOfSaleFromEuPage, NetValueOfSalesFromEuPage, NetValueOfSalesFromNiPage, SoldGoodsFromEuPage, SoldGoodsFromNiPage, VatOnSalesFromEuPage, VatOnSalesFromNiPage}

class CsvParserServiceSpec extends SpecBase with MockitoSugar with Matchers with BeforeAndAfterEach {

  "CSV Parser must" - {
    CsvParserService.split("") mustBe Seq()
  }

  "parse for a simple string of elements without quotes with multiple rows in the CSV with no quotes" in {
    val validCSVContent: String = {
      """"HM Revenue and Customs logo","","",""
        |"Import One Stop Shop VAT return","","",""
        |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
        |"Northern Ireland","Germany","12.50%","£1200","£140"
        |"Northern Ireland","France","15","33,333","£4423"
        |"Austria","France","10%","150.01","£15"
        |"France","Austria","12.50%","£1200","£140"
        |""".stripMargin
    }

    val actual: Seq[Seq[String]] = CsvParserService.split(validCSVContent).map(_.toSeq)
    val expected: Seq[Seq[String]] = Seq(
      Seq("HM Revenue and Customs logo","","",""),
      Seq("Import One Stop Shop VAT return", "", "", ""),
      Seq("CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"),
      Seq("Northern Ireland","Germany","12.50%","£1200","£140"),
      Seq("Northern Ireland","France","15","33,333","£4423"),
      Seq("Austria","France","10%","150.01","£15"),
      Seq("France","Austria","12.50%","£1200","£140")
    )

    actual `mustBe` expected
  }

  "populate user answers from CSV for NI to EU sales" in {

    val csv =
      """"HM Revenue and Customs logo","","",""
        |"Import One Stop Shop VAT return","","",""
        |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
        |"Northern Ireland","Germany","12.50%","£1200","£140"
        |"Northern Ireland","France","15","33,333","£4423"
        |""".stripMargin

    val service = new CsvParserService()
    val result = service.populateUserAnswersFromCsv(emptyUserAnswers, csv)

    result.isSuccess mustBe true

    val updated = result.get

    updated.get(SoldGoodsFromNiPage).value mustBe true
    updated.get(SoldGoodsFromEuPage).value mustBe false

    updated.get(CountryOfConsumptionFromNiPage(Index(0))).value.name mustBe "France"
    updated.get(NetValueOfSalesFromNiPage(Index(0), Index(0))).value mustBe BigDecimal(33333)
    updated.get(VatOnSalesFromNiPage(Index(0), Index(0))).value.amount mustBe BigDecimal(4423)

    updated.get(CountryOfConsumptionFromNiPage(Index(1))).value.name mustBe "Germany"
    updated.get(NetValueOfSalesFromNiPage(Index(1), Index(0))).value mustBe BigDecimal(1200)
    updated.get(VatOnSalesFromNiPage(Index(1), Index(0))).value.amount mustBe BigDecimal(140)

  }

  "populate user answers from CSV for EU to EU sales" in {

    val csv =
      """"HM Revenue and Customs logo","","",""
        |"Import One Stop Shop VAT return","","",""
        |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
        |"Austria","France","10%","150.01","£15"
        |"France","Austria","12.50%","£1200","£140"
        |""".stripMargin

    val service = new CsvParserService()
    val result = service.populateUserAnswersFromCsv(emptyUserAnswers, csv)

    result.isSuccess mustBe true

    val updated = result.get

    updated.get(SoldGoodsFromNiPage).value mustBe false
    updated.get(SoldGoodsFromEuPage).value mustBe true

    updated.get(CountryOfSaleFromEuPage(Index(0))).value.name mustBe "Austria"
    updated.get(CountryOfConsumptionFromEuPage(Index(0), Index(0))).value.name mustBe "France"
    updated.get(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0))).value mustBe BigDecimal(150.01)
    updated.get(VatOnSalesFromEuPage(Index(0), Index(0), Index(0))).value.amount mustBe BigDecimal(15)

    updated.get(CountryOfSaleFromEuPage(Index(1))).value.name mustBe "France"
    updated.get(CountryOfConsumptionFromEuPage(Index(1), Index(0))).value.name mustBe "Austria"
    updated.get(NetValueOfSalesFromEuPage(Index(1), Index(0), Index(0))).value mustBe BigDecimal(1200)
    updated.get(VatOnSalesFromEuPage(Index(1), Index(0), Index(0))).value.amount mustBe BigDecimal(140)
  }

  "populate user answers from CSV for NI to EU and EU to EU sales" in {

    val csv =
      """"HM Revenue and Customs logo","","",""
        |"Import One Stop Shop VAT return","","",""
        |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
        |"Northern Ireland","Germany","12.50%","£1200","£140"
        |"Northern Ireland","France","15","33,333","£4423"
        |"Austria","France","10%","150.01","£15"
        |"France","Austria","12.50%","£1200","£140"
        |""".stripMargin

    val service = new CsvParserService()
    val result = service.populateUserAnswersFromCsv(emptyUserAnswers, csv)

    result.isSuccess mustBe true

    val updated = result.get

    updated.get(SoldGoodsFromNiPage).value mustBe true
    updated.get(SoldGoodsFromEuPage).value mustBe true

    updated.get(CountryOfConsumptionFromNiPage(Index(0))).value.name mustBe "France"
    updated.get(NetValueOfSalesFromNiPage(Index(0), Index(0))).value mustBe BigDecimal(33333)
    updated.get(VatOnSalesFromNiPage(Index(0), Index(0))).value.amount mustBe BigDecimal(4423)

    updated.get(CountryOfConsumptionFromNiPage(Index(1))).value.name mustBe "Germany"
    updated.get(NetValueOfSalesFromNiPage(Index(1), Index(0))).value mustBe BigDecimal(1200)
    updated.get(VatOnSalesFromNiPage(Index(1), Index(0))).value.amount mustBe BigDecimal(140)

    updated.get(CountryOfSaleFromEuPage(Index(0))).value.name mustBe "Austria"
    updated.get(CountryOfConsumptionFromEuPage(Index(0), Index(0))).value.name mustBe "France"
    updated.get(NetValueOfSalesFromEuPage(Index(0), Index(0), Index(0))).value mustBe BigDecimal(150.01)
    updated.get(VatOnSalesFromEuPage(Index(0), Index(0), Index(0))).value.amount mustBe BigDecimal(15)

    updated.get(CountryOfSaleFromEuPage(Index(1))).value.name mustBe "France"
    updated.get(CountryOfConsumptionFromEuPage(Index(1), Index(0))).value.name mustBe "Austria"
    updated.get(NetValueOfSalesFromEuPage(Index(1), Index(0), Index(0))).value mustBe BigDecimal(1200)
    updated.get(VatOnSalesFromEuPage(Index(1), Index(0), Index(0))).value.amount mustBe BigDecimal(140)

  }

}

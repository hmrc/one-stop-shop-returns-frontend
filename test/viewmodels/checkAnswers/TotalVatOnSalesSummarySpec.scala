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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import models.{CheckMode, Country, SalesAtVatRate, VatRate, VatRateType}
import pages.{CountryOfConsumptionFromNiPage, SalesAtVatRateFromNiPage, VatRatesFromNiPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalVatOnSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  "TotalVatOnSalesSummary" - {

    "must show correct total vat owed" in {
      val expectedResult = SummaryListRowViewModel(
        "checkYourAnswers.vatOnSales.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;1,000")),
        Seq.empty
      )

      TotalVatOnSalesSummary.row(BigDecimal(1000)) mustBe Some(expectedResult)
    }
  }
}

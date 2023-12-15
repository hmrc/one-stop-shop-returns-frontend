/*
 * Copyright 2023 HM Revenue & Customs
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

package viewmodels.yourAccount

import base.SpecBase
import models.Quarter.{Q1, Q3, Q4}
import models.StandardPeriod
import models.SubmissionStatus.{Due, Next, Overdue}

class ReturnsViewModelSpec extends SpecBase {
  val app = applicationBuilder().build()

  private val period1 = StandardPeriod(2021, Q3)
  private val period2 = StandardPeriod(2021, Q4)
  private val period3 = StandardPeriod(2022, Q1)

  "must return correct view model when" - {

    "excluded" - {

      "and there is no returns due, no returns overdue and none in progress" in {

        val returns = Seq.empty
        val resultModel = ReturnsViewModel(returns)(messages(app))
        resultModel.contents mustBe Seq.empty
        resultModel.linkToStart mustBe None
      }
    }

    "there is no returns due, multiple returns overdue and none in progress" in {

      val returns = Seq(
        Return.fromPeriod(period1, Overdue, false, true),
        Return.fromPeriod(period2, Overdue, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Start your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(period1).url
    }

    "there is no returns due, multiple returns overdue and one in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, true, true),
        Return.fromPeriod(period2, Overdue, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Continue your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(period1).url
    }

    "there is no returns due, one return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, false, true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Start your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(period1).url
    }

    "there is no returns due, one return overdue and one in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, true, true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return in progress."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Continue your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(period1).url
    }

    "there is one return due, multiple returns overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, false, true),
        Return.fromPeriod(period2, Overdue, false, false),
        Return.fromPeriod(period3, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("January to March 2022 is due by 30 April 2022."))
      assert(resultModel.contents.map(p => p.content).contains("You also have 2 overdue returns."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Start your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(period1).url
    }

    "there is one returns due, multiple returns overdue and one in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, true, true),
        Return.fromPeriod(period2, Overdue, false, false),
        Return.fromPeriod(period3, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("January to March 2022 is due by 30 April 2022."))
      assert(resultModel.contents.map(p => p.content).contains("You also have 2 overdue returns."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Continue your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(period1).url
    }

    "there is one returns due, one return overdue and one in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, true, true),
        Return.fromPeriod(period2, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("October to December 2021 is due by 31 January 2022."))
      assert(resultModel.contents.map(p => p.content).contains("You also have an overdue return in progress."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Continue your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(period1).url
    }

    "there is one returns due, one return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, false, true),
        Return.fromPeriod(period2, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("October to December 2021 is due by 31 January 2022."))
      assert(resultModel.contents.map(p => p.content).contains("You also have an overdue return."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Start your July to September 2021 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(period1).url
    }

    "there is one returns due, no return overdue and one in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Due, true, true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains(
          """Your return for 1 July to 30 September 2021 is in progress.
            |<br>This is due by 31 October 2021.
            |<br>""".stripMargin))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Continue your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(period1).url
    }

    "there is one returns due, no return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Due, false, true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("July to September 2021 is due by 31 October 2021."))
      resultModel.linkToStart mustBe (defined)
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(period1).url
    }

    "there is no returns due, no return overdue" in {
      val returns = Seq(
        Return.fromPeriod(period1, Next, true, true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("""You can complete your July to September 2021 return from <span class="govuk-body govuk-!-font-weight-bold">1 October 2021</span>."""))
      resultModel.linkToStart must not be defined
    }
  }

}

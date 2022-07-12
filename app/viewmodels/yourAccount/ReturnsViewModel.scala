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

package viewmodels.yourAccount

import models.Period
import models.SubmissionStatus.{Due, Next, Overdue}
import play.api.i18n.Messages
import play.twirl.api.Html

import java.time.format.DateTimeFormatter

case class ReturnsViewModel(
                             content: Html,
                             linkToStart: Option[Html] = None
                           )

object ReturnsViewModel {
  def apply(returns: Seq[Return])(implicit messages: Messages): ReturnsViewModel = {
    val inProgress = returns.find(_.inProgress)
    val returnDue = returns.find(_.submissionStatus == Due)
    val overdueReturns = returns.filter(_.submissionStatus == Overdue)
    val nextReturn = returns.find(_.submissionStatus == Next)

    nextReturn.map(
      nextReturn =>
        ReturnsViewModel(content =
          Html(nextReturnParagraph(nextReturn.period))
        )
    ).getOrElse(
      dueReturnsModel(overdueReturns, inProgress, returnDue)
    )
  }

  private def startDueReturnLink(period: Period)(implicit messages: Messages) = Html(
    s"""<p class="govuk-body">
       |                <a class="govuk-link" href="${controllers.routes.StartReturnController.onPageLoad(period).url}" id="start-your-return">
       |                ${messages("index.yourReturns.dueReturn.startReturn")}
       |                </a>
       |                </p>""".stripMargin
  )

  private def startOverdueReturnLink(period: Period)(implicit messages: Messages) = Html(
    s"""<p class="govuk-body">
    <a class="govuk-link" href="${controllers.routes.StartReturnController.onPageLoad(period).url}" id="start-your-return">
    ${messages("index.yourReturns.startReturn", period.displayShortText)}
    </a>
    </p>"""
  )

  private def continueDueReturnLink(period: Period)(implicit messages: Messages) = Html(
    s"""<p class="govuk-body">
              <a class="govuk-link" href="${controllers.routes.ContinueReturnController.onPageLoad(period).url}" id="continue-your-return">
              ${messages("index.yourReturns.dueReturn.continueReturn")}
              </a>
              </p>"""
  )

  private def continueOverdueReturnLink(period: Period)(implicit messages: Messages) = Html(
    s"""<p class="govuk-body">
      <a class="govuk-link" href="${controllers.routes.ContinueReturnController.onPageLoad(period).url}" id="continue-your-return">
      ${messages("index.yourReturns.continueReturn", period.displayShortText)}
      </a>
      </p>"""
  )

  private def returnDueParagraph(period: Period)(implicit messages: Messages) =
    s"""<p class="govuk-body">
       |${messages("index.yourReturns.returnDue", period.displayShortText, period.paymentDeadlineDisplay)}</p>""".stripMargin

  private def returnDueInProgressParagraph(period: Period)(implicit messages: Messages) =
    s"""<p class="govuk-body">
       |${messages("index.yourReturns.inProgress", period.displayText)}
       |<br>${messages("index.yourReturns.inProgress.due", period.paymentDeadlineDisplay)}
       |<br></p>""".stripMargin

  private def returnOverdueSingularParagraph()(implicit messages: Messages) =
    s"""<p class="govuk-body">${messages("index.yourReturns.returnsOverdue.singular")}</p>"""

  private def returnOverdueParagraph()(implicit messages: Messages) = s"""<p class="govuk-body">${messages("index.yourReturns.returnOverdue")}</p>"""

  private def returnOverdueInProgressAdditionalParagraph()(implicit messages: Messages) =
    s"""<p class="govuk-body">${messages("index.yourReturns.returnOverdue.additional.inProgress")}</p>"""

  private def returnOverdueInProgressParagraph()(implicit messages: Messages) =
    s"""<p class="govuk-body">${messages("index.yourReturns.returnOverdue.inProgress")}</p>"""

  private def returnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    s"""<p class="govuk-body">${messages("index.yourReturns.returnsOverdue", numberOfOverdueReturns)}</p>"""

  private def onlyReturnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    s"""<p class="govuk-body">${messages("index.yourReturns.onlyReturnsOverdue", numberOfOverdueReturns)}</p>"""

  private def nextReturnParagraph(nextReturn: Period)(implicit messages: Messages) =
    s"""<p class="govuk-body" id="next-period">${messages("index.nextPeriod", nextReturn.displayShortText, nextReturn.lastDay.plusDays(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy")))}</p>"""

  private def dueReturnsModel(overdueReturns: Seq[Return], currentReturn: Option[Return], dueReturn: Option[Return])(implicit messages: Messages) = {
    (overdueReturns.size, currentReturn, dueReturn) match {
      case (0, None, Some(dueReturn)) =>
        ReturnsViewModel(
          Html(returnDueParagraph(dueReturn.period)),
          Some(startDueReturnLink(dueReturn.period))
        )
      case (0, Some(_), Some(dueReturn)) =>
        ReturnsViewModel(
          Html(returnDueInProgressParagraph(dueReturn.period)),
          Some(continueDueReturnLink(dueReturn.period))
        )
      case (1, None, _) =>
        val paragraph = dueReturn.map(dueReturn =>
          returnDueParagraph(dueReturn.period) + returnOverdueSingularParagraph()).getOrElse(returnOverdueParagraph())
        ReturnsViewModel(
          Html(paragraph),
          Some(startOverdueReturnLink(overdueReturns.head.period))
        )
      case (1, Some(inProgress), _) =>
        val paragraph = dueReturn.map(dueReturn =>
          returnDueParagraph(dueReturn.period) + returnOverdueInProgressAdditionalParagraph()).getOrElse(returnOverdueInProgressParagraph())
        ReturnsViewModel(
          Html(paragraph),
          Some(continueOverdueReturnLink(inProgress.period))
        )
      case (x, None, _) =>
        val paragraph = dueReturn.map(dueReturn =>
          returnDueParagraph(dueReturn.period) + returnsOverdueParagraph(x)).getOrElse(onlyReturnsOverdueParagraph(x))
        ReturnsViewModel(
          Html(paragraph),
          Some(startOverdueReturnLink(overdueReturns.minBy(_.period.lastDay.toEpochDay).period))
        )
      case (x, Some(inProgress), _) =>
        val paragraph = dueReturn.map(dueReturn =>
          returnDueParagraph(dueReturn.period) + returnsOverdueParagraph(x)).getOrElse(onlyReturnsOverdueParagraph(x))
        ReturnsViewModel(
          Html(paragraph),
          Some(continueOverdueReturnLink(inProgress.period))
        )
    }
  }
}
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

import models.Period
import models.SubmissionStatus.{Due, Next, Overdue}
import play.api.i18n.Messages
import viewmodels.{LinkModel, Paragraph, ParagraphSimple, ParagraphWithId}

import java.time.format.DateTimeFormatter

case class ReturnsViewModel(
                             contents: Seq[Paragraph],
                             linkToStart: Option[LinkModel] = None
                           )

object ReturnsViewModel {
  def apply(returns: Seq[Return])(implicit messages: Messages): ReturnsViewModel = {
    val inProgress = returns.find(_.inProgress)
    val returnDue = returns.find(_.submissionStatus == Due)
    val overdueReturns = returns.filter(_.submissionStatus == Overdue)
    val nextReturn = returns.find(_.submissionStatus == Next)

    nextReturn.map(
      nextReturn =>
        ReturnsViewModel(
          contents = Seq(nextReturnParagraph(nextReturn.period))
        )
    ).getOrElse(
      dueReturnsModel(overdueReturns, inProgress, returnDue)
    )
  }

  private def startDueReturnLink(period: Period)(implicit messages: Messages) = {
    LinkModel(
      linkText = messages("index.yourReturns.dueReturn.startReturn"),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(period).url
    )
  }

  private def startOverdueReturnLink(period: Period)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("index.yourReturns.startReturn", period.displayShortText),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(period).url
    )

  private def continueDueReturnLink(period: Period)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("index.yourReturns.dueReturn.continueReturn"),
      id = "continue-your-return",
      url = controllers.routes.ContinueReturnController.onPageLoad(period).url
    )

  private def continueOverdueReturnLink(period: Period)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("index.yourReturns.continueReturn", period.displayShortText),
      id = "continue-your-return",
      url = controllers.routes.ContinueReturnController.onPageLoad(period).url
    )

  private def returnDueParagraph(period: Period)(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnDue", period.displayShortText, period.paymentDeadlineDisplay))

  private def returnDueInProgressParagraph(period: Period)(implicit messages: Messages) =
    ParagraphSimple(s"""${messages("index.yourReturns.inProgress", period.displayText)}
       |<br>${messages("index.yourReturns.inProgress.due", period.paymentDeadlineDisplay)}
       |<br>""".stripMargin)

  private def returnOverdueSingularParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnsOverdue.singular"))

  private def returnOverdueParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnOverdue"))

  private def returnOverdueInProgressAdditionalParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnOverdue.additional.inProgress"))

  private def returnOverdueInProgressParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnOverdue.inProgress"))

  private def returnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.returnsOverdue", numberOfOverdueReturns))

  private def onlyReturnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("index.yourReturns.onlyReturnsOverdue", numberOfOverdueReturns))

  private def nextReturnParagraph(nextReturn: Period)(implicit messages: Messages) =
    ParagraphWithId(messages("index.nextPeriod", nextReturn.displayShortText, nextReturn.lastDay.plusDays(1)
      .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
      "next-period"
    )

  private def dueReturnsModel(overdueReturns: Seq[Return], currentReturn: Option[Return], dueReturn: Option[Return])(implicit messages: Messages) = {
    (overdueReturns.size, currentReturn, dueReturn) match {
      case (0, None, None) =>
        ReturnsViewModel(
          contents = Seq.empty,
          linkToStart = None
        )
      case (0, None, Some(dueReturn)) =>
        ReturnsViewModel(
          contents = Seq(returnDueParagraph(dueReturn.period)),
          linkToStart = Some(startDueReturnLink(dueReturn.period))
        )
      case (0, Some(_), Some(dueReturn)) =>
        ReturnsViewModel(
          contents = Seq(returnDueInProgressParagraph(dueReturn.period)),
          linkToStart = Some(continueDueReturnLink(dueReturn.period))
        )
      case (1, None, _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period),returnOverdueSingularParagraph())).getOrElse(Seq(returnOverdueParagraph()))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(overdueReturns.head.period))
        )
      case (1, Some(inProgress), _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period), returnOverdueInProgressAdditionalParagraph()))
          .getOrElse(Seq(returnOverdueInProgressParagraph()))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(continueOverdueReturnLink(inProgress.period))
        )
      case (x, None, _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period), returnsOverdueParagraph(x)))
          .getOrElse(Seq(onlyReturnsOverdueParagraph(x)))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(overdueReturns.minBy(_.period.lastDay.toEpochDay).period))
        )
      case (x, Some(inProgress), _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period), returnsOverdueParagraph(x)))
          .getOrElse(Seq(onlyReturnsOverdueParagraph(x)))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(continueOverdueReturnLink(inProgress.period))
        )
    }
  }
}
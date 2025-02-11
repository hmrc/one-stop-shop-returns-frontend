/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import models.Quarter.{Q1, Q2, Q3, Q4}
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.PathBindable
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

trait Period {
  val year: Int
  val quarter: Quarter
  val firstDay: LocalDate
  val lastDay: LocalDate
  val isPartial: Boolean

  protected val firstDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
  protected val lastDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  protected val firstQuarterFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM")
  protected val lastQuarterFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  val paymentDeadline: LocalDate = LocalDate.of(year, quarter.startMonth, 1).plusMonths(4).minusDays(1)

  val paymentDeadlineDisplay: String = paymentDeadline.format(lastDayFormatter)

  def isOverdue(clock: Clock): Boolean = {
    paymentDeadline.isBefore(LocalDate.now(clock))
  }

  def isInExpiredPeriod(clock: Clock): Boolean = {
    val threeYearsAfterPayment = paymentDeadline.plusYears(3)
    LocalDate.now(clock).isAfter(threeYearsAfterPayment)
  }

  def displayText(implicit messages: Messages): String =
    s"${firstDay.format(firstDayFormatter)} ${messages("site.to")} ${lastDay.format(lastDayFormatter)}"

  def displayQuarterText(implicit messages: Messages): String =
    s"${firstDay.format(firstQuarterFormatter)} ${messages("site.to")} ${lastDay.format(lastQuarterFormatter)}"

  def getNextPeriod: Period = {
    quarter match {
      case Q4 =>
        StandardPeriod(year + 1, Q1)
      case Q3 =>
        StandardPeriod(year, Q4)
      case Q2 =>
        StandardPeriod(year, Q3)
      case Q1 =>
        StandardPeriod(year, Q2)
    }
  }

  def getPreviousPeriod: Period = {
    quarter match {
      case Q4 =>
        StandardPeriod(year, Q3)
      case Q3 =>
        StandardPeriod(year, Q2)
      case Q2 =>
        StandardPeriod(year, Q1)
      case Q1 =>
        StandardPeriod(year - 1, Q4)
    }
  }

  override def toString: String = s"$year-${quarter.toString}"

  def toEtmpPeriodString: String = {
    val standardPeriod = StandardPeriod(this.year, this.quarter)
    val year = standardPeriod.year
    val quarter = standardPeriod.quarter
    val etmpQuarter = quarter.toString.replace("Q", "C")
    val lastYearDigits = year.toString.substring(2)

    s"$lastYearDigits$etmpQuarter"
  }

}

case class StandardPeriod(year: Int, quarter: Quarter) extends Period {

  override val firstDay: LocalDate = LocalDate.of(year, quarter.startMonth, 1)
  override val lastDay: LocalDate = firstDay.plusMonths(3).minusDays(1)
  override val isPartial: Boolean = false

  private val firstMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  def displayShortText(implicit messages: Messages): String =
    s"${firstDay.format(firstMonthFormatter)} ${messages("site.to")} ${lastDay.format(lastMonthYearFormatter)}"

}

object StandardPeriod {
  def apply(yearString: String, quarterString: String): Try[StandardPeriod] =
    for {
      year <- Try(yearString.toInt)
      quarter <- Quarter.fromString(quarterString)
    } yield StandardPeriod(year, quarter)

  def options(periods: Seq[StandardPeriod])(implicit messages: Messages): Seq[RadioItem] = periods.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(value.displayText),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
  }

  def fromString(string: String): Option[StandardPeriod] = {
    Period.fromString(string).map(fromPeriod)
  }

  def fromPeriod(period: Period): StandardPeriod = {
    StandardPeriod(period.year, period.quarter)
  }

  implicit val format: OFormat[StandardPeriod] = Json.format[StandardPeriod]
}

object Period {

  protected val pattern: Regex = """(\d{4})-(Q[1-4])""".r.anchored

  def fromString(string: String): Option[Period] =
    string match {
      case pattern(yearString, quarterString) =>
        StandardPeriod(yearString, quarterString).toOption
      case _ =>
        None
    }

  implicit val pathBindable: PathBindable[Period] = new PathBindable[Period] {

    override def bind(key: String, value: String): Either[String, Period] =
      fromString(value) match {
        case Some(period) => Right(period)
        case None => Left("Invalid period")
      }

    override def unbind(key: String, value: Period): String =
      value.toString
  }

  def fromEtmpPeriodKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val etmpQuarterString = key.drop(2)
    val quarterString = etmpQuarterString.replace("C", "Q")
    val year = s"20$yearLast2".toInt
    val quarter = Quarter.fromString(quarterString) match {
      case Success(q) => q
      case Failure(_) => throw new IllegalArgumentException(s"Invalid quarter string: $quarterString")
    }
    StandardPeriod(year, quarter)
  }

  def reads: Reads[Period] =
    PartialReturnPeriod.format.widen[Period] orElse
      StandardPeriod.format.widen[Period]

  def writes: Writes[Period] = Writes {
    case s: StandardPeriod => Json.toJson(s)(StandardPeriod.format)
    case p: PartialReturnPeriod => Json.toJson(p)(PartialReturnPeriod.format)
  }

  def getPeriod(date: LocalDate): Period = {
    val quarter = Quarter.fromString(date.format(DateTimeFormatter.ofPattern("QQQ")))

    quarter match {
      case Success(value) =>
        StandardPeriod(date.getYear, value)
      case Failure(exception) =>
        throw exception
    }
  }

  implicit def format: Format[Period] = Format(reads, writes)
}

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

package models

import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.PathBindable
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.{Clock, LocalDate}
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.matching.Regex

trait Period {
  val year: Int
  val quarter: Quarter
  val firstDay: LocalDate
  val lastDay: LocalDate
  val isPartial: Boolean

  protected val firstDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
  protected val lastDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  val paymentDeadline: LocalDate = LocalDate.of(year, quarter.startMonth, 1).plusMonths(4).minusDays(1)

  val paymentDeadlineDisplay: String = paymentDeadline.format(lastDayFormatter)

  def isOverdue(clock: Clock): Boolean = {
    paymentDeadline.isBefore(LocalDate.now(clock))
  }

  def displayText(implicit messages: Messages): String =
    s"${firstDay.format(firstDayFormatter)} ${messages("site.to")} ${lastDay.format(lastDayFormatter)}"

  override def toString: String = s"$year-${quarter.toString}"

}

case class StandardPeriod(year: Int, quarter: Quarter) extends Period {

  override val firstDay: LocalDate = LocalDate.of(year, quarter.startMonth, 1)
  override val lastDay: LocalDate = firstDay.plusMonths(3).minusDays(1)
  val rejoinDate: LocalDate = firstDay.plusYears(2)
  override val isPartial: Boolean = false

  private val firstMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  private val rejoinDateFormatter = DateTimeFormatter.ofPattern("d MMMM YYYY")


  def displayShortText(implicit messages: Messages): String =
    s"${firstDay.format(firstMonthFormatter)} ${messages("site.to")} ${lastDay.format(lastMonthYearFormatter)}"

  def displayRejoinDate(implicit messages: Messages): String =
    s"${rejoinDate.format(rejoinDateFormatter)}"

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

  /*  implicit val queryBindable: QueryStringBindable[Period] = new QueryStringBindable[Period] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Period]] = {
        params.get(key).flatMap(_.headOption).map {
          periodString =>
            fromString(periodString) match {
              case Some(period) => Right(period)
              case _ => Left("Invalid period")
            }
        }
      }

      override def unbind(key: String, value: Period): String = {
        s"$key=${value.toString}"
      }
    }*/

  def reads: Reads[Period] =
    PartialReturnPeriod.format.widen[Period] orElse
      StandardPeriod.format.widen[Period]

  def writes: Writes[Period] = Writes {
    case s: StandardPeriod => Json.toJson(s)(StandardPeriod.format)
    case p: PartialReturnPeriod => Json.toJson(p)(PartialReturnPeriod.format)
  }

  implicit def format: Format[Period] = Format(reads, writes)
}

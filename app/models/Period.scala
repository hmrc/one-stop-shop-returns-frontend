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

package models

import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import scala.util.Try
import scala.util.matching.Regex

case class Period(year: Int, quarter: Quarter) {

  val firstDay: LocalDate = LocalDate.of(year, quarter.startMonth, 1)
  val lastDay: LocalDate = firstDay.plusMonths(3).minusDays(1)
  val paymentDeadline: LocalDate = firstDay.plusMonths(4).minusDays(1)

  private val firstDayFormatter = DateTimeFormatter.ofPattern("d MMMM")
  private val lastDayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val firstMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  def displayText(implicit messages: Messages): String =
    s"${firstDay.format(firstDayFormatter)} ${messages("site.to")} ${lastDay.format(lastDayFormatter)}"

  def displayShortText(implicit messages: Messages): String =
    s"${firstDay.format(firstMonthFormatter)} ${messages("site.to")} ${lastDay.format(lastMonthYearFormatter)}"

  val paymentDeadlineDisplay: String = paymentDeadline.format(lastDayFormatter)

  def isOverdue(clock: Clock): Boolean = {
    paymentDeadline.isBefore(LocalDate.now(clock))
  }

  override def toString: String = s"$year-${quarter.toString}"
}

object Period {

  private val pattern: Regex = """(\d{4})-(Q[1-4])""".r.anchored

  def apply(yearString: String, quarterString: String): Try[Period] =
    for {
      year <- Try(yearString.toInt)
      quarter <- Quarter.fromString(quarterString)
    } yield Period(year, quarter)

  def fromString(string: String): Option[Period] =
    string match {
      case pattern(yearString, quarterString) =>
        Period(yearString, quarterString).toOption
      case _ =>
        None
    }

  implicit val format: OFormat[Period] = Json.format[Period]

  implicit val pathBindable: PathBindable[Period] = new PathBindable[Period] {

    override def bind(key: String, value: String): Either[String, Period] =
      fromString(value) match {
        case Some(period) => Right(period)
        case None => Left("Invalid period")
      }

    override def unbind(key: String, value: Period): String =
      value.toString
  }

  implicit val queryBindable: QueryStringBindable[Period] = new QueryStringBindable[Period] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Period]] = {
      params.get(key).flatMap(_.headOption).map{
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
    }

    def options(periods: Seq[Period])(implicit messages: Messages): Seq[RadioItem] = periods.zipWithIndex.map {
      case (value, index) =>
        RadioItem(
          content = Text(value.displayText),
          value = Some(value.toString),
          id = Some(s"value_$index")
        )
    }
  }

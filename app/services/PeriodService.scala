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

package services

import models.Quarter._
import models.{Period, StandardPeriod}
import models.Period.getPeriod

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class PeriodService @Inject()(clock: Clock) {

  def getReturnPeriods(commencementDate: LocalDate): Seq[Period] =
    getAllPeriods(commencementDate).filterNot(_.lastDay.isBefore(commencementDate))

  def getAllPeriods(commencementDate: LocalDate): Seq[Period] = {
    val firstPeriod = getPeriod(commencementDate)
    getPeriodsUntilDate(firstPeriod, LocalDate.now(clock))
  }

  private def getPeriodsUntilDate(currentPeriod: Period, endDate: LocalDate): Seq[Period] = {
    if(currentPeriod.lastDay.isBefore(endDate)) {
      Seq(currentPeriod) ++ getPeriodsUntilDate(getNextPeriod(currentPeriod), endDate)
    } else {
      Seq.empty
    }
  }

  def getNextPeriod(currentPeriod: Period): Period = {
    currentPeriod.quarter match {
      case Q4 =>
        StandardPeriod(currentPeriod.year + 1, Q1)
      case Q3 =>
        StandardPeriod(currentPeriod.year, Q4)
      case Q2 =>
        StandardPeriod(currentPeriod.year, Q3)
      case Q1 =>
        StandardPeriod(currentPeriod.year, Q2)
    }
  }

  def getPreviousPeriod(currentPeriod: Period): Period = {
    currentPeriod.quarter match {
      case Q1 =>
        StandardPeriod(currentPeriod.year - 1, Q4)
      case Q2 =>
        StandardPeriod(currentPeriod.year, Q1)
      case Q3 =>
        StandardPeriod(currentPeriod.year, Q2)
      case Q4 =>
        StandardPeriod(currentPeriod.year, Q3)
    }
  }
}

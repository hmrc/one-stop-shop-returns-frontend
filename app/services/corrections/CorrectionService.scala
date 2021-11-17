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

package services.corrections

import cats.implicits._
import models.{DataMissingError, Period, UserAnswers, ValidationResult}
import models.corrections.PeriodWithCorrections
import models.requests.corrections.CorrectionRequest
import pages.corrections.CorrectPreviousReturnPage
import queries.corrections.AllCorrectionPeriodsQuery
import services.PeriodService
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate
import javax.inject.Inject

class CorrectionService @Inject()(
                                 periodService: PeriodService
                                 ) {

  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period, commencementDate: LocalDate): ValidationResult[CorrectionRequest] = {

    if(firstPeriod(period, commencementDate)) {
      CorrectionRequest(vrn, period, List.empty).validNec
    } else {
      getCorrections(answers).map { corrections =>
        CorrectionRequest(vrn, period, corrections)
      }
    }
  }

  private def firstPeriod(period: Period, commencementDate: LocalDate): Boolean =
    periodService.getReturnPeriods(commencementDate).head == period

  private def getCorrections(answers: UserAnswers): ValidationResult[List[PeriodWithCorrections]] =
    answers.get(CorrectPreviousReturnPage) match {
      case Some(false) =>
        List.empty[PeriodWithCorrections].validNec
      case Some(true) =>
        processCorrections(answers)
      case None =>
        DataMissingError(CorrectPreviousReturnPage).invalidNec
    }

  private def processCorrections(answers: UserAnswers): ValidationResult[List[PeriodWithCorrections]] = {
    answers.get(AllCorrectionPeriodsQuery) match {
      case Some(value) if value.nonEmpty =>
        value.validNec
      case _ =>
        DataMissingError(AllCorrectionPeriodsQuery).invalidNec
    }
  }

}

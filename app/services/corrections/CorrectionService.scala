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

package services.corrections

import cats.implicits._
import connectors.corrections.CorrectionConnector
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import models.requests.corrections.CorrectionRequest
import models.{DataMissingError, Index, Period, StandardPeriod, UserAnswers, ValidationResult}
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.Lang.logger
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import services.PeriodService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionService @Inject()(
                                   periodService: PeriodService,
                                   connector: CorrectionConnector
                                 ) {

  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period, commencementDate: LocalDate): ValidationResult[CorrectionRequest] = {
    if (firstPeriod(period, commencementDate)) {
      CorrectionRequest(vrn, StandardPeriod.fromPeriod(period), List.empty).validNec
    } else {
      getCorrections(answers).map { corrections =>
        CorrectionRequest(vrn, StandardPeriod.fromPeriod(period), corrections)
      }
    }
  }

  private def firstPeriod(period: Period, commencementDate: LocalDate): Boolean = {
    periodService.getReturnPeriods(commencementDate).headOption match {
      case Some(firstAvailableReturnPeriod) => firstAvailableReturnPeriod == period
      case _ => false
    }
  }

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
      case Some(periodWithCorrections) if periodWithCorrections.nonEmpty =>
        periodWithCorrections.zipWithIndex.map {
          case (_, index) =>
            processCorrectionsToCountry(answers, Index(index))
        }.sequence.map{ _ =>
          periodWithCorrections
        }
      case _ =>
        DataMissingError(AllCorrectionPeriodsQuery).invalidNec
    }
  }

  private def processCorrectionsToCountry(answers: UserAnswers, periodIndex: Index): ValidationResult[List[CorrectionToCountry]] = {
    answers.get(AllCorrectionCountriesQuery(periodIndex)) match {
      case Some(value) if value.nonEmpty =>
        value.zipWithIndex.map {
          case (_, index) =>
            processCorrectionToCountry(answers, periodIndex, Index(index))
        }.sequence
      case _ =>
        DataMissingError(AllCorrectionCountriesQuery(periodIndex)).invalidNec
    }
  }

  private def processCorrectionToCountry(answers: UserAnswers, periodIndex: Index, countryIndex: Index): ValidationResult[CorrectionToCountry] = {
    answers.get(CorrectionToCountryQuery(periodIndex, countryIndex)) match {
      case Some(value) =>
        value match {
          case CorrectionToCountry(_, Some(_)) =>      value.validNec
          case CorrectionToCountry(_, None) =>         DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)).invalidNec

        }
      case _ =>
        DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)).invalidNec
    }
  }

  def getCorrectionsForPeriod(period: Period)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[CorrectionToCountry]] = {
    connector.getForCorrectionPeriod(period).map{
      response => response match {
        case Right(payloads) => {
          payloads.flatMap{payload => payload.corrections.flatMap{_.correctionsToCountry.getOrElse(List.empty)}}}
        case Left(error) =>
          logger.error(s"there was an error when getting corrections for period: $error")
          throw new Exception(error.toString)
      }
    }
  }
}

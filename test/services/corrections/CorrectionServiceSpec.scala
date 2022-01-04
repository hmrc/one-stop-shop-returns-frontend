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

import base.SpecBase
import connectors.corrections.CorrectionConnector
import cats.data.NonEmptyChain
import cats.data.Validated.{Invalid, Valid}
import models.requests.corrections.CorrectionRequest
import models.responses.UnexpectedResponseStatus
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections}
import models.{Country, DataMissingError, Period, VatOnSales}
import models.Quarter.{Q1, Q2, Q3, Q4}
import models.domain.{SalesDetails, SalesToCountry}
import models.VatOnSalesChoice.Standard
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery}
import services.PeriodService
import uk.gov.hmrc.domain.Vrn
import viewmodels.previousReturn.corrections.CorrectionSummary

import java.time.Instant

class CorrectionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".fromUserAnswers" - {

    "must return a vat return request" - {

      "when the user has made no corrections" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, false).success.value

        private val expectedResult = CorrectionRequest(vrn, period, List.empty)

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        service.fromUserAnswers(answers, vrn, period, registration.commencementDate) mustEqual Valid(expectedResult)
      }

      "when the user has made a correction to a single period" in new Fixture {

        private val country = arbitrary[Country].sample.value
        private val correctionAmount = BigDecimal(1000)
        private val correctionPeriod = Period(2021, Q1)

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value
            .set(CorrectionReturnPeriodPage(index), correctionPeriod).success.value
            .set(CorrectionCountryPage(index, index), country).success.value
            .set(CountryVatCorrectionPage(index, index), correctionAmount).success.value

        private val expectedResult = CorrectionRequest(
          vrn = vrn,
          period = period,
          corrections = List(
            PeriodWithCorrections(
              correctionReturnPeriod = correctionPeriod,
              correctionsToCountry = List(
                CorrectionToCountry(
                  correctionCountry = country,
                  countryVatCorrection = correctionAmount
                )
              )
            )
          ))

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        service.fromUserAnswers(answers, vrn, period, registration.commencementDate) mustEqual Valid(expectedResult)

      }

      "when the user has made corrections for multiple period" in new Fixture {

        private val country1 = arbitrary[Country].sample.value
        private val country2 = arbitrary[Country].sample.value
        private val country3 = arbitrary[Country].sample.value
        private val correctionAmount1 = BigDecimal(1000)
        private val correctionAmount2 = BigDecimal(-544.23)
        private val correctionAmount3 = BigDecimal(145.99)
        private val correctionPeriod1 = Period(2021, Q1)
        private val correctionPeriod2 = Period(2021, Q2)

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value
            .set(CorrectionReturnPeriodPage(index), correctionPeriod1).success.value
            .set(CorrectionCountryPage(index, index), country1).success.value
            .set(CountryVatCorrectionPage(index, index), correctionAmount1).success.value
            .set(CorrectionReturnPeriodPage(index + 1), correctionPeriod2).success.value
            .set(CorrectionCountryPage(index + 1, index), country2).success.value
            .set(CountryVatCorrectionPage(index + 1, index), correctionAmount2).success.value
            .set(CorrectionCountryPage(index + 1, index + 1), country3).success.value
            .set(CountryVatCorrectionPage(index + 1, index + 1), correctionAmount3).success.value

        private val expectedResult = CorrectionRequest(
          vrn = vrn,
          period = period,
          corrections = List(
            PeriodWithCorrections(
              correctionReturnPeriod = correctionPeriod1,
              correctionsToCountry = List(
                CorrectionToCountry(
                  correctionCountry = country1,
                  countryVatCorrection = correctionAmount1
                )
              )
            ),
            PeriodWithCorrections(
              correctionReturnPeriod = correctionPeriod2,
              correctionsToCountry = List(
                CorrectionToCountry(
                  correctionCountry = country2,
                  countryVatCorrection = correctionAmount2
                ),
                CorrectionToCountry(
                  correctionCountry = country3,
                  countryVatCorrection = correctionAmount3
                )
              )
            )
          ))

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        service.fromUserAnswers(answers, vrn, period, registration.commencementDate) mustEqual Valid(expectedResult)

      }

    }

    "must return Invalid" - {

      "when user is expected to make correction and doesn't" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        private val result = service.fromUserAnswers(answers, vrn, period, registration.commencementDate)

        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllCorrectionPeriodsQuery)))
      }

      "when user is expected adds a correction period but doesn't add anything else" in new Fixture {

        private val correctionPeriod = Period(2021, Q1)

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value
            .set(CorrectionReturnPeriodPage(index), correctionPeriod).success.value
            .set(AllCorrectionCountriesQuery(index), List.empty).success.value

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        private val result = service.fromUserAnswers(answers, vrn, period, registration.commencementDate)

        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllCorrectionCountriesQuery(index))))
      }

      "when user is expected adds a correction period and country but not the amount" in new Fixture {

        private val country1 = arbitrary[Country].sample.value
        private val correctionPeriod1 = Period(2021, Q1)

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value
            .set(CorrectionReturnPeriodPage(index), correctionPeriod1).success.value
            .set(CorrectionCountryPage(index, index), country1).success.value

        when(periodService.getReturnPeriods(any())) thenReturn Seq.empty

        private val result = service.fromUserAnswers(answers, vrn, period, registration.commencementDate)

        result mustEqual Invalid(NonEmptyChain(DataMissingError(AllCorrectionPeriodsQuery)))
      }

    }

  }

  ".getCorrectionsForPeriod" - {
    "must return list of corrections" in new Fixture {
      val correctionPayload = arbitrary[CorrectionPayload].sample.value
      when(connector.getForCorrectionPeriod(any())(any())) thenReturn Future.successful(Right(Seq(correctionPayload)))
      service.getCorrectionsForPeriod(period)(ExecutionContext.global, hc).futureValue mustBe correctionPayload.corrections.flatMap(_.correctionsToCountry)
    }

    "must throw if connector returns an error" in new Fixture {
      val correctionPayload = arbitrary[CorrectionPayload].sample.value
      when(connector.getForCorrectionPeriod(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(123, "error")))
      val result = service.getCorrectionsForPeriod(period)(ExecutionContext.global, hc)
      whenReady(result.failed) { exp => exp mustBe a[Exception] }
    }
  }

  trait Fixture {

    protected val periodService: PeriodService = mock[PeriodService]
    protected val connector: CorrectionConnector = mock[CorrectionConnector]
    protected val service = new CorrectionService(periodService, connector)

  }

}

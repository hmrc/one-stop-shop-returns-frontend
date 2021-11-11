package services.corrections

import base.SpecBase
import cats.data.Validated.Valid
import models.requests.corrections.CorrectionRequest
import models.{Country, Period}
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import models.Quarter.{Q1, Q2}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CorrectionReturnSinglePeriodPage, CorrectPreviousReturnPage, CountryVatCorrectionPage}

class CorrectionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  ".fromUserAnswers" - {

    "must return a vat return request" - {

      "when the user has made no corrections" in new Fixture {

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, false).success.value

        private val expectedResult = CorrectionRequest(vrn, period, List.empty)

        service.fromUserAnswers(answers, vrn, period) mustEqual Valid(expectedResult)

      }

      "when the user has made a correction to a single period" in new Fixture {

        private val country = arbitrary[Country].sample.value
        private val correctionAmount = BigDecimal(1000)
        private val correctionPeriod = Period(2021, Q1)

        private val answers =
          emptyUserAnswers
            .set(CorrectPreviousReturnPage, true).success.value
            .set(CorrectionReturnSinglePeriodPage(index), true).success.value
            .set(CorrectionReturnPeriodPage(index), correctionPeriod).success.value
            .set(CorrectionCountryPage(index, index), country).success.value
            .set(CountryVatCorrectionPage(index, index), correctionAmount).success.value

        private val expectedResult = CorrectionRequest(
          vrn = vrn,
          period = period,
          corrections = List(
            PeriodWithCorrections(
              correctionReturnPeriod = correctionPeriod,
              correctionToCountry = List(
                CorrectionToCountry(
                  correctionCountry = country,
                  countryVatCorrection = correctionAmount
                )
              )
            )
          ))

        service.fromUserAnswers(answers, vrn, period) mustEqual Valid(expectedResult)

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
            .set(CorrectionReturnSinglePeriodPage(index), true).success.value
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
              correctionToCountry = List(
                CorrectionToCountry(
                  correctionCountry = country1,
                  countryVatCorrection = correctionAmount1
                )
              )
            ),
            PeriodWithCorrections(
              correctionReturnPeriod = correctionPeriod2,
              correctionToCountry = List(
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

        service.fromUserAnswers(answers, vrn, period) mustEqual Valid(expectedResult)

      }

    }

  }

  trait Fixture {

    protected val service = new CorrectionService()

  }

}

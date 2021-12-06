package utils

import base.SpecBase
import models.VatOnSalesChoice.Standard
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections}
import models.domain.{SalesDetails, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.{Country, Period, VatOnSales}
import models.Quarter.{Q3, Q4}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.domain.Vrn

import java.time.Instant

class CorrectionUtilsSpec extends SpecBase {

  "groupByCountry" - {

    "should add multiple countries with corrections and vat return" in {

      val country1 = arbitrary[Country].sample.value

      val correctionPayload = CorrectionPayload(
        vrn = completeVatReturn.vrn,
        period = completeVatReturn.period,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10))
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = completeVatReturn.copy(
        salesFromNi = List(SalesToCountry(country1,
          List(SalesDetails(DomainVatRate(10.00,
            DomainVatRateType.Reduced),
            1000.00,
            VatOnSales(Standard, 100.00))))),
        salesFromEu = List.empty
      )

      CorrectionUtils.groupByCountryAndSum(correctionPayload, vatReturn) mustBe Map(country1 -> BigDecimal(120))
    }

    "should add multiple countries with negative corrections" in {

      val country1 = arbitrary[Country].sample.value
      val country2 = arbitrary[Country].suchThat(_ != country1).sample.value

      val correctionPayload = CorrectionPayload(
        vrn = arbitrary[Vrn].sample.value,
        period = arbitrary[Period].sample.value,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(10)),
              CorrectionToCountry(country2, BigDecimal(-10))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(-10)),
              CorrectionToCountry(country2, BigDecimal(10))
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = completeVatReturn.copy(
        salesFromNi = List(SalesToCountry(country1,
          List(SalesDetails(DomainVatRate(10.00,
            DomainVatRateType.Reduced),
            1000.00,
            VatOnSales(Standard, 100.00))))),
        salesFromEu = List.empty
      )

      CorrectionUtils.groupByCountryAndSum(correctionPayload, vatReturn) must contain.theSameElementsAs(
        Map(
          country1 -> BigDecimal(100),
          country2 -> BigDecimal(0)
        )
      )
    }

    "should have nil return with with a mix of corrections" in {

      val country1 = arbitrary[Country].sample.value
      val country2 = arbitrary[Country].suchThat(_ != country1).sample.value

      val correctionPayload = CorrectionPayload(
        vrn = arbitrary[Vrn].sample.value,
        period = arbitrary[Period].sample.value,
        corrections = List(
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q3),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(52.44)),
              CorrectionToCountry(country2, BigDecimal(-589.24))
            )),
          PeriodWithCorrections(
            correctionReturnPeriod = Period(2021, Q4),
            correctionsToCountry = List(
              CorrectionToCountry(country1, BigDecimal(-10)),
            ))
        ),
        submissionReceived = Instant.now(),
        lastUpdated = Instant.now()
      )

      val vatReturn = emptyVatReturn

      CorrectionUtils.groupByCountryAndSum(correctionPayload, vatReturn) must contain.theSameElementsAs(
        Map(
          country1 -> BigDecimal(42.44),
          country2 -> BigDecimal(-589.24)
        )
      )
    }

  }

}

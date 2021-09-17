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

package generators

import models.VatOnSalesChoice.Standard
import models.{domain, _}
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType, SalesDetails, SalesFromEuCountry, SalesToCountry, VatReturn, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.registration._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.math.BigDecimal.RoundingMode

trait ModelGenerators {

  implicit val arbitraryVatOnSales: Arbitrary[VatOnSales] =
    Arbitrary {
      for {
        choice <- Gen.oneOf(VatOnSalesChoice.values)
        amount <- arbitrary[BigDecimal]
      } yield VatOnSales(choice, amount)
    }

  implicit val arbitraryQuarter: Arbitrary[Quarter] =
    Arbitrary {
      Gen.oneOf(Quarter.values)
    }

  private def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  implicit def arbitraryVatRate: Arbitrary[VatRate] =
    Arbitrary {
      for {
        rate <- Gen.choose[BigDecimal](BigDecimal(0.1), BigDecimal(100))
        rateType <- Gen.oneOf(VatRateType.values)
        validFrom <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2100, 1, 1))
      } yield VatRate(rate, rateType, validFrom)
    }

  implicit val arbitraryPeriod: Arbitrary[Period] =
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099)
        quarter <- Gen.oneOf(Quarter.values)
      } yield Period(year, quarter)
  }

  implicit def arbitraryVrn: Arbitrary[Vrn] = Arbitrary {
    Gen.listOfN(9, Gen.numChar).map(_.mkString).map(Vrn(_))
  }

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit lazy val arbitraryInternationalAddress: Arbitrary[InternationalAddress] =
    Arbitrary {
      for {
        line1         <- arbitrary[String]
        line2         <- Gen.option(arbitrary[String])
        townOrCity    <- arbitrary[String]
        stateOrRegion <- Gen.option(arbitrary[String])
        postCode      <- Gen.option(arbitrary[String])
        country       <- arbitrary[Country]
      } yield InternationalAddress(line1, line2, townOrCity, stateOrRegion, postCode, country)
    }

  implicit lazy val arbitraryUkAddress: Arbitrary[UkAddress] =
    Arbitrary {
      for {
        line1      <- arbitrary[String]
        line2      <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        county     <- Gen.option(arbitrary[String])
        postCode   <- arbitrary[String]
      } yield UkAddress(line1, line2, townOrCity, county, postCode)
    }

  implicit val arbitraryAddress: Arbitrary[Address] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[UkAddress],
        arbitrary[InternationalAddress],
        arbitrary[DesAddress]
      )
    }

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1       <- arbitrary[String]
        line2       <- Gen.option(arbitrary[String])
        line3       <- Gen.option(arbitrary[String])
        line4       <- Gen.option(arbitrary[String])
        line5       <- Gen.option(arbitrary[String])
        postCode    <- Gen.option(arbitrary[String])
        countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
      } yield DesAddress(line1, line2, line3, line4, line5, postCode, countryCode)
    }

  implicit lazy val arbitraryFixedEstablishment: Arbitrary[FixedEstablishment] =
    Arbitrary {
      for {
        tradingName <- arbitrary[String]
        address     <- arbitrary[InternationalAddress]
      } yield FixedEstablishment(tradingName, address)
    }

  implicit val arbitraryEuTaxIdentifierType: Arbitrary[EuTaxIdentifierType] =
    Arbitrary {
      Gen.oneOf(EuTaxIdentifierType.values)
    }

  implicit lazy val arbitraryContactDetails: Arbitrary[ContactDetails] =
    Arbitrary {
      for {
        fullName        <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress    <- arbitrary[String]
      } yield ContactDetails(fullName, telephoneNumber, emailAddress)
    }

  implicit val arbitraryEuTaxIdentifier: Arbitrary[EuTaxIdentifier] =
    Arbitrary {
      for {
        identifierType <- arbitrary[EuTaxIdentifierType]
        value          <- arbitrary[Int].map(_.toString)
      } yield domain.EuTaxIdentifier(identifierType, value)
    }

  implicit val arbitraryVatDetails: Arbitrary[VatDetails] =
    Arbitrary {
      for {
        address  <- arbitrary[Address]
        source   <- Gen.oneOf(VatDetailSource.values)
        vatGroup <- arbitrary[Boolean]
        date     <- datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2021, 1, 1))
      } yield VatDetails(date, address, vatGroup, source)
    }


  implicit val arbitraryRegistration: Arbitrary[Registration] =
    Arbitrary {
      for {
        vrn               <- arbitrary[Vrn]
        name              <- arbitrary[String]
        vatDetails        <- arbitrary[VatDetails]
        contactDetails    <- arbitrary[ContactDetails]
        commencementDate  <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        isOnlineMarketplace <- arbitrary[Boolean]
      } yield Registration(vrn, name, vatDetails, Nil, contactDetails, commencementDate, isOnlineMarketplace)
    }

  implicit val arbitraryDomainVatRate: Arbitrary[DomainVatRate] =
    Arbitrary {
      for {
        vatRateType <- Gen.oneOf(DomainVatRateType.values)
        rate        <- Gen.choose(BigDecimal(0), BigDecimal(100))
      } yield DomainVatRate(rate.setScale(2, RoundingMode.HALF_EVEN), vatRateType)
    }

  implicit val arbitrarySalesDetails: Arbitrary[SalesDetails] =
    Arbitrary {
      for {
        vatRate       <- arbitrary[DomainVatRate]
        taxableAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
        vatAmount     <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
      } yield SalesDetails(
        vatRate,
        taxableAmount.setScale(2, RoundingMode.HALF_EVEN),
        VatOnSales(Standard, vatAmount.setScale(2, RoundingMode.HALF_EVEN))
      )
    }

  implicit val arbitrarySalesToCountry: Arbitrary[SalesToCountry] =
    Arbitrary {
      for {
        country <- arbitrary[Country]
        number  <- Gen.choose(1, 2)
        amounts <- Gen.listOfN(number, arbitrary[SalesDetails])
      } yield SalesToCountry(country, amounts)
    }

  implicit val arbitrarySalesFromEuCountry: Arbitrary[SalesFromEuCountry] =
    Arbitrary {
      for {
        country       <- arbitrary[Country]
        taxIdentifier <- Gen.option(arbitrary[EuTaxIdentifier])
        number        <- Gen.choose(1, 3)
        amounts       <- Gen.listOfN(number, arbitrary[SalesToCountry])
      } yield SalesFromEuCountry(country, taxIdentifier, amounts)
    }

  implicit val arbitraryVatReturn: Arbitrary[VatReturn] =
    Arbitrary {
      for {
        vrn         <- arbitrary[Vrn]
        period      <- arbitrary[Period]
        niSales     <- Gen.choose(1, 3)
        euSales     <- Gen.choose(1, 3)
        salesFromNi <- Gen.listOfN(niSales, arbitrary[SalesToCountry])
        salesFromEu <- Gen.listOfN(euSales, arbitrary[SalesFromEuCountry])
        now         = Instant.now
      } yield VatReturn(vrn, period, ReturnReference(vrn, period), PaymentReference(vrn, period), None, None, salesFromNi, salesFromEu, now, now)
    }

  implicit val arbitraryReturnReference: Arbitrary[ReturnReference] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        period <- arbitrary[Period]
      } yield ReturnReference(vrn, period)
    }
}

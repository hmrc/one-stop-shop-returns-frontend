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

import models._
import models.registration._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate, ZoneOffset}

trait ModelGenerators {

  implicit lazy val arbitrarySalesDetailsFromEu: Arbitrary[SalesDetailsFromEu] =
    Arbitrary {
      for {
        netValueOfSales <- arbitrary[String]
        vatOnSales <- arbitrary[String]
      } yield SalesDetailsFromEu(netValueOfSales, vatOnSales)
    }

  implicit lazy val arbitraryCountryOfEstablishmentFromEu: Arbitrary[CountryOfEstablishmentFromEu] =
    Arbitrary {
      Gen.oneOf(CountryOfEstablishmentFromEu.values.toSeq)
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
        year <- Gen.choose(2022, 2100)
        quarter <- Gen.oneOf(Quarter.values)
      } yield Period(year, quarter)
  }

  implicit def arbitraryVrn: Arbitrary[Vrn] = Arbitrary {
    for {
      prefix <- Gen.oneOf("", "GB")
      chars  <- Gen.listOfN(9, Gen.numChar)
    } yield {
      Vrn(prefix + chars.mkString(""))
    }
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
      } yield EuTaxIdentifier(identifierType, value)
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
        commencementDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
      } yield Registration(vrn, name, vatDetails, Nil, contactDetails, commencementDate)
    }
}

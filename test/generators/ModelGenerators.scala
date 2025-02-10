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

package generators

import connectors.SavedUserAnswers
import models.Period.fromEtmpPeriodKey
import models.VatOnSalesChoice.Standard
import models.corrections.{CorrectionPayload, CorrectionToCountry, PeriodWithCorrections, ReturnCorrectionValue}
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType, SalesDetails, SalesFromEuCountry, SalesToCountry, VatReturn, VatRate as DomainVatRate, VatRateType as DomainVatRateType}
import models.etmp.*
import models.financialdata.Charge
import models.registration.*
import models.{domain, *}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.Vrn
import viewmodels.yourAccount.{CurrentReturns, Return}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import scala.math.BigDecimal.RoundingMode

trait ModelGenerators {

  implicit lazy val arbitraryContinueReturn: Arbitrary[ContinueReturn] =
    Arbitrary {
      Gen.oneOf(ContinueReturn.values.toSeq)
    }

  implicit val arbitraryBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary {
      for {
        nonDecimalNumber <- arbitrary[Int].retryUntil(_ > 0)
        decimalNumber <- arbitrary[Int].retryUntil(_ > 0).retryUntil(_.toString.reverse.head.toString != "0")
      } yield BigDecimal(s"$nonDecimalNumber.$decimalNumber")
    }

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
        rate <- Gen.choose[BigDecimal](BigDecimal(1), BigDecimal(100))
        rateType <- Gen.oneOf(VatRateType.values)
        validFrom <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2100, 1, 1))
      } yield VatRate(rate.setScale(2, RoundingMode.HALF_EVEN), rateType, validFrom)
    }

  implicit val arbitraryEuVatRate: Arbitrary[EuVatRate] = {
    Arbitrary {
      for {
        country <- arbitrary[Country]
        vatRate <- arbitrary[BigDecimal]
        vatRateType <- Gen.oneOf(VatRateType.values)
        situatedOn <- arbitrary[LocalDate]
      } yield EuVatRate(
        country = country,
        vatRate = vatRate,
        vatRateType = vatRateType,
        situatedOn = situatedOn
      )
    }
  }

  implicit val arbitraryStandardPeriod: Arbitrary[StandardPeriod] =
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099)
        quarter <- Gen.oneOf(Quarter.values)
      } yield StandardPeriod(year, quarter)
    }

  implicit val arbitraryPeriod: Arbitrary[Period] =
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099)
        quarter <- Gen.oneOf(Quarter.values)
      } yield StandardPeriod(year, quarter)
    }

  implicit val arbitraryPeriodWithStatus: Arbitrary[PeriodWithStatus] =
    Arbitrary {
      for {
        period <- arbitrary[StandardPeriod]
        status <- Gen.oneOf(SubmissionStatus.values)
      } yield PeriodWithStatus(period, status)
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
        line1 <- arbitrary[String]
        line2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        stateOrRegion <- Gen.option(arbitrary[String])
        postCode <- Gen.option(arbitrary[String])
        country <- arbitrary[Country]
      } yield InternationalAddress(line1, line2, townOrCity, stateOrRegion, postCode, country)
    }

  implicit lazy val arbitraryUkAddress: Arbitrary[UkAddress] =
    Arbitrary {
      for {
        line1 <- arbitrary[String]
        line2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        county <- Gen.option(arbitrary[String])
        postCode <- arbitrary[String]
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
        line1 <- arbitrary[String]
        line2 <- Gen.option(arbitrary[String])
        line3 <- Gen.option(arbitrary[String])
        line4 <- Gen.option(arbitrary[String])
        line5 <- Gen.option(arbitrary[String])
        postCode <- Gen.option(arbitrary[String])
        countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
      } yield DesAddress(line1, line2, line3, line4, line5, postCode, countryCode)
    }

  implicit lazy val arbitraryFixedEstablishment: Arbitrary[TradeDetails] =
    Arbitrary {
      for {
        tradingName <- arbitrary[String]
        address <- arbitrary[InternationalAddress]
      } yield TradeDetails(tradingName, address)
    }

  implicit val arbitraryEuTaxIdentifierType: Arbitrary[EuTaxIdentifierType] =
    Arbitrary {
      Gen.oneOf(EuTaxIdentifierType.values)
    }

  implicit lazy val arbitraryContactDetails: Arbitrary[ContactDetails] =
    Arbitrary {
      for {
        fullName <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress <- arbitrary[String]
      } yield ContactDetails(fullName, telephoneNumber, emailAddress)
    }

  implicit val arbitraryEuTaxIdentifier: Arbitrary[EuTaxIdentifier] =
    Arbitrary {
      for {
        identifierType <- arbitrary[EuTaxIdentifierType]
        value <- arbitrary[Int].map(_.toString)
      } yield domain.EuTaxIdentifier(identifierType, value)
    }

  implicit val arbitraryVatDetails: Arbitrary[VatDetails] =
    Arbitrary {
      for {
        address <- arbitrary[Address]
        source <- Gen.oneOf(VatDetailSource.values)
        vatGroup <- arbitrary[Boolean]
        date <- datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2021, 1, 1))
      } yield VatDetails(date, address, vatGroup, source)
    }


  implicit val arbitraryRegistration: Arbitrary[Registration] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        name <- arbitrary[String]
        vatDetails <- arbitrary[VatDetails]
        contactDetails <- arbitrary[ContactDetails]
        commencementDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        isOnlineMarketplace <- arbitrary[Boolean]
      } yield Registration(vrn, name, vatDetails, Nil, contactDetails, commencementDate, isOnlineMarketplace, None, None)
    }

  implicit val arbitraryDomainVatRate: Arbitrary[DomainVatRate] =
    Arbitrary {
      for {
        vatRateType <- Gen.oneOf(DomainVatRateType.values)
        rate <- Gen.choose(BigDecimal(0), BigDecimal(100))
      } yield DomainVatRate(rate.setScale(2, RoundingMode.HALF_EVEN), vatRateType)
    }

  implicit val arbitrarySalesDetails: Arbitrary[SalesDetails] =
    Arbitrary {
      for {
        vatRate <- arbitrary[DomainVatRate]
        taxableAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
        vatAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
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
        number <- Gen.choose(1, 2)
        amounts <- Gen.listOfN(number, arbitrary[SalesDetails])
      } yield SalesToCountry(country, amounts)
    }

  implicit val arbitrarySalesFromEuCountry: Arbitrary[SalesFromEuCountry] =
    Arbitrary {
      for {
        country <- arbitrary[Country]
        taxIdentifier <- Gen.option(arbitrary[EuTaxIdentifier])
        number <- Gen.choose(1, 3)
        amounts <- Gen.listOfN(number, arbitrary[SalesToCountry])
      } yield SalesFromEuCountry(country, taxIdentifier, amounts)
    }

  implicit val arbitraryVatReturn: Arbitrary[VatReturn] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        period <- arbitrary[StandardPeriod]
        niSales <- Gen.choose(1, 3)
        euSales <- Gen.choose(1, 3)
        salesFromNi <- Gen.listOfN(niSales, arbitrary[SalesToCountry])
        salesFromEu <- Gen.listOfN(euSales, arbitrary[SalesFromEuCountry])
        now = Instant.now
      } yield VatReturn(vrn, period, ReturnReference(vrn, period), PaymentReference(vrn, period), None, None, salesFromNi, salesFromEu, now, now)
    }

  implicit val arbitraryCharge: Arbitrary[Charge] =
    Arbitrary {
      for {
        period <- arbitrary[StandardPeriod]
        originalAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
        outstandingAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
        clearedAmount <- Gen.choose(BigDecimal(0), BigDecimal(1000000))
      } yield Charge(
        period,
        originalAmount.setScale(2, RoundingMode.HALF_EVEN),
        outstandingAmount.setScale(2, RoundingMode.HALF_EVEN),
        clearedAmount.setScale(2, RoundingMode.HALF_EVEN)
      )
    }

  implicit val arbitraryReturnReference: Arbitrary[ReturnReference] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        period <- arbitrary[StandardPeriod]
      } yield ReturnReference(vrn, period)
    }

  implicit val arbitraryCorrectionToCountry: Arbitrary[CorrectionToCountry] =
    Arbitrary {
      for {
        country <- arbitrary[Country]
        countryVatCorrection <- Gen.choose(BigDecimal(1), BigDecimal(999999))
      } yield CorrectionToCountry(country, Some(countryVatCorrection.setScale(2, RoundingMode.HALF_UP)))
    }

  implicit val arbitraryCorrection: Arbitrary[PeriodWithCorrections] =
    Arbitrary {
      for {
        correctionPeriod <- arbitrary[StandardPeriod]
        amount <- Gen.choose(1, 30)
        correctionsToCountry <- Gen.listOfN(amount, arbitrary[CorrectionToCountry])
      } yield PeriodWithCorrections(correctionPeriod, Some(correctionsToCountry))
    }

  implicit val arbitraryCorrectionPayload: Arbitrary[CorrectionPayload] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        period <- arbitrary[StandardPeriod]
        amount <- Gen.choose(1, 30)
        corrections <- Gen.listOfN(amount, arbitrary[PeriodWithCorrections])
        now = Instant.now
      } yield CorrectionPayload(vrn, period, corrections, now, now)
    }

  implicit val arbitrarySavedUserAnswers: Arbitrary[SavedUserAnswers] =
    Arbitrary {
      for {
        vrn <- arbitrary[Vrn]
        period <- arbitrary[StandardPeriod]
        data = JsObject(Seq("test" -> Json.toJson("test")))
        now = Instant.now
      } yield SavedUserAnswers(vrn, period, data, now)
    }

  implicit val arbitraryVatCustomerInfo: Arbitrary[VatCustomerInfo] =
    Arbitrary {
      for {
        address <- arbitrary[Address]
        registrationDate <- arbitrary[LocalDate]
        partOfVatGroup <- arbitrary[Boolean]
        organisationName <- Gen.option(arbitrary[String])
        individualName <- Gen.option(arbitrary[String])
        singleMarketIndicator <- Gen.option(arbitrary[Boolean])
        deregistrationDecisionDate <- Gen.option(arbitrary[LocalDate])

      } yield VatCustomerInfo(address, registrationDate, partOfVatGroup, organisationName, individualName, singleMarketIndicator, deregistrationDecisionDate)
    }

  implicit val arbitraryReturnCorrectionValue: Arbitrary[ReturnCorrectionValue] =
    Arbitrary {
      for {
        correctionValue <- arbitrary[BigDecimal]
      } yield ReturnCorrectionValue(correctionValue)
    }

  implicit val arbitraryObligationDetails: Arbitrary[EtmpObligationDetails] =
    Arbitrary {
      for {
        status <- Gen.oneOf(EtmpObligationsFulfilmentStatus.values)
        periodKey <- arbitraryPeriod.arbitrary
      } yield EtmpObligationDetails(
        status = status,
        periodKey = periodKey.toString
      )
    }

  implicit val arbitraryObligations: Arbitrary[EtmpObligations] =
    Arbitrary {
      for {
        obligationDetails <- Gen.listOfN(3, arbitrary[EtmpObligationDetails])
      } yield {
        EtmpObligations(Seq(EtmpObligation(
          obligationDetails = obligationDetails
        )))
      }
    }

  implicit lazy val arbitraryEtmpVatReturnGoodsSupplied: Arbitrary[EtmpVatReturnGoodsSupplied] = {
    Arbitrary {
      for {
        msOfConsumption <- arbitraryCountry.arbitrary.map(_.code)
        msOfEstablishment <- arbitraryCountry.arbitrary.map(_.code)
        vatRateType <- Gen.oneOf(EtmpVatRateType.values)
        taxableAmountGBP <- arbitrary[BigDecimal]
        vatAmountGBP <- arbitrary[BigDecimal]
      } yield EtmpVatReturnGoodsSupplied(
        msOfConsumption = msOfConsumption,
        msOfEstablishment = msOfEstablishment,
        vatRateType = vatRateType,
        taxableAmountGBP = taxableAmountGBP,
        vatAmountGBP = vatAmountGBP
      )
    }
  }

  implicit lazy val arbitraryPeriodKey: Arbitrary[String] = {
    Arbitrary {
      for {
        year <- Gen.oneOf(21, 99)
        quarter <- arbitraryQuarter.arbitrary
      } yield s"${year}$quarter"
    }
  }

  implicit lazy val arbitraryEtmpVatReturnCorrection: Arbitrary[EtmpVatReturnCorrection] =
    Arbitrary {
      for {
        periodKey <- arbitraryPeriodKey.arbitrary
        periodFrom <- arbitrary[String]
        periodTo <- arbitrary[String]
        msOfConsumption <- arbitraryCountry.arbitrary.map(_.code)
        totalVATAmountCorrectionGBP <- arbitrary[BigDecimal]
        totalVATAmountCorrectionEUR <- arbitrary[BigDecimal]
      } yield EtmpVatReturnCorrection(
        periodKey = periodKey,
        periodFrom = periodFrom,
        periodTo = periodTo,
        msOfConsumption = msOfConsumption,
        totalVATAmountCorrectionGBP = totalVATAmountCorrectionGBP,
        totalVATAmountCorrectionEUR = totalVATAmountCorrectionEUR
      )
    }

  implicit lazy val arbitraryEtmpVatReturnBalanceOfVatDue: Arbitrary[EtmpVatReturnBalanceOfVatDue] =
    Arbitrary {
      for {
        msOfConsumption <- arbitraryCountry.arbitrary.map(_.code)
        totalVATDueGBP <- arbitrary[BigDecimal]
        totalVATEUR <- arbitrary[BigDecimal]
      } yield EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = msOfConsumption,
        totalVATDueGBP = totalVATDueGBP,
        totalVATEUR = totalVATEUR
      )
    }


  implicit lazy val arbitraryEtmpVatReturn: Arbitrary[EtmpVatReturn] = {
    Arbitrary {
      for {
        returnReference <- arbitrary[String]
        returnVersion <- arbitrary[LocalDateTime]
        periodKey <- arbitraryPeriodKey.arbitrary
        amountOfGoodsSupplied <- Gen.oneOf(List(1, 2, 3))
        goodsSupplied <- Gen.listOfN(amountOfGoodsSupplied, arbitrary[EtmpVatReturnGoodsSupplied])
        totalVATGoodsSuppliedGBP <- arbitrary[BigDecimal]
        totalVATAmountPayable <- arbitrary[BigDecimal]
        totalVATAmountPayableAllSpplied <- arbitrary[BigDecimal]
        amountOfCorrections <- Gen.oneOf(List(1, 2, 3))
        correctionPreviousVATReturn <- Gen.listOfN(amountOfCorrections, arbitrary[EtmpVatReturnCorrection])
        totalVATAmountFromCorrectionGBP <- arbitrary[BigDecimal]
        amountOfBalanceOfVATDueForMS <- Gen.oneOf(List(1, 2, 3))
        balanceOfVATDueForMS <- Gen.listOfN(amountOfBalanceOfVATDueForMS, arbitrary[EtmpVatReturnBalanceOfVatDue])
        totalVATAmountDueForAllMSGBP <- arbitrary[BigDecimal]
        paymentReference <- arbitrary[String]
      } yield EtmpVatReturn(
        returnReference = returnReference,
        returnVersion = returnVersion,
        periodKey = periodKey,
        returnPeriodFrom = fromEtmpPeriodKey(periodKey).firstDay,
        returnPeriodTo = fromEtmpPeriodKey(periodKey).lastDay,
        goodsSupplied = goodsSupplied,
        totalVATGoodsSuppliedGBP = totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = balanceOfVATDueForMS,
        totalVATAmountDueForAllMSGBP = totalVATAmountDueForAllMSGBP,
        paymentReference = paymentReference
      )
    }
  }

  implicit lazy val arbitraryPartialReturnPeriod: Arbitrary[PartialReturnPeriod] = {
    Arbitrary {
      for {
        period <- arbitraryPeriod.arbitrary
      } yield PartialReturnPeriod(
        firstDay = period.firstDay.withDayOfMonth(9),
        lastDay = period.lastDay,
        year = period.year,
        quarter = period.quarter
      )
    }
  }

  implicit lazy val arbitraryReturn: Arbitrary[Return] = {
    Arbitrary {
      for {
        period <- arbitrary[StandardPeriod]
        submissionStatus <- Gen.oneOf(SubmissionStatus.values)
        inProgress <- arbitrary[Boolean]
        isOldest <- arbitrary[Boolean]
      } yield {
        Return(
          period = period,
          firstDay = period.firstDay,
          lastDay = period.lastDay,
          dueDate = period.paymentDeadline,
          submissionStatus = submissionStatus,
          inProgress = inProgress,
          isOldest = isOldest
        )
      }
    }
  }

  implicit lazy val arbitraryCurrentReturns: Arbitrary[CurrentReturns] = {
    Arbitrary {
      for {
        returns <- Gen.listOfN(3, arbitraryReturn.arbitrary)
        excluded <- arbitrary[Boolean]
        excludedReturns <- Gen.listOfN(1, arbitraryReturn.arbitrary)
      } yield {
        CurrentReturns(
          returns = returns,
          excluded = excluded,
          excludedReturns = excludedReturns
        )
      }
    }
  }

}

  

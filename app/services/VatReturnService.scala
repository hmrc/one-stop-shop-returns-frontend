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

import cats.implicits._
import connectors.VatReturnConnector
import models._
import models.domain.EuTaxIdentifierType.Vat
import models.domain.{EuTaxIdentifier, SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.registration.{EuVatRegistration, Registration, RegistrationWithFixedEstablishment}
import models.requests.VatReturnRequest
import pages._
import play.api.i18n.Lang.logger
import queries._
import services.corrections.CorrectionService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait VatReturnService {
  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period, registration: Registration): ValidationResult[VatReturnRequest] =
    (
      getSalesFromNi(answers),
      getSalesFromEu(answers, registration)
    ).mapN(
      (salesFromNi, salesFromEu) =>
        VatReturnRequest(vrn, StandardPeriod.fromPeriod(period), Some(period.firstDay), Some(period.lastDay), salesFromNi, salesFromEu)
    )

  def getLatestVatAmountForPeriodAndCountry(country: Country, period: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[BigDecimal]


  private def getSalesFromNi(answers: UserAnswers): ValidationResult[List[SalesToCountry]] =
    answers.get(SoldGoodsFromNiPage) match {
      case Some(true) =>
        processSalesFromNi(answers)

      case Some(false) =>
        List.empty[SalesToCountry].validNec

      case None =>
        DataMissingError(SoldGoodsFromNiPage).invalidNec
    }

  private def processSalesFromNi(answers: UserAnswers): ValidationResult[List[SalesToCountry]] = {
    answers.get(AllSalesFromNiQuery) match {
      case Some(salesFromNi) if salesFromNi.nonEmpty =>
        salesFromNi.zipWithIndex.map {
          case (_, index) =>
            processSalesFromNiToCountry(answers, Index(index))
        }.sequence.map {
          salesDetails =>
            salesFromNi.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesToCountry(sales.countryOfConsumption, salesDetails)
            }
        }

      case _ =>
        DataMissingError(AllSalesFromNiQuery).invalidNec
    }
  }

  private def processSalesFromNiToCountry(answers: UserAnswers, countryIndex: Index): ValidationResult[List[SalesDetails]] =
    answers.get(VatRatesFromNiPage(countryIndex)) match {
      case Some(list) if list.nonEmpty =>
        list.zipWithIndex.map {
          case (vatRate, index) =>
            processSalesFromNiAtVatRate(answers, countryIndex, Index(index), vatRate)
        }.sequence

      case _ =>
        DataMissingError(VatRatesFromNiPage(countryIndex)).invalidNec
    }

  private def processSalesFromNiAtVatRate(answers: UserAnswers, countryIndex: Index, vatRateIndex: Index, vatRate: VatRate): ValidationResult[SalesDetails] =
    answers.get(NiSalesAtVatRateWithOptionalVatQuery(countryIndex, vatRateIndex)) match {
      case Some(SalesAtVatRateWithOptionalVat(netValueOfSales, Some(vatOnSales))) =>
        SalesDetails(
          vatRate = toDomainVatRate(vatRate),
          netValueOfSales = netValueOfSales,
          vatOnSales = vatOnSales
        ).validNec
      case Some(SalesAtVatRateWithOptionalVat(_, None)) =>
        DataMissingError(VatOnSalesFromNiQuery(countryIndex, vatRateIndex)).invalidNec
      case None =>
        DataMissingError(NiSalesAtVatRateQuery(countryIndex, vatRateIndex)).invalidNec

    }

  private def getSalesFromEu(answers: UserAnswers, registration: Registration): ValidationResult[List[SalesFromEuCountry]] =
    answers.get(SoldGoodsFromEuPage) match {
      case Some(true) =>
        processSalesFromEu(answers, registration)

      case Some(false) =>
        List.empty[SalesFromEuCountry].validNec

      case None =>
        DataMissingError(SoldGoodsFromEuPage).invalidNec
    }

  private def processSalesFromEu(answers: UserAnswers, registration: Registration): ValidationResult[List[SalesFromEuCountry]] =
    answers.get(AllSalesFromEuQueryWithOptionalVatQuery) match {
      case Some(salesFromEu) if salesFromEu.nonEmpty =>
        salesFromEu.zipWithIndex.map {
          case (_, index) =>
            processSalesFromEuCountry(answers, Index(index))
        }.sequence.map {
          salesDetails =>
            salesFromEu.zip(salesDetails).map {
              case (sales, salesDetails) =>
                val taxIdentifier = registration.euRegistrations.find(_.country == sales.countryOfSale) match {
                  case Some(r: EuVatRegistration) => Some(EuTaxIdentifier(Vat, r.vatNumber))
                  case Some(r: RegistrationWithFixedEstablishment) => Some(r.taxIdentifier)
                  case _ => None
                }
                SalesFromEuCountry(sales.countryOfSale, taxIdentifier, salesDetails)
            }
        }

      case _ =>
        DataMissingError(AllSalesFromEuQuery).invalidNec
    }

  private def processSalesFromEuCountry(answers: UserAnswers, countryFromIndex: Index): ValidationResult[List[SalesToCountry]] =
    answers.get(AllSalesToEuQuery(countryFromIndex)) match {
      case Some(salesToEu) if salesToEu.nonEmpty =>
        salesToEu.zipWithIndex.map {
          case (_, index) =>
            processSalesToEuCountry(answers, countryFromIndex, Index(index))
        }.sequence.map {
          salesDetails =>
            salesToEu.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesToCountry(sales.countryOfConsumption, salesDetails)
            }
        }

      case _ =>
        DataMissingError(AllSalesToEuQuery(countryFromIndex)).invalidNec
    }

  private def processSalesToEuCountry(answers: UserAnswers, countryFromIndex: Index, countryToIndex: Index): ValidationResult[List[SalesDetails]] =
    answers.get(VatRatesFromEuPage(countryFromIndex, countryToIndex)) match {
      case Some(list) if list.nonEmpty =>
        list.zipWithIndex.map {
          case (vatRate, index) =>
            processSalesFromEuAtVatRate(answers, countryFromIndex, countryToIndex, Index(index), vatRate)
        }.sequence

      case _ =>
        DataMissingError(VatRatesFromEuPage(countryFromIndex, countryToIndex)).invalidNec
    }

  private def processSalesFromEuAtVatRate(
                                           answers: UserAnswers,
                                           countryFromIndex: Index,
                                           countryToIndex: Index,
                                           vatRateIndex: Index,
                                           vatRate: VatRate
                                         ): ValidationResult[SalesDetails] =
    answers.get(EuSalesAtVatRateWithOptionalVatQuery(countryFromIndex, countryToIndex, vatRateIndex)) match {
      case Some(SalesAtVatRateWithOptionalVat(netValueOfSales, Some(vatOnSales))) =>
        SalesDetails(
          vatRate = toDomainVatRate(vatRate),
          netValueOfSales = netValueOfSales,
          vatOnSales = vatOnSales
        ).validNec
      case Some(SalesAtVatRateWithOptionalVat(_, None)) =>
        DataMissingError(VatOnSalesFromEuQuery(countryFromIndex, countryToIndex, vatRateIndex)).invalidNec
      case _ =>
        DataMissingError(EuSalesAtVatRateQuery(countryFromIndex, countryToIndex, vatRateIndex)).invalidNec
    }

  private def toDomainVatRate(vatRate: VatRate): DomainVatRate = {
    DomainVatRate(
      vatRate.rate,
      if (vatRate.rateType == VatRateType.Reduced) {
        DomainVatRateType.Reduced
      } else {
        DomainVatRateType.Standard
      }
    )
  }
}

class VatReturnServiceRepoImpl @Inject()(connector: VatReturnConnector, correctionService: CorrectionService) extends VatReturnService {

  private def getVatOwedToCountryOnReturn(country: Country, period: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[BigDecimal] = {
    connector.get(period).map {
      case Right(vatReturn) =>
        val sumFromNiToSelectedCountry = vatReturn.salesFromNi.filter(_.countryOfConsumption.code == country.code)
          .flatMap(sales => sales.amounts.map(_.vatOnSales.amount)).sum
        val salesFromEU = vatReturn.salesFromEu.flatMap(_.sales)
        val sumFromEUToSelectedCountry = salesFromEU.filter(_.countryOfConsumption.code == country.code)
          .flatMap(sales => sales.amounts.map(_.vatOnSales.amount)).sum
        val vatOwedToCountryOnPrevReturn = sumFromEUToSelectedCountry + sumFromNiToSelectedCountry
        vatOwedToCountryOnPrevReturn
      case Left(value) =>
        logger.error(s"there was an error getting the vat return: $value")
        throw new Exception(value.toString)
    }
  }

  override def getLatestVatAmountForPeriodAndCountry(country: Country, period: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[BigDecimal] =
    for {
      vatOwedToCountryOnPrevReturn <- getVatOwedToCountryOnReturn(country, period)
      correctionsForPeriod <- correctionService.getCorrectionsForPeriod(period)
    } yield {
      val correctionsToCountry = correctionsForPeriod.filter(_.correctionCountry == country).map {
        _.countryVatCorrection.getOrElse(BigDecimal(0))
      }.sum
      vatOwedToCountryOnPrevReturn + correctionsToCountry
    }

}

class VatReturnServiceEtmpImpl @Inject()(correctionService: CorrectionService) extends VatReturnService {

  override def getLatestVatAmountForPeriodAndCountry(country: Country, period: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[BigDecimal] =
    for {
      correctionsForPeriod <- correctionService.getReturnCorrectionValue(country, period)
    } yield correctionsForPeriod.maximumCorrectionValue

}
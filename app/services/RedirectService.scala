/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.Validated.Invalid
import controllers._
import controllers.actions.AuthenticatedControllerComponents
import controllers.corrections.{routes => correctionsRoutes}
import logging.Logging
import models.{CheckMode, DataMissingError, Index, Period, ValidationError}
import models.requests.DataRequest
import pages.corrections.CorrectPreviousReturnPage
import pages.{VatRatesFromEuPage, VatRatesFromNiPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents}
import queries._
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import services.corrections.CorrectionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RedirectService @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 vatReturnService: VatReturnService,
                                 correctionService: CorrectionService
                            )(implicit ec: ExecutionContext)extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def validate(period: Period)(implicit request: DataRequest[AnyContent]): List[ValidationError] = {

    val validatedVatReturnRequest =
      vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration)

    val validatedCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage).map(_ =>
      correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration.commencementDate))

    (validatedVatReturnRequest, validatedCorrectionRequest) match {
      case (Invalid(vatReturnErrors), Some(Invalid(correctionErrors))) =>
        (vatReturnErrors ++ correctionErrors).toChain.toList
      case (Invalid(errors), _) =>
        errors.toChain.toList
      case (_, Some(Invalid(errors))) =>
        errors.toChain.toList
      case _ => List.empty[ValidationError]
    }
  }

  def getRedirect(errors: List[ValidationError], period: Period): List[Call] = {
    errors.flatMap {
      case DataMissingError(AllSalesFromNiQuery) =>
        logger.error(s"Data missing - no data provided for NI sales")
        Some(routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(VatRatesFromNiPage(index)) =>
        logger.error(s"Data missing - vat rates with index ${index.position}")
        Some(routes.VatRatesFromNiController.onPageLoad(CheckMode, period, index))
      case DataMissingError(NiSalesAtVatRateQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - net value of sales at vat rate ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, period, countryIndex, vatRateIndex))
      case DataMissingError(VatOnSalesFromNiQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat charged on sales at vat rate ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.VatOnSalesFromNiController.onPageLoad(CheckMode, period, countryIndex, vatRateIndex))

      case DataMissingError(AllSalesFromEuQuery) =>
        logger.error(s"Data missing - no data provided for EU sales")
        Some(routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(AllSalesToEuQuery(countryFromIndex)) =>
        logger.error(s"Data missing - country of consumption from country ${countryFromIndex.position}")
        Some(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, period, countryFromIndex, Index(0)))
      case DataMissingError(VatRatesFromEuPage(countryFromIndex, countryToIndex)) =>
        logger.error(s"Data missing - vat rates for sales from country ${countryFromIndex.position} to country ${countryToIndex.position}")
        Some(routes.VatRatesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex))
      case DataMissingError(EuSalesAtVatRateQuery(countryFromIndex, countryToIndex, vatRateIndex)) =>
        logger.error(s"Data missing - net value of sales from country ${countryFromIndex.position} to country " +
          s"${countryToIndex.position} at vat rate ${vatRateIndex.position} ")
        Some(routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex, vatRateIndex))
      case DataMissingError(VatOnSalesFromEuQuery(countryFromIndex, countryToIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat charged on sales from country ${countryFromIndex.position} to country " +
          s"${countryToIndex.position} at vat rate ${vatRateIndex.position} ")
        Some(routes.VatOnSalesFromEuController.onPageLoad(CheckMode, period, countryFromIndex, countryToIndex, vatRateIndex))

      case DataMissingError(AllCorrectionPeriodsQuery) =>
        logger.error(s"Data missing - no data provided for corrections")
        Some(correctionsRoutes.CorrectionReturnPeriodController.onPageLoad(CheckMode, period, Index(0)))
      case DataMissingError(AllCorrectionCountriesQuery(periodIndex)) =>
        logger.error(s"Data missing - no countries found for corrections to period ${periodIndex.position}")
        Some(correctionsRoutes.CorrectionCountryController.onPageLoad(CheckMode, period, periodIndex, Index(0)))
      case DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)) =>
        logger.error(s"Data missing - correction to country ${countryIndex.position} in period ${periodIndex.position}")
        Some(correctionsRoutes.CountryVatCorrectionController.onPageLoad(CheckMode, period, periodIndex, countryIndex, undeclaredCountry = false))

      case DataMissingError(_) =>
        logger.error(s"Unhandled DataMissingError")
        None
      case _ =>
        logger.error(s"Unhandled ValidationError")
        None
    }
  }
}

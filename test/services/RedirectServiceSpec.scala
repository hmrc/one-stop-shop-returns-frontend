/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import controllers.actions.AuthenticatedControllerComponents
import controllers.corrections.routes as correctionRoutes
import controllers.routes
import models.requests.DataRequest
import models.{CheckMode, DataMissingError, Index, UserAnswers, ValidationError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.corrections.CorrectionCountryPage
import pages.{SoldGoodsFromNiPage, VatOnSalesFromEuPage, VatRatesFromEuPage, VatRatesFromNiPage}
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import queries.corrections.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import queries.*
import services.corrections.CorrectionService

class RedirectServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockVatReturnService: VatReturnService = mock[VatReturnService]
  private val mockCorrectionService: CorrectionService = mock[CorrectionService]

  private implicit val dataRequest: DataRequest[AnyContent] = DataRequest[AnyContent](FakeRequest(), testCredentials, vrn, registration, completeUserAnswersWithCorrections)

  override def beforeEach(): Unit = {
    reset(mockVatReturnService)
    reset(mockCorrectionService)
    super.beforeEach()
  }

  "RedirectService" - {

    ".validate" - {

      "must return a list of VAT Return validation errors when users answers contains validation errors for a VAT Return" in {

        val validationError: ValidationError = DataMissingError(VatRatesFromNiPage(Index(0)))

        when(mockVatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn validationError.invalidNec
        when(mockCorrectionService.fromUserAnswers(any(), any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val result = service.validate(period)

          result `mustBe` List(validationError)
        }
      }

      "must return a list of Correction validation errors when users answers contains validation errors for Corrections" in {

        val validationError: ValidationError = DataMissingError(CorrectionCountryPage(Index(0), Index(0)))

        when(mockCorrectionService.fromUserAnswers(any(), any(), any(), any())) thenReturn validationError.invalidNec
        when(mockVatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val result = service.validate(period)

          result `mustBe` List(validationError)
        }
      }

      "must return a list of both VAT Return and Correction validation errors when users answers contains validation " +
        "errors for both VAT Return and Corrections" in {

        val vatReturnValidationError: ValidationError = DataMissingError(VatRatesFromNiPage(Index(0)))
        val correctionValidationError: ValidationError = DataMissingError(CorrectionCountryPage(Index(0), Index(0)))

        when(mockVatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn vatReturnValidationError.invalidNec
        when(mockCorrectionService.fromUserAnswers(any(), any(), any(), any())) thenReturn correctionValidationError.invalidNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val result = service.validate(period)

          result `mustBe` List(vatReturnValidationError, correctionValidationError)
        }
      }

      "must return an empty list when users answers contain no validation errors for VAT returns or Corrections" in {

        when(mockCorrectionService.fromUserAnswers(any(), any(), any(), any())) thenReturn List.empty.validNec
        when(mockVatReturnService.fromUserAnswers(any(), any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val result = service.validate(period)

          result `mustBe` List.empty
        }
      }
    }

    ".getRedirect" - {

      "from NI" - {

        "must redirect to Country Of Consumption From Ni Controller when there's no data for NI sales present" in {

          val updatedUserAnswers: UserAnswers = completeSalesFromNIUserAnswers
            .remove(AllSalesFromNiQuery).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(AllSalesFromNiQuery)

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.CountryOfConsumptionFromNiController.onPageLoad(CheckMode, period, Index(0)))
          }
        }

        "must redirect to Vat Rates From Ni Controller when there's no data present for a selected vat rate" in {

          val updatedUserAnswers: UserAnswers = completeSalesFromNIUserAnswers
            .remove(VatRatesFromNiPage(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(VatRatesFromNiPage(Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.VatRatesFromNiController.onPageLoad(CheckMode, period, Index(0)))
          }
        }

        "must redirect to Net Value Of Sales From Ni Controller when there's no data present for net value of sales at vat rate for country" in {

          val updatedUserAnswers: UserAnswers = completeSalesFromNIUserAnswers
            .remove(NiSalesAtVatRateQuery(Index(0), Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(NiSalesAtVatRateQuery(Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.NetValueOfSalesFromNiController.onPageLoad(CheckMode, period, Index(0), Index(0)))
          }
        }

        "must redirect to Vat On Sales From Ni Controller when there's no data present for vat charged on sales at vat rate for country" in {

          val updatedUserAnswers: UserAnswers = completeSalesFromNIUserAnswers
            .remove(VatRatesFromNiPage(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(VatOnSalesFromNiQuery(Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.VatOnSalesFromNiController.onPageLoad(CheckMode, period, Index(0), Index(0)))
          }
        }
      }

      "from EU" - {

        "must redirect to Country Of Sale From Eu Controller when there's no EU sales data present" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswers
            .remove(AllSalesFromEuQuery).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(AllSalesFromEuQuery)

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.CountryOfSaleFromEuController.onPageLoad(CheckMode, period, Index(0)))
          }
        }

        "must redirect to Country Of Consumption From Eu Controller when there's no country of consumption from country data present" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswers
            .remove(AllSalesToEuQuery(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(AllSalesToEuQuery(Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.CountryOfConsumptionFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0)))
          }
        }

        "must redirect to Vat Rates From Eu Controller when there's no data present for vat rates for sales from a country to a country" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswers
            .remove(VatRatesFromEuPage(Index(0), Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(VatRatesFromEuPage(Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.VatRatesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0)))
          }
        }

        "must redirect to Net Value Of Sales From Eu Controller when no data is present for net value of sales from a country to a country at the selected vat rate" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswers
            .remove(EuSalesAtVatRateQuery(Index(0), Index(0), Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(EuSalesAtVatRateQuery(Index(0), Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.NetValueOfSalesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0), Index(0)))
          }
        }

        "must redirect to Vat On Sales From Eu Controller when no data is present for vat charged on sales from a country to a country at the selected vat rate" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswers
            .remove(VatOnSalesFromEuPage(Index(0), Index(0), Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(VatOnSalesFromEuQuery(Index(0), Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(routes.VatOnSalesFromEuController.onPageLoad(CheckMode, period, Index(0), Index(0), Index(0)))
          }
        }
      }

      "corrections" - {

        "must redirect to Correction Return Period Controller when no correction data is present" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswersWithCorrections
            .remove(AllCorrectionPeriodsQuery).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(AllCorrectionPeriodsQuery)

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(correctionRoutes.CorrectionReturnPeriodController.onPageLoad(CheckMode, period, Index(0)))
          }
        }

        "must redirect to Correction Country Controller when no data is present for corrections for the selected country and period" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswersWithCorrections
            .remove(AllCorrectionCountriesQuery(Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(AllCorrectionCountriesQuery(Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(correctionRoutes.CorrectionCountryController.onPageLoad(CheckMode, period, Index(0), Index(0)))
          }
        }

        "must redirect to Country Vat Correction Controller when no data is present for correction to country in period" in {

          val updatedUserAnswers: UserAnswers = completeUserAnswersWithCorrections
            .remove(CorrectionToCountryQuery(Index(0), Index(0))).success.value

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
            .build()

          running(application) {

            val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

            val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

            val validationError = DataMissingError(CorrectionToCountryQuery(Index(0), Index(0)))

            val result = service.getRedirect(List(validationError), period).headOption

            result `mustBe` Some(correctionRoutes.CountryVatCorrectionController.onPageLoad(CheckMode, period, Index(0), Index(0), undeclaredCountry = false))
          }
        }
      }

      "must return None when any other data is missing" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(SoldGoodsFromNiPage).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val validationError = DataMissingError(SoldGoodsFromNiPage)

          val result = service.getRedirect(List(validationError), period).headOption

          result `mustBe` None
        }
      }


      "must return None when there are no validation errors" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockVatReturnService, mockCorrectionService)

          val result = service.getRedirect(List.empty, period).headOption

          result `mustBe` None
        }
      }
    }
  }
}
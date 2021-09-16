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

package base

import controllers.actions._
import generators.Generators
import models.VatOnSalesChoice.Standard
import models.PaymentReference
import models.domain.{EuTaxIdentifier, EuTaxIdentifierType, SalesDetails, SalesFromEuCountry, SalesToCountry, VatReturn, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.registration._
import models.requests.VatReturnRequest
import models.{Country, Index, Period, Quarter, ReturnReference, UserAnswers, VatOnSales, VatOnSalesChoice, VatRate, VatRateType}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with Generators {

  val index: Index                 = Index(0)
  def period: Period               = Period(2021, Quarter.Q3)
  val userAnswersId: String        = "12345-credId"
  val testCredentials: Credentials = Credentials(userAnswersId, "GGW")
  val vrn: Vrn                     = Vrn("123456789")

  val arbitraryDate: LocalDate        = datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31)).sample.value
  val arbitraryInstant: Instant       = arbitraryDate.atStartOfDay(ZoneId.systemDefault).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault)

  val address: UkAddress = UkAddress("line 1", None, "town", None, "AA11 1AA")
  val registration: Registration = Registration(
    vrn                   = Vrn("123456789"),
    registeredCompanyName = arbitrary[String].sample.value,
    vatDetails            = VatDetails(LocalDate.of(2000, 1, 1), address, false, VatDetailSource.Mixed),
    euRegistrations       = Nil,
    contactDetails        = ContactDetails("name", "0123 456789", "email@example.com"),
    commencementDate      = LocalDate.now,
    isOnlineMarketplace   = false
  )

  val twentyPercentVatRate = VatRate(20, VatRateType.Reduced, arbitraryDate)
  val fivePercentVatRate = VatRate(5, VatRateType.Reduced, arbitraryDate)

  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId, period, lastUpdated = arbitraryInstant)

  def completeSalesFromNIUserAnswers : UserAnswers = UserAnswers(userAnswersId, period, lastUpdated = arbitraryInstant)
    .set(SoldGoodsFromNiPage, true).success.value
    .set(CountryOfConsumptionFromNiPage(index), Country("ES", "Spain")).success.value
    .set(VatRatesFromNiPage(index), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
    .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromNiPage(index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(1000))).success.value

  def completeUserAnswers : UserAnswers = completeSalesFromNIUserAnswers
    .set(SoldGoodsFromEuPage,true).success.value
    .set(CountryOfSaleFromEuPage(index), Country("HR", "Croatia")).success.value
    .set(CountryOfConsumptionFromEuPage(index, index), Country("BE", "Belgium")).success.value
    .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate)).success.value
    .set(NetValueOfSalesFromEuPage(index, index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromEuPage(index, index, index), VatOnSales(Standard, BigDecimal(20))).success.value

  val completeVatReturn: VatReturn =
      VatReturn(
        Vrn("063407423"),
        Period("2086", "Q3").get,
        ReturnReference("XI/XI063407423/Q3.2086"),
        PaymentReference("XI063407423Q386"),
        None,
        None,
        List(SalesToCountry(Country("LT",
          "Lithuania"),
          List(SalesDetails(DomainVatRate(45.54,
            DomainVatRateType.Reduced),
            306338.71,
            VatOnSales(Standard, 230899.32)),
            SalesDetails(DomainVatRate(98.54,
              DomainVatRateType.Reduced),
              295985.50,
              VatOnSales(Standard, 319051.84)))),
          SalesToCountry(Country("MT",
            "Malta"),
            List(SalesDetails(DomainVatRate(80.28,
              DomainVatRateType.Standard),
              357873.00,
              VatOnSales(Standard, 191855.64))))),
        List(SalesFromEuCountry(Country("DE", "Germany"),
          Some(EuTaxIdentifier(EuTaxIdentifierType.Vat, "-1")),
          List(SalesToCountry(Country("FI",
            "Finland"),
            List(SalesDetails(DomainVatRate(56.02,
              DomainVatRateType.Standard),
              543742.51,
              VatOnSales(Standard, 801143.05)))))),
          SalesFromEuCountry(Country("IE",
            "Ireland"),
            Some(EuTaxIdentifier(EuTaxIdentifierType.Other, "-2147483648")),
            List(SalesToCountry(Country("CY",
              "Republic of Cyprus"),
              List(SalesDetails(DomainVatRate(98.97,
                DomainVatRateType.Reduced),
                356270.07,
                VatOnSales(Standard, 24080.60)),
                SalesDetails(DomainVatRate(98.92,
                  DomainVatRateType.Reduced),
                  122792.32,
                  VatOnSales(Standard, 554583.78))))))),
        Instant.ofEpochSecond(1630670836),
        Instant.ofEpochSecond(1630670836))

  val emptyVatReturn: VatReturn =
    VatReturn(
      Vrn("063407423"),
      Period("2086", "Q3").get,
      ReturnReference("XI/XI063407423/Q3.2086"),
      PaymentReference("XI063407423Q386"),
      None,
      None,
      List.empty,
      List.empty,
      Instant.ofEpochSecond(1630670836),
      Instant.ofEpochSecond(1630670836)
    )

  val vatReturnRequest: VatReturnRequest =
    VatReturnRequest(
      Vrn("063407423"),
      Period("2086", "Q3").get,
      Some(LocalDate.now()),
      Some(LocalDate.now().plusDays(1)),
      List.empty,
      List.empty
    )

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None, clock: Option[Clock] = None): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[AuthenticatedIdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalActionProvider].toInstance(new FakeDataRetrievalActionProvider(userAnswers)),
        bind[GetRegistrationAction].toInstance(new FakeGetRegistrationAction(registration)),
        bind[CheckReturnsFilterProvider].toInstance(new FakeCheckReturnsFilterProvider()),
        bind[Clock].toInstance(clockToBind)
      )
  }
}

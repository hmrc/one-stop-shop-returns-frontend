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
import models.{Country, Index, Period, Quarter, SalesAtVatRate, SalesDetailsFromEu, UserAnswers, VatRate, VatRateType}
import models.registration._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.{CountryOfConsumptionFromEuPage, CountryOfConsumptionFromNiPage, SalesAtVatRateFromNiPage, SalesDetailsFromEuPage, SoldGoodsFromEuPage, SoldGoodsFromNiPage, VatRatesFromEuPage, VatRatesFromNiPage}
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

  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId, period, lastUpdated = arbitraryInstant)

  def completeSalesFromNIUserAnswers : UserAnswers = UserAnswers(userAnswersId, period, lastUpdated = arbitraryInstant)
    .set(SoldGoodsFromNiPage, true).success.value
    .set(CountryOfConsumptionFromNiPage(index), Country("COU", "country")).success.value
    .set(VatRatesFromNiPage(index), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
    .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(1000))).success.value

  def completeUserAnswers : UserAnswers = completeSalesFromNIUserAnswers
    .set(SoldGoodsFromEuPage,true).success.value
    .set(CountryOfConsumptionFromEuPage(Index(0), Index(0)), Country("BE", "Belgium")).success.value
    .set(VatRatesFromEuPage(Index(0), Index(0)), List(twentyPercentVatRate)).success.value
    .set(SalesDetailsFromEuPage(Index(0), Index(0), Index(0)), SalesDetailsFromEu(BigDecimal(100), BigDecimal(20))).success.value

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None, clock: Option[Clock] = None): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[AuthenticatedIdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalActionProvider].toInstance(new FakeDataRetrievalActionProvider(userAnswers)),
        bind[GetRegistrationAction].toInstance(new FakeGetRegistrationAction(registration)),
        bind[Clock].toInstance(clockToBind)
      )
  }
}

package repositories

import config.FrontendAppConfig
import generators.Generators
import models.domain.VatReturn
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class CachedVatReturnRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CachedVatReturnWrapper]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Generators {

  private val userId  = "id-123"
  private val instant = Instant.now
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  private val vatReturn: VatReturn = arbitrary[VatReturn].sample.value
  private val cachedVatReturnWrapper = CachedVatReturnWrapper(userId, vatReturn.period, Some(vatReturn), instant)


  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cachedVatReturnTtl) thenReturn 1

  protected override val repository = new CachedVatReturnRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )

  ".set" - {

    "must set the last updated time to `now` and save the vat return" in {

      val expectedResult = CachedVatReturnWrapper(userId, vatReturn.period, Some(vatReturn), instant)

      val setResult = repository.set(userId, vatReturn.period, Some(vatReturn)).futureValue
      val dbRecord  = findAll().futureValue.headOption.value

      setResult mustEqual true
      dbRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this user" - {

      "must get the record" in {

        val wrapper = CachedVatReturnWrapper(userId, vatReturn.period, Some(vatReturn), instant)
        insert(wrapper).futureValue

        val result = repository.get(userId, vatReturn.period).futureValue

        result.value mustEqual cachedVatReturnWrapper
      }
    }

    "when there is no record for this user" - {

      "must return None" in {

        repository.get(userId, vatReturn.period).futureValue must not be defined
      }
    }
  }

  "clear" - {

    "must remove a record" in {

      val wrapper = CachedVatReturnWrapper(userId, vatReturn.period, Some(vatReturn), instant)
      insert(wrapper).futureValue

      val result = repository.clear(wrapper.userId, vatReturn.period).futureValue

      result mustEqual true
      repository.get(wrapper.userId, vatReturn.period).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist", vatReturn.period).futureValue

      result mustEqual true
    }
  }
}

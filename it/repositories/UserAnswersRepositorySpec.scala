package repositories

import com.typesafe.config.Config
import config.FrontendAppConfig
import crypto.UserAnswersEncryptor
import generators.Generators
import models.{EncryptedUserAnswers, StandardPeriod, UserAnswers}
import models.Quarter.{Q3, Q4}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import services.crypto.EncryptionService
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class UserAnswersRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedUserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Generators {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  private val userAnswers = UserAnswers("id", StandardPeriod(2021, Q3))
  private val otherUserAnswers = UserAnswers("id", StandardPeriod(2021, Q4))

  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockEncryptionService: EncryptionService = mock[EncryptionService]
  private val encryptor = new UserAnswersEncryptor(mockAppConfig, mockEncryptionService)
  private val secretKey: String = "VqmXp7yigDFxbCUdDdNZVIvbW6RgPNJsliv6swQNCL8="

  protected override val repository: UserAnswersRepository = new UserAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    encryptor = encryptor,
    clock = stubClock
  )

  when(mockConfiguration.underlying) thenReturn mockConfig
  when(mockConfig.getString(any())) thenReturn secretKey
  when(mockAppConfig.encryptionKey) thenReturn secretKey

  private val encryptedUserAnswersData = "WovNVJUSKWKgpi/IrP3cgnx1COboOHM/0XqRS8XWpoD0Gj2Ng+DxqPUEXnEP"
  when(mockEncryptionService.encryptField(any())) thenReturn encryptedUserAnswersData
  when(mockEncryptionService.decryptField(any())) thenReturn userAnswers.data.toString

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
      val encryptedExpectedResult = encryptor.encryptUserAnswers(expectedResult)

      val setResult     = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("userId", userAnswers.userId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual encryptedExpectedResult
    }
  }

  ".get" - {

    "when there is a record for this id and period" - {

      "must update the lastUpdated time and get the record" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue
        insert(encryptor.encryptUserAnswers(otherUserAnswers)).futureValue

        val result         = repository.get(userAnswers.userId, StandardPeriod(2021, Q3)).futureValue
        val expectedResult = userAnswers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

        repository.get("id", StandardPeriod(2021, Q4)).futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

      val result = repository.clear(userAnswers.userId).futureValue

      result mustEqual true
      repository.get(userAnswers.userId, StandardPeriod(2021, Q3)).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {

    "when there are records for this id" - {

      "must update their lastUpdated to `now` and return true" in {


        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue
        insert(encryptor.encryptUserAnswers(otherUserAnswers)).futureValue

        val result = repository.keepAlive("id").futureValue

        val expectedUpdatedAnswers = Seq(
          userAnswers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS)),
          otherUserAnswers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
        )

        val encryptedExpectedUpdatedAnswers = expectedUpdatedAnswers.map(encryptor.encryptUserAnswers)

        result mustEqual true
        val updatedAnswers = find(Filters.equal("userId", "id")).futureValue
        updatedAnswers must contain theSameElementsAs encryptedExpectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }
  }
}

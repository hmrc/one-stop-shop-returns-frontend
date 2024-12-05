package repositories

import config.FrontendAppConfig
import generators.Generators
import models.SessionData
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[SessionData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Generators {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied session data to `now`, and save them" in {

      val sessionData = SessionData("id")
      val expectedResult = sessionData copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))

      val setResult     = repository.set(sessionData).futureValue
      val updatedRecord = find(Filters.equal("userId", sessionData.userId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this id and period" - {

      "must update the lastUpdated time and get the record" in {

        val answers = SessionData("id")
        val otherAnswers = SessionData("zzz")
        insert(answers).futureValue
        insert(otherAnswers).futureValue

        val result         = repository.get(answers.userId).futureValue
        val expectedResult = answers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))

        result.head mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return empty sequence" in {

        val answers = SessionData("id")
        insert(answers).futureValue

        repository.get("zzz").futureValue mustBe Seq.empty
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      val answers = SessionData("id")

      insert(answers).futureValue

      val result = repository.clear(answers.userId).futureValue

      result mustEqual true
      repository.get(answers.userId).futureValue mustBe Seq.empty
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {

    "when there are records for this id" - {

      "must update their lastUpdated to `now` and return true" in {

        val answers = SessionData("id")
        insert(answers).futureValue

        val result = repository.keepAlive("id").futureValue

        val expectedUpdatedAnswers = Seq(
          answers copy (lastUpdated = Instant.now(stubClock).truncatedTo(ChronoUnit.MILLIS))
        )

        result mustEqual true
        val updatedAnswers = find(Filters.equal("userId", "id")).futureValue
        updatedAnswers must contain theSameElementsAs expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }
  }
}

package repositories

import config.FrontendAppConfig
import generators.Generators
import models.Quarter.{Q3, Q4}
import models.{Period, UserAnswers}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class UserAnswersRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[UserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Generators {

  private val instant = Instant.now
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new UserAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val answers = UserAnswers("id", Period(2021, Q3))
      val expectedResult = answers copy (lastUpdated = instant)

      val setResult     = repository.set(answers).futureValue
      val updatedRecord = find(Filters.equal("userId", answers.userId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {

    "when there is a record for this id and period" - {

      "must update the lastUpdated time and get the record" in {

        val answers = UserAnswers("id", Period(2021, Q3))
        val otherAnswers = UserAnswers("id", Period(2021, Q4))
        insert(answers).futureValue
        insert(otherAnswers).futureValue

        val result         = repository.get(answers.userId, Period(2021, Q3)).futureValue
        val expectedResult = answers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        val answers = UserAnswers("id", Period(2021, Q3))
        insert(answers).futureValue

        repository.get("id", Period(2021, Q4)).futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      val period = Period(2021, Q3)

      val answers = UserAnswers("id", period)

      insert(answers).futureValue

      val result = repository.clear(answers.userId).futureValue

      result mustEqual true
      repository.get(answers.userId, period).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {

    "when there are records for this id" - {

      "must update their lastUpdated to `now` and return true" in {

        val answers = UserAnswers("id", Period(2021, Q3))
        val otherAnswers = UserAnswers("id", Period(2021, Q4))
        insert(answers).futureValue
        insert(otherAnswers).futureValue

        val result = repository.keepAlive("id").futureValue

        val expectedUpdatedAnswers = Seq(
          answers copy (lastUpdated = instant),
          otherAnswers copy (lastUpdated = instant)
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

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

package repositories

import config.FrontendAppConfig
import models.{Period, UserAnswers}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import org.mongodb.scala.ObservableFuture

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserAnswersRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   appConfig: FrontendAppConfig,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "user-answers",
    mongoComponent = mongoComponent,
    domainFormat   = UserAnswers.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending("userId", "period"),
        IndexOptions()
          .name("userIdAndPeriodIdx")
          .unique(true)
      )
    )
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byUserId(userId: String): Bson = Filters.equal("userId", userId)

  private def byUserIdAndPeriod(userId: String, period: Period): Bson =
    Filters.and(
      Filters.equal("userId", userId),
      Filters.equal("period", period.toBson)
    )

  def keepAlive(userId: String): Future[Boolean] =
    collection
      .updateMany(
        filter = byUserId(userId),
        update = Updates.set("lastUpdated", Instant.now(clock)),
      )
      .toFuture()
      .map(_ => true)

  def get(userId: String, period: Period): Future[Option[UserAnswers]] =
    keepAlive(userId).flatMap {
      _ =>
        val periodToReturn = collection
          .find(byUserId(userId))
          .filter(userAnswers => userAnswers.period.year == period.year && userAnswers.period.quarter == period.quarter)
          .headOption()

        periodToReturn
    }

  def get(userId: String): Future[Seq[UserAnswers]] =
    keepAlive(userId).flatMap {
      _ =>
        collection
          .find(byUserId(userId)).toFuture()
    }

  def set(answers: UserAnswers): Future[Boolean] = {

    val updatedAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter      = byUserIdAndPeriod(updatedAnswers.userId, answers.period),
        replacement = updatedAnswers,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def clear(userId: String): Future[Boolean] =
    collection
      .deleteOne(byUserId(userId))
      .toFuture()
      .map(_ => true)
}

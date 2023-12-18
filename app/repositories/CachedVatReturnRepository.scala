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

package repositories

import config.FrontendAppConfig
import models.StandardPeriod
import models.domain.VatReturn
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CachedVatReturnRepository @Inject()(
                                         mongoComponent: MongoComponent,
                                         appConfig: FrontendAppConfig,
                                         clock: Clock
                                         )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[CachedVatReturnWrapper](
    collectionName = "cachedVatReturns",
    mongoComponent = mongoComponent,
    domainFormat = CachedVatReturnWrapper.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cachedVatReturnTtl, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending("userId", "period"),
        IndexOptions()
          .name("userIdAndPeriodIdx")
          .unique(true)
      )
    )
  ) {

  private def byId(id: String, period: StandardPeriod): Bson = Filters.and(
    Filters.equal("userId", id),
    Filters.equal("period", period.toBson(legacyNumbers = false))
  )

  def get(id: String, period: StandardPeriod): Future[Option[CachedVatReturnWrapper]] =
    collection
      .find(byId(id, period))
      .headOption()

  def set(userId: String, period: StandardPeriod, vatReturn: Option[VatReturn]): Future[Boolean] = {

    val wrapper = CachedVatReturnWrapper(userId, period, vatReturn, Instant.now(clock))

    collection
      .replaceOne(
        filter      = byId(userId, period),
        replacement = wrapper,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def clear(userId: String, period: StandardPeriod): Future[Boolean] =
    collection
      .deleteOne(byId(userId, period))
      .toFuture()
      .map(_ => true)
}


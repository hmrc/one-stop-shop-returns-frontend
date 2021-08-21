#!/bin/bash

echo ""
echo "Applying migration SoldGoodsFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/soldGoodsFromEu                        controllers.SoldGoodsFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/soldGoodsFromEu                        controllers.SoldGoodsFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeSoldGoodsFromEu                  controllers.SoldGoodsFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeSoldGoodsFromEu                  controllers.SoldGoodsFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "soldGoodsFromEu.title = soldGoodsFromEu" >> ../conf/messages.en
echo "soldGoodsFromEu.heading = soldGoodsFromEu" >> ../conf/messages.en
echo "soldGoodsFromEu.checkYourAnswersLabel = soldGoodsFromEu" >> ../conf/messages.en
echo "soldGoodsFromEu.error.required = Select yes if soldGoodsFromEu" >> ../conf/messages.en
echo "soldGoodsFromEu.change.hidden = SoldGoodsFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySoldGoodsFromEuUserAnswersEntry: Arbitrary[(SoldGoodsFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SoldGoodsFromEuPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySoldGoodsFromEuPage: Arbitrary[SoldGoodsFromEuPage.type] =";\
    print "    Arbitrary(SoldGoodsFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SoldGoodsFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration SoldGoodsFromEu completed"

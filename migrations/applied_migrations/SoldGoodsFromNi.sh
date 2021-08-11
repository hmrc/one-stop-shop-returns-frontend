#!/bin/bash

echo ""
echo "Applying migration SoldGoodsFromNi"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /soldGoodsFromNi                        controllers.SoldGoodsFromNiController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /soldGoodsFromNi                        controllers.SoldGoodsFromNiController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeSoldGoodsFromNi                  controllers.SoldGoodsFromNiController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeSoldGoodsFromNi                  controllers.SoldGoodsFromNiController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "soldGoodsFromNi.title = soldGoodsFromNi" >> ../conf/messages.en
echo "soldGoodsFromNi.heading = soldGoodsFromNi" >> ../conf/messages.en
echo "soldGoodsFromNi.checkYourAnswersLabel = soldGoodsFromNi" >> ../conf/messages.en
echo "soldGoodsFromNi.error.required = Select yes if soldGoodsFromNi" >> ../conf/messages.en
echo "soldGoodsFromNi.change.hidden = SoldGoodsFromNi" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySoldGoodsFromNiUserAnswersEntry: Arbitrary[(SoldGoodsFromNiPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SoldGoodsFromNiPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySoldGoodsFromNiPage: Arbitrary[SoldGoodsFromNiPage.type] =";\
    print "    Arbitrary(SoldGoodsFromNiPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SoldGoodsFromNiPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration SoldGoodsFromNi completed"

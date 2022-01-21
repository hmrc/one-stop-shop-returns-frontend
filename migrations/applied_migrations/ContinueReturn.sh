#!/bin/bash

echo ""
echo "Applying migration ContinueReturn"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/continueReturn                        controllers.ContinueReturnController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/continueReturn                        controllers.ContinueReturnController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeContinueReturn                  controllers.ContinueReturnController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeContinueReturn                  controllers.ContinueReturnController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "continueReturn.title = ContinueReturn" >> ../conf/messages.en
echo "continueReturn.heading = ContinueReturn" >> ../conf/messages.en
echo "continueReturn.continue = Continue my return" >> ../conf/messages.en
echo "continueReturn.delete = Delete my return and start again" >> ../conf/messages.en
echo "continueReturn.checkYourAnswersLabel = ContinueReturn" >> ../conf/messages.en
echo "continueReturn.error.required = Select continueReturn" >> ../conf/messages.en
echo "continueReturn.change.hidden = ContinueReturn" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContinueReturnUserAnswersEntry: Arbitrary[(ContinueReturnPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[ContinueReturnPage.type]";\
    print "        value <- arbitrary[ContinueReturn].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContinueReturnPage: Arbitrary[ContinueReturnPage.type] =";\
    print "    Arbitrary(ContinueReturnPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryContinueReturn: Arbitrary[ContinueReturn] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(ContinueReturn.values.toSeq)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(ContinueReturnPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration ContinueReturn completed"

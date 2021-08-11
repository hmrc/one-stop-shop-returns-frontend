#!/bin/bash

echo ""
echo "Applying migration StartReturn"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /startReturn                        controllers.StartReturnController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /startReturn                        controllers.StartReturnController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeStartReturn                  controllers.StartReturnController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeStartReturn                  controllers.StartReturnController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "startReturn.title = startReturn" >> ../conf/messages.en
echo "startReturn.heading = startReturn" >> ../conf/messages.en
echo "startReturn.checkYourAnswersLabel = startReturn" >> ../conf/messages.en
echo "startReturn.error.required = Select yes if startReturn" >> ../conf/messages.en
echo "startReturn.change.hidden = StartReturn" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryStartReturnUserAnswersEntry: Arbitrary[(StartReturnPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[StartReturnPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryStartReturnPage: Arbitrary[StartReturnPage.type] =";\
    print "    Arbitrary(StartReturnPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(StartReturnPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration StartReturn completed"

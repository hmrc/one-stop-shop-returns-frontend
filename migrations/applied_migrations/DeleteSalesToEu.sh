#!/bin/bash

echo ""
echo "Applying migration DeleteSalesToEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/deleteSalesToEu                        controllers.DeleteSalesToEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/deleteSalesToEu                        controllers.DeleteSalesToEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeDeleteSalesToEu                  controllers.DeleteSalesToEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeDeleteSalesToEu                  controllers.DeleteSalesToEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "deleteSalesToEu.title = deleteSalesToEu" >> ../conf/messages.en
echo "deleteSalesToEu.heading = deleteSalesToEu" >> ../conf/messages.en
echo "deleteSalesToEu.checkYourAnswersLabel = deleteSalesToEu" >> ../conf/messages.en
echo "deleteSalesToEu.error.required = Select yes if deleteSalesToEu" >> ../conf/messages.en
echo "deleteSalesToEu.change.hidden = DeleteSalesToEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesToEuUserAnswersEntry: Arbitrary[(DeleteSalesToEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[DeleteSalesToEuPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryDeleteSalesToEuPage: Arbitrary[DeleteSalesToEuPage.type] =";\
    print "    Arbitrary(DeleteSalesToEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(DeleteSalesToEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration DeleteSalesToEu completed"

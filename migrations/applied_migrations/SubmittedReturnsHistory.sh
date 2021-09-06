#!/bin/bash

echo ""
echo "Applying migration SubmittedReturnsHistory"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/submittedReturnsHistory                       controllers.SubmittedReturnsHistoryController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "submittedReturnsHistory.title = submittedReturnsHistory" >> ../conf/messages.en
echo "submittedReturnsHistory.heading = submittedReturnsHistory" >> ../conf/messages.en

echo "Migration SubmittedReturnsHistory completed"

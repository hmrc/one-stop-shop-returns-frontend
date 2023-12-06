#!/bin/bash

echo ""
echo "Applying migration LeaveThisServicePage"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/leaveThisServicePage                       controllers.LeaveThisServicePageController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "leaveThisServicePage.title = leaveThisServicePage" >> ../conf/messages.en
echo "leaveThisServicePage.heading = leaveThisServicePage" >> ../conf/messages.en

echo "Migration LeaveThisServicePage completed"

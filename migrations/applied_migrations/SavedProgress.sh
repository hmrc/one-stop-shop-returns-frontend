#!/bin/bash

echo ""
echo "Applying migration SavedProgress"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/savedProgress                       controllers.SavedProgressController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "savedProgress.title = savedProgress" >> ../conf/messages.en
echo "savedProgress.heading = savedProgress" >> ../conf/messages.en

echo "Migration SavedProgress completed"

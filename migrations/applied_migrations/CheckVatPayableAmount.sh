#!/bin/bash

echo ""
echo "Applying migration CheckVatPayableAmount"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/checkVatPayableAmount                       controllers.CheckVatPayableAmountController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "checkVatPayableAmount.title = checkVatPayableAmount" >> ../conf/messages.en
echo "checkVatPayableAmount.heading = checkVatPayableAmount" >> ../conf/messages.en

echo "Migration CheckVatPayableAmount completed"

# Twilio Verify Setup Guide for Calinga App

## Overview
The Calinga app uses Twilio Verify for OTP (One-Time Password) verification during phone number registration. Twilio Verify provides robust, scalable SMS verification without requiring a dedicated phone number.

## Setting Up Twilio Verify

### 1. Create a Twilio Account
1. Go to [Twilio.com](https://www.twilio.com)
2. Sign up for a free account
3. Complete phone verification for your account

### 2. Create a Verify Service
1. In your Twilio Console, go to **Verify** > **Services**
2. Click "Create new Verify Service"
3. Give it a name (e.g., "Calinga App Verification")
4. Configure settings as needed (default settings work fine)
5. Note the **Service SID** (starts with "VA") - you'll need this

### 3. Get Your Credentials
After creating your account and Verify service, you'll need these credentials:

1. **Account SID** - Found on your Twilio Console Dashboard (starts with "AC")
2. **Auth Token** - Found on your Twilio Console Dashboard (click "Show" to reveal)
3. **Verify Service SID** - From the Verify service you created (starts with "VA")

**Note:** You do NOT need a Twilio phone number when using the Verify service.

### 4. Configure the App

Edit the file: `app/src/main/assets/twilio.properties`

```properties
# Your Twilio Account SID (starts with AC)
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Your Twilio Auth Token
TWILIO_AUTH_TOKEN=your_auth_token_here

# Your Verify Service SID (starts with VA)
TWILIO_VERIFY_SERVICE_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Phone number not needed when using Verify Service
# TWILIO_PHONE_NUMBER=not_required_for_verify_service
```

### 5. Security Best Practices

#### For Development:
- Keep your credentials in the `twilio.properties` file
- Add `twilio.properties` to your `.gitignore` file (already configured):
```
# Twilio credentials
app/src/main/assets/twilio.properties
twilio.properties
```

#### For Production:
- Store credentials as environment variables
- Use Android's encrypted shared preferences
- Consider using a backend service to handle Twilio API calls

### 6. Rate Limiting & Features
The app includes built-in protections:
- **Rate limiting**: Users can only request one OTP per minute per phone number
- **Automatic retry logic**: Handles network errors gracefully
- **Error handling**: Clear error messages for users
- **Security**: No phone number required, uses Twilio's secure infrastructure

### 7. Supported Countries
The Twilio Verify service supports phone numbers from many countries including:
- **United States (+1)** - for US users
- **Philippines (+63)** - for Philippines users
- **Many other countries** - Twilio Verify automatically handles global SMS delivery

To add more country codes to the app UI:
1. Edit `RegisterActivity.kt`
2. Update the `setupCountryCodeSpinner()` method
3. Add more country codes to the array

### 8. Cost & Benefits
**Twilio Verify Advantages:**
- No need to purchase/manage phone numbers
- Built-in rate limiting and fraud protection
- Global SMS delivery infrastructure
- Automatic fallback to voice calls if SMS fails
- Per-verification pricing (typically lower cost)

**Free Trial:**
- New accounts come with free credits
- Monitor usage in Twilio Console > Usage

### 9. Testing
For testing purposes:
1. Use your own phone number with the country code
2. Check Twilio Console > Verify > Logs for verification attempts
3. Test with different country codes
4. Verify rate limiting works (try sending OTP twice quickly)

### 10. Troubleshooting

#### Common Issues:
1. **OTP not received**: 
   - Check Twilio Console > Verify > Logs for delivery status
   - Verify phone number format (+country code + number)
   - Check if number supports SMS in that country

2. **401 Unauthorized**: 
   - Verify Account SID and Auth Token are correct
   - Ensure no extra spaces in credentials

3. **404 Service Not Found**: 
   - Verify the Verify Service SID is correct
   - Ensure the Verify service is active in Twilio Console

4. **Rate limiting messages**: 
   - This is normal - wait 60 seconds between requests
   - Prevents abuse and reduces costs

#### Where to Check Logs:
- **Twilio Console**: Verify > Logs (for SMS delivery status)
- **Android Logcat**: Search for "TwilioService" (for app-level errors)
- **Network**: Ensure device has internet connectivity

### 11. Configuration Summary

Your final `twilio.properties` should look like this:
```properties
# Twilio Configuration for Verify Service
TWILIO_ACCOUNT_SID=AC22328e2a62ac18b456a9bfe5c3cce2dd
TWILIO_AUTH_TOKEN=08017ce56e5915fd4ee771290b1d6bdc
TWILIO_VERIFY_SERVICE_SID=VA7a5954f40796a03d9d0cfa08bb9e0f30
```

**Important:** Replace the example values above with your actual Twilio credentials!

## Alternative: Mock OTP for Development
If you don't want to set up Twilio immediately, you can use the mock OTP mode:
1. Leave the Twilio credentials empty in `twilio.properties`
2. The app will generate a random 6-digit OTP and log it to Android Studio
3. Check Android Logcat to see the generated OTP for testing
4. This allows you to test the UI without SMS charges

## Need Help?
- [Twilio Documentation](https://www.twilio.com/docs)
- [Twilio SMS API Guide](https://www.twilio.com/docs/sms)
- [Twilio Verify API Guide](https://www.twilio.com/docs/verify/api)

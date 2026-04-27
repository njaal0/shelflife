package com.shelflife.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FirebaseConfigTest {

    /**
     * Minimal service-account JSON accepted by {@link com.google.auth.oauth2.GoogleCredentials#fromStream}.
     * The private key is a real PKCS#8-encoded RSA key generated solely for testing purposes;
     * it carries no production secrets.
     */
    private static final String SERVICE_ACCOUNT_JSON = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "key1",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDQqQoucXpNj5NT\\nCA/VX+0oIC7fSuzRVWYPBjvHHusq2OadYrXPGBvS9TN9aQynb46M/ebkiFsO85Xo\\nh4OYssMsWDCx05+U+KkiMyB8lKW/BWjnLUHcJzLlk8PcvxnWs5Wcc6+NLEJdA3hn\\nLvvgqxrX6F48ceY9FG/zQHSTWHg2qDTfWDD2lWYFvR+pLwk3/8S0o08SJdR8TDjy\\nAFW6cIGdhk9etPc3UcbPkoTGGgsV2RS6B8XylyMRRfWCynrNG1vDLleh/LFbK6Hi\\ng3UTmw1dJM/Are3o42uc4y4vmyuFvQpmKjvPVKf4wCs92J9ORoFzqWkZT9KhBFah\\nTaFcG13FAgMBAAECggEAZeDrGjMI2p7L+WH5zR5JnmpcSGT+TNUsABD0levYObLu\\nopCkIWnth+p1yflFFulmihucJSAYiavQGVk1HRTHK7shh2nCxItGlW6uIXW4mZRs\\nHFeiDPpMpQApRPo/hO9pEJsBb2XZPy3nF0Y+7/rLH/kSLeQU3OTCJlxlrt2m0FQd\\nLOHF+IdqPXp1A4kgzQ7xrBaE3aOBqnT1aHLvo+Fal5kjFAYXMjVFJDWPbNvQj1Jx\\nWHdXnOl7NCLzq8A5RzPVUShF5ANH9lX7OXquC/UVNNLWGC2yLYU4Oc1JEkkLx0A5\\nJeQ16EHWO0dyfDwSqEzpLq+j2CELhgz0Yk4Jm72EDwKBgQD5GNMpTgXH03Urnhp+\\naf740NOtv1zr/jtdN4QJoOHvpRqYDR2R6c9xBurt/lrLygJOM3EIOAVyAlsMcMNb\\nthH0AhMCOLpb2D0O2Jf9L9u7KQInq1xIJPd5/9y2290l1A54M909GHUmn2K/HATT\\nRfttvoy0c+csLO/Jj6owOr56SwKBgQDWcVgUu44IH0pkuob/M16x66A6cwTy1Efo\\n9C1v1cD4Ae909E5AInel1fIWX/jnESf8Z62JncZms6wOmTsiugwoQmmAPCGyWti0\\ncdNPnFOz56zqbBnGKDgcAdeGYh0vqmjZPzTdC4bu5rrpo/wm2YO4Qy0cNG+2nAOq\\nsjsIoZZ+LwKBgEUANW4ZqMxDrQ9aCmi+H9z4q8pTWgAdOoCF4lNwd2WUzJsV9vz2\\nUXozulwWuzv8R0s2QLdQHWIJeQMnMVZ0RuDauj1BTY4nhjJYmTGPUkS0BV7LvxJU\\n+yCWWaNloArchO2TIFPONJVwTUA3GLcViaJKyFssR/sar4mb/D7D5wOlAoGBAMG5\\nUU4hhZUW13kZ/PytJp3q9Cv5Xn4UggYUGLwQSayC6xUn400TFi3Nkk7eUphEKnBe\\nJSrOSr7bD9N8YCzmEDeKj46WRAbBBUY2EiS4X/GFmPOJxtr52H6Mt2yiVCWMd/3S\\nmOocEMDf1fld+FwMYm9i2GzDfixk0vcw3h3VBIRnAoGAWGlHRR3Gno7h233uM2C/\\nyzbznT3QuuOrn43XH1T33MirtVkU4/Mul1ctS6E1IZuBhsn+3Y0C3BrZVJA/Gwis\\nO0UNSyj68ZhLnlmqjpJ4Lmt5T2aT54sSrnS4CylYu48IcWNgf9gGpHsm54Bqfyta\\n/svDfLq2sAuQ3g24+yObgQE=\\n-----END PRIVATE KEY-----\\n",
              "client_email": "firebase-adminsdk@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;

    @AfterEach
    void resetFirebaseApps() {
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    /**
     * Verifies that {@link FirebaseConfig#initializeFirebase()} registers a default
     * {@link FirebaseApp} when no app exists yet.
     */
    @Test
    void initializeFirebase_shouldRegisterDefaultApp() throws Exception {
        Resource resource = new ByteArrayResource(SERVICE_ACCOUNT_JSON.getBytes());
        FirebaseConfig config = new FirebaseConfig(resource);

        config.initializeFirebase();

        assertFalse(FirebaseApp.getApps().isEmpty(), "FirebaseApp should be initialized");
    }

    /**
     * Verifies that calling {@link FirebaseConfig#initializeFirebase()} a second time is
     * idempotent and does not throw an exception.
     */
    @Test
    void initializeFirebase_shouldBeIdempotentWhenCalledTwice() throws Exception {
        Resource resource = new ByteArrayResource(SERVICE_ACCOUNT_JSON.getBytes());
        FirebaseConfig config = new FirebaseConfig(resource);

        config.initializeFirebase();

        assertDoesNotThrow(config::initializeFirebase,
                "Second initialization should be a no-op, not throw");
    }
}

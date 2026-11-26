# खर्चा (Kharcha) – automatic payment popup

Bank/UPI SMS aate hi ek popup/notification aata hai: "₹500 kisko gaye?" – ek tap me category choose karo.
Kisi bhi UPI app (PhonePe, Paytm, GPay...) ke payment par kaam karta hai, kyunki bank ka SMS padha jata hai.

## APK kaise milega
GitHub → Actions → **Build Kharcha APK** → latest run → Artifacts → `kharcha-debug-apk`.
Phone me install karo (Unknown sources allow), phir app kholkar SMS + Notification permission do.

## Notes
- Sirf Android. Sab data phone me hi rehta hai.
- Xiaomi/Oppo/Vivo: app ki battery setting "No restrictions" karo, warna background me band ho sakta hai.
- SMS parser: `app/src/main/java/com/udyamcare/kharcha/SmsParser.kt` (tests: `SmsParserTest.kt`).

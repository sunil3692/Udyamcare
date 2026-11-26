# खर्चा (Kharcha) – automatic payment popup + khata

Bank/UPI SMS aate hi popup/notification aata hai: "₹500 kisko gaye?" – ek tap me category choose karo.
PhonePe, Paytm, GPay ya kisi bhi UPI app ke payment par kaam karta hai (bank ka SMS padha jata hai).

## Features
- 🏠 Home: mahine ka kharcha/aamdani/bachat, search, filter, din ke hisab se list, nakad entry (＋)
- 📊 Reports: category donut chart, 6 mahine ka bar chart, top log
- 🤝 Udhaar: kisse kitna lena/dena hai, apne-aap
- ⚙️ Settings: Hindi/English, purane SMS import (30/90 din), CSV export, permission help
- "Pichli baar jaisa" suggestion aur notification me one-tap save buttons

## APK kaise milega
GitHub → Actions → **Build Kharcha APK** → latest run → Artifacts → `kharcha-debug-apk`.

## Notes
- Sirf Android. Sab data phone me hi rehta hai.
- Xiaomi/Oppo/Vivo: app ki battery setting "No restrictions" karo.
- SMS parser: `app/src/main/java/com/udyamcare/kharcha/SmsParser.kt` (tests: `SmsParserTest.kt`).

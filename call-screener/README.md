# AI Call Screener

Call aane par assistant (Hindi/Hinglish me) uthata hai, poochta hai "kaun hai, kya kaam hai", phir
classify karta hai: **emergency / sales / promotional / personal / unclear**, aur aapko SMS bhej deta hai.

- Emergency → aapko turant SMS + aapke number par call forward
- Sales/Promo → politely mana karke call kaat deta hai, SMS summary
- Personal/unclear → message le leta hai, SMS summary

## Setup
1. Twilio account + ek number lo (India ke liye voice-enabled number / ya US number se start karo).
2. `pip install -r requirements.txt`, `.env.example` ko env vars me set karo.
3. `uvicorn app:app --port 8000` (ya Railway par deploy). Public URL ko `PUBLIC_URL` me daalo.
4. Twilio number → Voice webhook: `POST https://<host>/incoming`.
5. Apne phone me **conditional call forwarding** (busy / no-answer) Twilio number par laga do
   (dial `**61*<twilio number>#` jaise carrier codes; Jio/Airtel me Settings → Call forwarding).
6. `ANTHROPIC_API_KEY` dalo to Claude classification karega, warna keyword matching.

Test: `python -m pytest`

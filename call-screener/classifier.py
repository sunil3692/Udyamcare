"""Classify what a caller said: emergency / sales / promotional / personal / unknown."""
import json
import os

CATEGORIES = ["emergency", "sales", "promotional", "personal", "unknown"]

EMERGENCY_WORDS = [
    "emergency", "urgent", "accident", "hospital", "ambulance", "police", "fire",
    "serious", "critical", "jaldi", "turant", "zaruri", "zaroori", "haadsa",
    "एमरजेंसी", "इमरजेंसी", "जरूरी", "ज़रूरी", "तुरंत", "जल्दी", "एक्सीडेंट", "अस्पताल", "हॉस्पिटल",
]
SALES_WORDS = [
    "loan", "insurance", "credit card", "offer", "discount", "scheme", "plan",
    "investment", "demat", "policy", "emi", "bank", "membership", "package",
    "लोन", "बीमा", "इंश्योरेंस", "क्रेडिट कार्ड", "ऑफर", "डिस्काउंट", "स्कीम", "प्लान", "निवेश",
]
PROMO_WORDS = [
    "survey", "feedback", "congratulations", "winner", "lucky draw", "recharge",
    "subscription", "advertisement", "free", "prize", "cashback",
    "सर्वे", "बधाई", "इनाम", "फ्री", "रिचार्ज", "कैशबैक", "लकी ड्रॉ",
]


def keyword_classify(text: str) -> dict:
    t = text.lower()
    scores = {
        "emergency": sum(w in t for w in EMERGENCY_WORDS),
        "sales": sum(w in t for w in SALES_WORDS),
        "promotional": sum(w in t for w in PROMO_WORDS),
    }
    best = max(scores, key=scores.get)
    if scores[best] == 0:
        return {"category": "unknown", "summary": text[:200], "caller_name": ""}
    return {"category": best, "summary": text[:200], "caller_name": ""}


def classify(transcript: str) -> dict:
    """Use Claude when ANTHROPIC_API_KEY is set, else fall back to keywords."""
    if not transcript.strip():
        return {"category": "unknown", "summary": "(caller said nothing)", "caller_name": ""}
    if not os.getenv("ANTHROPIC_API_KEY"):
        return keyword_classify(transcript)
    try:
        import anthropic

        client = anthropic.Anthropic()
        msg = client.messages.create(
            model=os.getenv("CLAUDE_MODEL", "claude-haiku-5-5"),
            max_tokens=300,
            system=(
                "You screen phone calls for the owner. The transcript is what the caller "
                "said (Hindi/English/Hinglish, may have speech-recognition errors). "
                "Reply ONLY with JSON: {\"category\": one of "
                f"{CATEGORIES}, \"caller_name\": string or \"\", "
                "\"summary\": one short English sentence}. "
                "emergency = someone is in danger/hospital/accident/time-critical personal matter. "
                "sales = trying to sell a product/service/loan/insurance. "
                "promotional = surveys, offers, prize/lucky-draw, recorded ads. "
                "personal = known person/family/friend/work contact with a normal reason. "
                "When unsure between emergency and anything else, choose emergency."
            ),
            messages=[{"role": "user", "content": transcript}],
        )
        data = json.loads(msg.content[0].text)
        if data.get("category") not in CATEGORIES:
            data["category"] = "unknown"
        return data
    except Exception:
        return keyword_classify(transcript)

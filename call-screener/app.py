"""AI call screener: Twilio voice webhook -> talk to caller -> classify -> notify owner.

Run:  uvicorn app:app --port 8000   (expose with ngrok / deploy on Railway)
Twilio number's "A call comes in" webhook -> POST https://<host>/incoming
"""
import os
from urllib.parse import quote

from fastapi import FastAPI, Form, Request, Response
from twilio.request_validator import RequestValidator
from twilio.rest import Client
from twilio.twiml.voice_response import Gather, VoiceResponse

from classifier import classify

OWNER_NAME = os.getenv("OWNER_NAME", "Sunil")
OWNER_PHONE = os.getenv("OWNER_PHONE", "")          # where alerts are sent, e.g. +919XXXXXXXXX
TWILIO_NUMBER = os.getenv("TWILIO_NUMBER", "")      # your Twilio number
LANG = os.getenv("SPEECH_LANG", "hi-IN")            # hi-IN understands Hinglish well
VOICE = os.getenv("TTS_VOICE", "Polly.Aditi")       # Hindi/Indian-English female voice
MAX_TURNS = 2

app = FastAPI()
calls: dict[str, list[str]] = {}  # CallSid -> what the caller said so far


def twiml(vr: VoiceResponse) -> Response:
    return Response(content=str(vr), media_type="application/xml")


def say(node, text: str):
    node.say(text, voice=VOICE, language="hi-IN")


def listen(vr: VoiceResponse, prompt: str, turn: int):
    g = Gather(input="speech", language=LANG, speech_timeout="auto",
               action=f"/gather?turn={turn}", method="POST")
    say(g, prompt)
    vr.append(g)
    # no speech heard -> go straight to wrap-up
    vr.redirect(f"/gather?turn={turn}", method="POST")


@app.middleware("http")
async def verify_twilio(request: Request, call_next):
    token = os.getenv("TWILIO_AUTH_TOKEN")
    if token and request.url.path in ("/incoming", "/gather"):
        form = await request.form()
        url = os.getenv("PUBLIC_URL", str(request.base_url).rstrip("/")) + request.url.path
        if request.url.query:
            url += "?" + request.url.query
        sig = request.headers.get("X-Twilio-Signature", "")
        if not RequestValidator(token).validate(url, dict(form), sig):
            return Response("forbidden", status_code=403)
    return await call_next(request)


@app.post("/incoming")
async def incoming(CallSid: str = Form(...)):
    calls[CallSid] = []
    vr = VoiceResponse()
    listen(vr, f"Namaste, main {OWNER_NAME} ka assistant bol raha hoon. "
               "Aap kaun bol rahe hain, aur kis kaam se call kiya hai?", 1)
    return twiml(vr)


@app.post("/gather")
async def gather(turn: int = 1, CallSid: str = Form(...),
                 From: str = Form(""), SpeechResult: str = Form("")):
    if SpeechResult:
        calls.setdefault(CallSid, []).append(SpeechResult)
    transcript = " | ".join(calls.get(CallSid, []))
    result = classify(transcript)
    vr = VoiceResponse()

    # Ask one follow-up if we still can't tell and caller did speak
    if result["category"] == "unknown" and SpeechResult and turn < MAX_TURNS:
        listen(vr, "Kya aap thoda aur bata sakte hain ki kaam kya hai?", turn + 1)
        return twiml(vr)

    cat = result["category"]
    if cat == "emergency":
        say(vr, f"Samajh gaya. Main abhi {OWNER_NAME} ko turant khabar kar raha hoon. "
                "Kripya line par bane rahiye.")
        notify(From, result, transcript, urgent=True)
        # Optional: ring the owner directly for emergencies
        if OWNER_PHONE:
            vr.dial(OWNER_PHONE, caller_id=TWILIO_NUMBER or None, timeout=25)
    elif cat in ("sales", "promotional"):
        say(vr, f"Dhanyawad. {OWNER_NAME} is tarah ki calls nahi lete. Kripya dobara call na karein.")
        notify(From, result, transcript)
        vr.hangup()
    else:
        say(vr, f"Dhanyawad. Maine aapka sandesh {OWNER_NAME} ko bhej diya hai. "
                "Woh jald hi aapko wapas call karenge.")
        notify(From, result, transcript)
        vr.hangup()
    calls.pop(CallSid, None)
    return twiml(vr)


LABELS = {"emergency": "🚨 EMERGENCY", "sales": "💼 Sales call",
          "promotional": "📢 Promotional call", "personal": "👤 Personal/Work",
          "unknown": "❓ Unclear"}


def notify(caller: str, result: dict, transcript: str, urgent: bool = False):
    body = (f"{LABELS.get(result['category'], result['category'])}\n"
            f"From: {caller}\nName: {result.get('caller_name') or '-'}\n"
            f"Summary: {result.get('summary')}\nSaid: {transcript[:300]}")
    print(body)
    sid, token = os.getenv("TWILIO_ACCOUNT_SID"), os.getenv("TWILIO_AUTH_TOKEN")
    if sid and token and OWNER_PHONE and TWILIO_NUMBER:
        Client(sid, token).messages.create(to=OWNER_PHONE, from_=TWILIO_NUMBER, body=body)


@app.get("/")
def health():
    return {"ok": True}

#!/usr/bin/env python3
"""UdyamCare auto poster: topic -> Hindi script -> image + reel -> Facebook Page.

Usage: python post.py --slot loan [--kind reel|image|both] [--dry-run]
Env:   ANTHROPIC_API_KEY, FB_PAGE_ID, FB_PAGE_TOKEN, (optional) ANTHROPIC_MODEL, TTS_VOICE
"""
import argparse, asyncio, json, os, random, subprocess, sys, textwrap
from pathlib import Path

import requests
from PIL import Image, ImageDraw, ImageFont, features

ROOT = Path(__file__).parent
OUT = ROOT / "out"
HISTORY = ROOT / "history.json"
GRAPH = "https://graph.facebook.com/v21.0"
W, H = 1080, 1920
PALETTES = [((11, 92, 173), (6, 40, 90)), ((16, 120, 80), (6, 60, 40)), ((200, 80, 20), (110, 35, 10)), ((110, 50, 170), (55, 20, 90))]
FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf",
    "/usr/share/fonts/opentype/noto/NotoSansDevanagari-Bold.ttf",
    "/usr/share/fonts/truetype/lohit-devanagari/Lohit-Devanagari.ttf",
    "/usr/share/fonts/truetype/freefont/FreeSans.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]
SAMPLE = {
    "title": "PMEGP लोन कैसे पाएं?",
    "slides": ["PMEGP में सब्सिडी के साथ बिज़नेस लोन मिलता है", "सब्सिडी की दर वर्ग और क्षेत्र पर निर्भर करती है", "KVIC पोर्टल पर ऑनलाइन आवेदन करें", "आवेदन के साथ प्रोजेक्ट रिपोर्ट (DPR) लगती है"],
    "voiceover": "क्या आप अपना बिज़नेस शुरू करना चाहते हैं? पी एम ई जी पी योजना में सब्सिडी के साथ लोन मिलता है। आवेदन के लिए के वी आई सी पोर्टल पर जाएं और प्रोजेक्ट रिपोर्ट तैयार रखें।",
    "caption": "PMEGP लोन से अपना बिज़नेस शुरू करें! 💼",
    "tags": ["#PMEGP"],
}


def log(*a):
    print(*a, flush=True)


def load_history():
    try:
        return json.loads(HISTORY.read_text())
    except Exception:
        return []


def load_bank(slot):
    try:
        return json.loads((ROOT / "content_bank" / f"{slot}.json").read_text())
    except Exception:
        return []


def pick_from_bank(slot, history):
    """Next unused bank item for this slot; once all are used, restart from the least recently used."""
    bank = load_bank(slot)
    if not bank:
        return None, False
    used_order = [h["title"] for h in history if h.get("slot") == slot]
    for item in bank:
        if item["title"] not in used_order:
            return item, False
    for t in used_order:  # oldest use first
        for item in bank:
            if item["title"] == t:
                return item, True
    return bank[0], True


def pick_topic(slot, history):
    topics = json.loads((ROOT / "topics.json").read_text())[slot]
    recent = {h["topic"] for h in history[-40:]}
    fresh = [t for t in topics if t not in recent] or topics
    return random.choice(fresh)


def generate_content(slot, topic, recent_titles):
    import anthropic
    client = anthropic.Anthropic()
    prompt = f"""UdyamCare ek Indian MSME/loan/business guidance brand hai. Facebook ke liye Hindi (Devanagari) me content banao.
Category: {slot}
Topic: {topic}
Pehle ye titles aa chuke hain, repeat mat karo: {recent_titles}
Sirf valid facts likho; agar koi amount/percentage pakka na ho to mat likho. Koi jhootha wada (guaranteed loan) mat do.
Sirf JSON do:
{{"title": "chhota title (max 8 shabd)",
 "slides": ["5 se 6 chhoti lines, har line max 12 shabd; aakhri line call-to-action (UdyamCare se jud'ein)"],
 "voiceover": "35-50 second ka natural Hindi script, numbers shabdon me",
 "caption": "2-4 line ka caption emojis ke saath",
 "hashtags": ["#..", "5-7 hashtags"]}}"""
    msg = client.messages.create(model=os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-5-5"), max_tokens=1500, messages=[{"role": "user", "content": prompt}])
    text = msg.content[0].text
    data = json.loads(text[text.index("{"): text.rindex("}") + 1])
    for k in ("title", "slides", "voiceover", "caption", "hashtags"):
        assert k in data, f"missing {k}"
    return data


_FONT_CACHE = {}


def _find_font(pattern, fallbacks):
    key = pattern
    if key not in _FONT_CACHE:
        found = None
        try:
            r = subprocess.run(["fc-match", "-f", "%{file}", pattern], capture_output=True, text=True)
            if r.stdout.strip() and Path(r.stdout.strip()).exists():
                found = r.stdout.strip()
        except Exception:
            pass
        _FONT_CACHE[key] = found or next((p for p in fallbacks if Path(p).exists()), None)
    return _FONT_CACHE[key]


def get_font(size, latin=False):
    if latin:
        path = _find_font("Noto Sans:bold", ["/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"])
    else:
        path = _find_font("Noto Sans Devanagari:bold", FONT_CANDIDATES)
    engine = ImageFont.Layout.RAQM if features.check("raqm") else ImageFont.Layout.BASIC
    try:
        return ImageFont.truetype(path, size, layout_engine=engine) if path else ImageFont.load_default()
    except Exception:
        return ImageFont.load_default()


def runs(text):
    """Split into (segment, is_latin) runs: Latin letters use a Latin font, everything else the Devanagari font
    (the Devanagari font has no Latin glyphs)."""
    out = []
    for ch in text:
        latin = ch.isascii() and ch.isalpha()
        if out and (out[-1][1] == latin or ch == " "):
            out[-1] = (out[-1][0] + ch, out[-1][1])
        else:
            out.append((ch, latin))
    return out


def text_w(d, text, size):
    return sum(d.textlength(seg, font=get_font(size, lat)) for seg, lat in runs(text))


def draw_text(d, xy, text, size, fill):
    x, y = xy
    for seg, lat in runs(text):
        f = get_font(size, lat)
        d.text((x, y), seg, font=f, fill=fill)
        x += d.textlength(seg, font=f)


def wrap(d, text, size, max_w):
    lines, cur = [], ""
    for w in text.split():
        t = (cur + " " + w).strip()
        if text_w(d, t, size) <= max_w:
            cur = t
        else:
            if cur:
                lines.append(cur)
            cur = w
    return lines + [cur] if cur else lines


def gradient(size, palette):
    w, h = size
    top, bot = palette
    mask = Image.linear_gradient("L").resize((w, h))
    return Image.composite(Image.new("RGB", size, bot), Image.new("RGB", size, top), mask)


def fit_lines(d, text, start, minimum, max_w, max_h, spacing=1.45):
    size = start
    while True:
        lines = wrap(d, text, size, max_w)
        if len(lines) * size * spacing <= max_h or size <= minimum:
            return size, lines
        size -= 4


def make_card(text, idx, total, palette, path, title=None):
    img = gradient((W, H), palette)
    d = ImageDraw.Draw(img)
    draw_text(d, (60, 70), "UdyamCare", 56, (255, 255, 255))
    start = 104 if (title and idx == 0) else 88
    size, lines = fit_lines(d, text, start, 52, W - 160, 1100)
    lh = int(size * 1.45)
    y = (H - lh * len(lines)) // 2
    for ln in lines:
        draw_text(d, ((W - text_w(d, ln, size)) / 2, y), ln, size, "white")
        y += lh
    if total > 1:
        draw_text(d, (60, H - 120), f"{idx + 1}/{total}", 44, (255, 255, 255))
    img.save(path)
    return path


def make_image_post(content, palette):
    S = 1080
    path = OUT / "post.png"
    img = gradient((S, S), palette)
    d = ImageDraw.Draw(img)
    draw_text(d, (60, 50), "UdyamCare", 48, (255, 255, 255))
    tsize, tlines = fit_lines(d, content["title"], 84, 56, S - 120, 300)
    y = 140
    for ln in tlines:
        draw_text(d, (60, y), ln, tsize, "white")
        y += int(tsize * 1.4)
    d.rectangle([60, y + 10, 260, y + 16], fill=(255, 255, 255))
    y += 50
    bullets = content["slides"][:4]
    avail = S - y - 60
    size = 56
    while size > 36:
        blocks = [wrap(d, b, size, S - 190) for b in bullets]
        if sum(len(b) for b in blocks) * size * 1.4 + len(bullets) * 22 <= avail:
            break
        size -= 4
    blocks = [wrap(d, b, size, S - 190) for b in bullets]
    for lines in blocks:
        d.ellipse([60, y + size * 0.45, 60 + 20, y + size * 0.45 + 20], fill=(255, 255, 255))
        for ln in lines:
            draw_text(d, (110, y), ln, size, "white")
            y += int(size * 1.4)
        y += 22
    img.save(path)
    return path


async def _tts(text, path):
    import edge_tts
    await edge_tts.Communicate(text, os.environ.get("TTS_VOICE", "hi-IN-SwaraNeural")).save(str(path))


def audio_duration(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)], capture_output=True, text=True, check=True)
    return float(r.stdout.strip())


def make_reel(content, palette, dry_run):
    OUT.mkdir(exist_ok=True)
    audio = OUT / "voice.mp3"
    if dry_run and os.environ.get("SKIP_TTS"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "sine=frequency=220:duration=30", str(audio)], check=True)
    else:
        asyncio.run(_tts(content["voiceover"], audio))
    dur = min(audio_duration(audio), 88)
    slides = [content["title"]] + content["slides"] + ([CTA_SLIDE] if content.get("_slot") else [])
    per = dur / len(slides)
    lst = OUT / "slides.txt"
    lines = []
    for i, s in enumerate(slides):
        p = make_card(s, i, len(slides), palette, OUT / f"s{i}.png", title=content["title"])
        lines.append(f"file '{p.name}'\nduration {per:.3f}")
    lines.append(f"file '{(OUT / f's{len(slides) - 1}.png').name}'")
    lst.write_text("\n".join(lines))
    video = OUT / "reel.mp4"
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst), "-i", str(audio),
                    "-vf", "fps=30,format=yuv420p", "-c:v", "libx264", "-c:a", "aac", "-shortest", "-movflags", "+faststart", str(video)], check=True)
    return video


COMMON_TAGS = ["#UdyamCare", "#MSME", "#BusinessIndia"]
DISCLAIMER = "ℹ️ योजनाओं के नियम बदलते रहते हैं, आवेदन से पहले आधिकारिक पोर्टल/बैंक से जानकारी ज़रूर जाँचें।"
CTA_SLIDE = "प्रोजेक्ट रिपोर्ट (DPR) और पूरी मदद के लिए UdyamCare से जुड़ें"
CTA_VOICE = " प्रोजेक्ट रिपोर्ट और पूरी जानकारी के लिए, उद्यम केयर से जुड़ें।"


def finalize_bank_item(item, slot):
    c = dict(item)
    c["slides"] = list(item["slides"])
    if not c.get("_cta_done"):
        c["voiceover"] = item["voiceover"].rstrip() + CTA_VOICE
    c["hashtags"] = list(dict.fromkeys(item.get("tags", []) + COMMON_TAGS))
    c["_slot"] = slot
    return c


def caption_text(c):
    body = c["caption"].strip()
    if c.get("_slot") in ("loan", "registration"):
        body += "\n\n" + DISCLAIMER
    return body + "\n\n" + " ".join(c["hashtags"])


def fb_ok(r, what):
    """raise_for_status, but include Facebook's error message (never contains our token)."""
    if r.ok:
        return r
    try:
        err = r.json().get("error", {})
        detail = f"code={err.get('code')} subcode={err.get('error_subcode')} type={err.get('type')} message={err.get('message')}"
    except Exception:
        detail = r.text[:500]
    raise RuntimeError(f"Facebook {what} failed (HTTP {r.status_code}): {detail}")


def fb_post_image(path, caption):
    r = requests.post(f"{GRAPH}/{os.environ['FB_PAGE_ID']}/photos", data={"caption": caption, "access_token": os.environ["FB_PAGE_TOKEN"]}, files={"source": open(path, "rb")}, timeout=120)
    fb_ok(r, "photo upload")
    return r.json()


def fb_post_reel(path, caption):
    page, token = os.environ["FB_PAGE_ID"], os.environ["FB_PAGE_TOKEN"]
    r = requests.post(f"{GRAPH}/{page}/video_reels", data={"upload_phase": "start", "access_token": token}, timeout=60)
    fb_ok(r, "reel start")
    start = r.json()
    data = Path(path).read_bytes()
    up = requests.post(start["upload_url"], headers={"Authorization": f"OAuth {token}", "offset": "0", "file_size": str(len(data))}, data=data, timeout=300)
    fb_ok(up, "reel upload")
    fin = requests.post(f"{GRAPH}/{page}/video_reels", data={"upload_phase": "finish", "video_id": start["video_id"], "video_state": "PUBLISHED", "description": caption, "access_token": token}, timeout=60)
    fb_ok(fin, "reel finish")
    return fin.json()


def check_fb():
    """Print what FB_PAGE_TOKEN and FB_PAGE_ID actually are (never prints the token)."""
    page_id, token = os.environ["FB_PAGE_ID"], os.environ["FB_PAGE_TOKEN"]
    log(f"FB_PAGE_ID looks numeric: {page_id.strip().isdigit()}; has whitespace/quotes: {page_id != page_id.strip().strip(chr(34)).strip(chr(39))}")
    log(f"FB_PAGE_TOKEN length: {len(token)}; has whitespace/quotes: {token != token.strip().strip(chr(34)).strip(chr(39))}")
    me = requests.get(f"{GRAPH}/me", params={"fields": "id,name", "access_token": token}, timeout=30)
    log(f"/me -> HTTP {me.status_code}: {me.text[:300]}")
    try:
        me_id = me.json().get("id")
    except Exception:
        me_id = None
    log(f"Token belongs to the same ID as FB_PAGE_ID: {me_id == page_id.strip()}")
    pg = requests.get(f"{GRAPH}/{page_id.strip()}", params={"fields": "id,name,category", "access_token": token}, timeout=30)
    log(f"/FB_PAGE_ID -> HTTP {pg.status_code}: {pg.text[:300]}")
    acc = requests.get(f"{GRAPH}/me/accounts", params={"fields": "id,name", "access_token": token}, timeout=30)
    log(f"/me/accounts -> HTTP {acc.status_code}: {acc.text[:400]}")
    perms = requests.get(f"{GRAPH}/me/permissions", params={"access_token": token}, timeout=30)
    log(f"/me/permissions -> HTTP {perms.status_code}: {perms.text[:400]}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--slot", required=True, choices=["registration", "loan", "idea", "tips"])
    ap.add_argument("--kind", default="both", choices=["both", "reel", "image"])
    ap.add_argument("--source", default="auto", choices=["auto", "bank", "api"], help="auto: bank first, API once the bank is used up")
    ap.add_argument("--check-fb", action="store_true", help="diagnose FB_PAGE_ID / FB_PAGE_TOKEN, post nothing")
    ap.add_argument("--dry-run", action="store_true", help="no API calls / no Facebook posting; uses sample content")
    a = ap.parse_args()
    if a.check_fb:
        return check_fb()
    OUT.mkdir(exist_ok=True)
    if not features.check("raqm"):
        log("WARNING: libraqm missing - Devanagari conjuncts may render incorrectly")
    history = load_history()
    topic = None
    cycled = False
    item, cycled = pick_from_bank(a.slot, history) if a.source in ("auto", "bank") else (None, False)
    if item and not (cycled and a.source == "auto" and os.environ.get("ANTHROPIC_API_KEY") and not a.dry_run):
        content = finalize_bank_item(item, a.slot)
        topic = item.get("topic", item["title"])
        log(f"Source: content bank{' (cycled, all used once)' if cycled else ''}")
    elif a.dry_run:
        content = SAMPLE
        topic = "sample"
    else:
        topic = pick_topic(a.slot, history)
        try:
            content = generate_content(a.slot, topic, [h["title"] for h in history[-30:]])
            log("Source: Claude API")
        except Exception as e:
            if not item:
                raise
            log(f"Claude API failed ({type(e).__name__}); falling back to content bank")
            content = finalize_bank_item(item, a.slot)
            topic = item.get("topic", item["title"])
    palette = random.choice(PALETTES)
    cap = caption_text(content)
    log(f"Topic: {topic}\nTitle: {content['title']}")
    results = {}
    if a.kind in ("both", "image"):
        img = make_image_post(content, palette)
        results["image"] = "dry-run" if a.dry_run else fb_post_image(img, cap)
    if a.kind in ("both", "reel"):
        vid = make_reel(content, palette, a.dry_run)
        results["reel"] = "dry-run" if a.dry_run else fb_post_reel(vid, cap)
    log(json.dumps(results, ensure_ascii=False))
    if not a.dry_run:
        history.append({"slot": a.slot, "topic": topic, "title": content["title"]})
        HISTORY.write_text(json.dumps(history[-600:], ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()

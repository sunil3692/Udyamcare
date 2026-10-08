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
    "slides": ["PMEGP से पाएं 25 लाख तक का लोन", "सामान्य वर्ग को 15–25% सब्सिडी", "विशेष वर्ग को 25–35% सब्सिडी", "KVIC पोर्टल पर ऑनलाइन आवेदन करें", "DPR बनवाएं – UdyamCare के साथ"],
    "voiceover": "क्या आप अपना बिज़नेस शुरू करना चाहते हैं? पी एम ई जी पी योजना में पच्चीस लाख तक का लोन मिलता है। सामान्य वर्ग को पंद्रह से पच्चीस प्रतिशत सब्सिडी मिलती है। आवेदन के लिए के वी आई सी पोर्टल पर जाएं और प्रोजेक्ट रिपोर्ट तैयार रखें। अधिक जानकारी के लिए यूद्यमकेयर से जुड़ें।",
    "caption": "PMEGP लोन से अपना बिज़नेस शुरू करें! 💼\nDPR और पूरी जानकारी के लिए UdyamCare से जुड़ें।",
    "hashtags": ["#PMEGP", "#MSME", "#Udyam", "#BusinessLoan", "#UdyamCare"],
}


def log(*a):
    print(*a, flush=True)


def load_history():
    try:
        return json.loads(HISTORY.read_text())
    except Exception:
        return []


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


def _fc_font():
    try:
        r = subprocess.run(["fc-match", "-f", "%{file}", "Noto Sans Devanagari:bold"], capture_output=True, text=True)
        return [r.stdout.strip()] if r.stdout.strip() else []
    except Exception:
        return []


def get_font(size):
    for p in _fc_font() + FONT_CANDIDATES:
        if Path(p).exists():
            try:
                return ImageFont.truetype(p, size, layout_engine=ImageFont.Layout.RAQM if features.check("raqm") else ImageFont.Layout.BASIC)
            except Exception:
                continue
    return ImageFont.load_default()


def wrap(draw, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if draw.textlength(t, font=font) <= max_w:
            cur = t
        else:
            lines.append(cur)
            cur = w
    return lines + [cur] if cur else lines


def make_card(text, idx, total, palette, path, title=None, size=(W, H)):
    w, h = size
    top, bot = palette
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        t = y / h
        c = tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    d = ImageDraw.Draw(img)
    d.text((60, 70 * h // H), "UdyamCare", font=get_font(56 * w // W), fill=(255, 255, 255, 230))
    if title and idx == 0:
        f = get_font(92 * w // W)
        lines = wrap(d, title, f, w - 160)
        text_block = lines
    else:
        f = get_font(84 * w // W)
        text_block = wrap(d, text, f, w - 160)
    line_h = int(f.size * 1.5)
    y = (h - line_h * len(text_block)) // 2
    for ln in text_block:
        tw = d.textlength(ln, font=f)
        d.text(((w - tw) / 2, y), ln, font=f, fill="white")
        y += line_h
    if total > 1:
        d.text((60, h - 110 * h // H), f"{idx + 1}/{total}", font=get_font(44 * w // W), fill=(255, 255, 255))
    img.save(path)
    return path


def make_image_post(content, palette):
    path = OUT / "post.png"
    body = "\n".join(content["slides"][:4])
    make_card(content["title"] + "\n" + "\n".join("• " + s for s in content["slides"][:4]), 0, 1, palette, path, size=(1080, 1080))
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
    slides = [content["title"]] + content["slides"]
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


def caption_text(c):
    return c["caption"].strip() + "\n\n" + " ".join(c["hashtags"])


def fb_post_image(path, caption):
    r = requests.post(f"{GRAPH}/{os.environ['FB_PAGE_ID']}/photos", data={"caption": caption, "access_token": os.environ["FB_PAGE_TOKEN"]}, files={"source": open(path, "rb")}, timeout=120)
    r.raise_for_status()
    return r.json()


def fb_post_reel(path, caption):
    page, token = os.environ["FB_PAGE_ID"], os.environ["FB_PAGE_TOKEN"]
    r = requests.post(f"{GRAPH}/{page}/video_reels", data={"upload_phase": "start", "access_token": token}, timeout=60)
    r.raise_for_status()
    start = r.json()
    data = Path(path).read_bytes()
    up = requests.post(start["upload_url"], headers={"Authorization": f"OAuth {token}", "offset": "0", "file_size": str(len(data))}, data=data, timeout=300)
    up.raise_for_status()
    fin = requests.post(f"{GRAPH}/{page}/video_reels", data={"upload_phase": "finish", "video_id": start["video_id"], "video_state": "PUBLISHED", "description": caption, "access_token": token}, timeout=60)
    fin.raise_for_status()
    return fin.json()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--slot", required=True, choices=["registration", "loan", "idea", "tips"])
    ap.add_argument("--kind", default="both", choices=["both", "reel", "image"])
    ap.add_argument("--dry-run", action="store_true", help="no API calls / no Facebook posting; uses sample content")
    a = ap.parse_args()
    OUT.mkdir(exist_ok=True)
    if not features.check("raqm"):
        log("WARNING: libraqm missing - Devanagari conjuncts may render incorrectly")
    history = load_history()
    topic = pick_topic(a.slot, history)
    content = SAMPLE if a.dry_run else generate_content(a.slot, topic, [h["title"] for h in history[-30:]])
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
        HISTORY.write_text(json.dumps(history[-200:], ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()

# UdyamCare Auto Poster (Reels + Image)

Roz 4 slot (8:30 AM, 12:30 PM, 4:30 PM, 8:30 PM IST) par apne aap:
**content bank** (`content_bank/*.json`, 118 pehle se likhe Hindi posts) se agla unused post chunna -> image + voiceover wali reel (ffmpeg + edge-tts) -> Facebook Page par post.
Kul: **4 reels + 4 image posts roz**. Topics `topics.json` me badal sakte hain.

## Content kahan se aata hai (Claude API ka paisa nahi lagta)
- `content_bank/registration.json`, `loan.json`, `idea.json`, `tips.json` me har slot ke ~30 posts hain (title, 4 slides, voiceover, caption, tags).
- Script har slot me agla *unused* post uthati hai (`history.json` me record hota hai). Sab use ho jayein to wahi se dobara shuru karti hai.
- Naye posts chahiye to bank me items jodo (same format, unique `title`), ya Claude se naya bank banwa lo.
- Optional: `ANTHROPIC_API_KEY` set ho aur bank khatam ho chuka ho, to script naya content API se banane ki koshish karti hai; fail ho to bank me wapas aati hai.
- Har reel ke end me CTA slide, voiceover me CTA, aur loan/registration posts me "niyam badalte rehte hain" wali disclaimer line apne aap judti hai.
- **Facts check karte rehna:** yojanaon ke amount/niyam badal sakte hain (PMEGP, Mudra, GST etc.). Bank ke posts ek baar apne hisaab se padh lo.

## Setup (ek baar)
GitHub repo -> Settings -> Secrets and variables -> Actions -> New secret:

| Secret | Kya hai |
|---|---|
| `ANTHROPIC_API_KEY` | *Optional* - sirf tab jab bank khatam ho jaye aur naya content API se banwana ho |
| `FB_PAGE_ID` | Facebook Page ka numeric ID |
| `FB_PAGE_TOKEN` | **Long-lived Page access token** (permissions: `pages_manage_posts`, `pages_read_engagement`, `pages_show_list`, `publish_video`) |

Page token: developers.facebook.com -> App banayein -> Graph API Explorer -> User token (upar ki permissions) -> long-lived banayein -> `/me/accounts` se Page token lein.

## Test
Actions -> "UdyamCare Reels + Image Poster" -> Run workflow (dry_run = true: post nahi hoga, sirf media banega, artifact me download karke dekhein). Phir dry_run = false se ek asli test post.

Local: `pip install -r requirements.txt && python post.py --slot loan --dry-run`

## Dhyan rakhein
- Purana `udyamcare-auto-poster.yml` bhi same slots par chalta hai; yeh naya workflow 30 min baad chalta hai. Duplicate nahi chahiye to ek band karein.
- Facebook Reel: 9:16, 3–90 sec. App ko Live mode me rakhna padta hai warna post sirf admins ko dikhegi.
- GitHub schedule kabhi 5–30 min late ho sakta hai (workflow 1 ghante ka window handle karta hai).

# UdyamCare — Project Report (DPR) Generator

Thodi si details bharo aur bank-ready **Detailed Project Report (DPR)** ban jaati hai — PMEGP, CM YUVA (UP), MUDRA,
CGTMSE, Stand-Up India ya normal bank loan ke liye.

## Kaise chalayein
`index.html` ko browser me kholiye (koi server / install nahi chahiye). Kisi bhi static hosting par
(GitHub Pages, Floot `static/`, Netlify) yeh folder as-it-is upload kar sakte hain.

1. **Details bharein** — promoter, business, scheme, machinery list, products (capacity + price), staff.
   Baaki sab fields me default bhare hue hain. "Sample" buttons se demo data load karke dekh sakte hain.
2. **Generate Project Report** — puri DPR ban jaati hai.
3. **Print / Save as PDF** — browser ke print dialog se A4 PDF save karein.

Data browser me auto-save hota hai; `Save data (.json)` se file download karke baad me `Load data` se wapas la sakte hain.

## Report me kya-kya aata hai
Cover page, Project at a glance, Promoter profile, Introduction, Market potential & marketing strategy,
Process, Location & infrastructure, Machinery list, Manpower, Capacity & sales projection, Cost of project
(amount in words), Means of finance (PMEGP subsidy auto), Working capital assessment, Projected P&L,
Balance sheet, Cash flow, Depreciation (WDV), Term-loan repayment schedule, DSCR, Break-even, Key ratios
(current ratio, D/E, ROCE, payback, project IRR), SWOT, Statutory approvals, Assumptions, Conclusion.

## Calculation rules (short)
- **Scheme defaults:** PMEGP own contribution 10% (General) / 5% (special category); subsidy 15/25% (General
  urban/rural), 25/35% (special category urban/rural). MUDRA 10%, CGTMSE & Stand-Up 15%, others 20%.
- **CM YUVA (UP):** own margin General 15%, OBC 12.5%, SC/ST/Divyang or aspirational district 10%;
  margin money subsidy 10% of project cost (max ₹50,000); 100% interest subsidy for 4 years (shown as income);
  loan 4 years incl. 6 months moratorium; warning if loan exceeds ₹5 lakh or age is outside 21–40.
- **Fixed amounts:** term loan / WC loan / WC requirement can be entered as fixed by the bank; own contribution
  then becomes the balance of the project cost.
- **Term loan:** interest serviced during moratorium, then equal monthly principal instalments.
  PMEGP subsidy kept as TDR — no interest on that portion, adjusted against the loan after 3 years.
- **Working capital:** operating-cycle method (stock, finished goods, debtors less creditors); bank finances
  the same share as the term loan.
- **Depreciation:** WDV at Income-tax rates. Pre-operative expenses + contingency written off over 5 years.
- **Tax:** proprietorship — new-regime slabs with 87A rebate; firm/LLP 30%; Pvt Ltd 115BAA; +4% cess; losses carried forward.
- Balance sheet always tallies (cash is derived from the cash-flow statement).

Files: `calc.js` (financial model, also runs in Node), `report.js` (report layout), `app.js` (form), `style.css`.

/* UdyamCare DPR Generator — renders the computed model into a bank-ready Detailed Project Report. */
(function (root) {
  'use strict';

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function inr(n) { return '₹' + Math.round(n || 0).toLocaleString('en-IN'); }
  function lakh(n) {
    var v = (n || 0) / 100000;
    if (Math.abs(v) < 0.005) return '0.00';
    var s = Math.abs(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return v < 0 ? '(' + s + ')' : s;
  }
  function pct(f, dp) { return f === null || f === undefined ? '—' : (f * 100).toFixed(dp == null ? 2 : dp) + '%'; }
  function ratio(r) { return r === null || r === undefined ? '—' : r.toFixed(2); }
  function qty(n) { return Math.round(n).toLocaleString('en-IN'); }

  var ONES = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve',
    'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
  var TENS = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
  function twoDigits(n) { return n < 20 ? ONES[n] : TENS[Math.floor(n / 10)] + (n % 10 ? ' ' + ONES[n % 10] : ''); }
  function threeDigits(n) {
    var h = Math.floor(n / 100), r = n % 100;
    return (h ? ONES[h] + ' Hundred' + (r ? ' ' : '') : '') + (r ? twoDigits(r) : '');
  }
  function inWords(amount) {
    var n = Math.round(amount);
    if (n === 0) return 'Rupees Zero Only';
    var parts = [];
    var crore = Math.floor(n / 10000000); n %= 10000000;
    var lakhs = Math.floor(n / 100000); n %= 100000;
    var thousand = Math.floor(n / 1000); n %= 1000;
    if (crore) parts.push(threeDigits(crore) + ' Crore');
    if (lakhs) parts.push(twoDigits(lakhs) + ' Lakh');
    if (thousand) parts.push(twoDigits(thousand) + ' Thousand');
    if (n) parts.push(threeDigits(n));
    return 'Rupees ' + parts.join(' ') + ' Only';
  }

  var SCHEME_NAMES = {
    PMEGP: "Prime Minister's Employment Generation Programme (PMEGP)",
    MUDRA: 'Pradhan Mantri MUDRA Yojana (PMMY)',
    CGTMSE: 'MSME Term Loan under CGTMSE (collateral-free)',
    STANDUP: 'Stand-Up India Scheme',
    OTHER: 'Bank Term Loan & Working Capital'
  };

  function table(head, rows, opts) {
    opts = opts || {};
    var h = '<table class="fin' + (opts.cls ? ' ' + opts.cls : '') + '"><thead><tr>' +
      head.map(function (c) { return '<th>' + c + '</th>'; }).join('') + '</tr></thead><tbody>';
    rows.forEach(function (r) {
      var cls = '';
      if (r && r.cls) { cls = ' class="' + r.cls + '"'; r = r.cells; }
      h += '<tr' + cls + '>' + r.map(function (c) { return '<td>' + c + '</td>'; }).join('') + '</tr>';
    });
    return h + '</tbody></table>';
  }
  function kv(rows) {
    return '<table class="kv"><tbody>' + rows.filter(function (r) { return r[1] !== '' && r[1] != null; })
      .map(function (r) { return '<tr><th>' + r[0] + '</th><td>' + r[1] + '</td></tr>'; }).join('') + '</tbody></table>';
  }
  function yearHead(m, first) {
    return [first || 'Particulars'].concat(m.years.map(function (Y) { return 'Year ' + Y.year; }));
  }
  function yRow(m, label, fn, cls) {
    var cells = [label].concat(m.years.map(function (Y) { return fn(Y); }));
    return cls ? { cls: cls, cells: cells } : cells;
  }

  function defaultMarket(d) {
    var a = d.activity;
    var p = esc(d.productLine);
    var place = esc([d.district, d.state].filter(Boolean).join(', ')) || 'the proposed area';
    if (a === 'Service') {
      return 'Demand for ' + p + ' is growing steadily in ' + place + ' due to rising incomes, urbanisation and changing ' +
        'lifestyle. Customers today prefer reliable, nearby and quality service providers. At present the area is served ' +
        'mostly by small unorganised players, which leaves good scope for a well-equipped, professionally run unit. ' +
        'The promoter expects to build a regular customer base through quality service, fair pricing and word of mouth.';
    }
    if (a === 'Trading') {
      return 'There is consistent daily demand for ' + p + ' in ' + place + ' and nearby villages/towns from households, ' +
        'retailers and institutional buyers. Growth in population and purchasing power is increasing consumption year after ' +
        'year. The unit will source directly from manufacturers/wholesalers and offer competitive prices and timely supply, ' +
        'which will help it capture a good share of the local market.';
    }
    return 'The products — ' + p + ' — are items of regular and growing demand. Consumption in ' + place + ' and the ' +
      'surrounding market is rising with population growth, urbanisation and a shift towards packaged and quality products. ' +
      'Local supply is presently met largely by small unorganised units and by supplies from outside the district, which ' +
      'adds transport cost. A local unit with modern machinery can supply fresh, quality products at competitive rates. ' +
      'The proposed capacity is modest compared to total demand, so the unit should be able to sell its output comfortably.';
  }
  function defaultProcess(d) {
    if (d.activity === 'Service') {
      return 'Customer enquiry / appointment → understanding requirement → service delivery using the proposed equipment ' +
        'by trained staff → quality check & customer satisfaction → billing and follow-up for repeat business. ' +
        'Proper hygiene, safety and record keeping will be maintained.';
    }
    if (d.activity === 'Trading') {
      return 'Assessment of demand → purchase of goods from manufacturers / authorised wholesalers → receipt, quality ' +
        'check and storage → display and sale to retail & institutional customers → billing (GST compliant), delivery ' +
        'and collection. Stock levels will be monitored regularly to avoid shortages and dead stock.';
    }
    return 'Procurement of quality raw material → cleaning / sorting → processing on the proposed machinery → quality ' +
      'inspection → packing & labelling → storage → dispatch to distributors, retailers and direct customers. ' +
      'The process is well established, the technology is simple and machinery is easily available from domestic suppliers.';
  }
  function defaultMarketing(d) {
    return 'Direct sale to local customers, tie-ups with retailers / dealers and institutional buyers, competitive pricing, ' +
      'quality assurance, timely delivery, signboards and local advertising, and use of social media / WhatsApp Business ' +
      'and online listings (Google Business Profile, GeM / ONDC where applicable) to reach new customers.';
  }
  function approvals(d) {
    var list = ['Udyam Registration (MSME)', 'GST Registration (if turnover exceeds threshold / for B2B sales)',
      'Shop & Establishment / Trade Licence from local body', 'Current account with the financing bank'];
    if (d.activity === 'Manufacturing') {
      list.push('Consent to Establish / Operate from State Pollution Control Board (if applicable)',
        'Electricity connection (commercial / industrial load) from DISCOM');
    }
    if (/food|atta|flour|besan|masala|spice|bakery|sweet|namkeen|dairy|milk|oil|pickle|papad|restaurant|dhaba|tiffin|catering|rice|dal/i.test(d.productLine)) {
      list.push('FSSAI Registration / Licence');
    }
    if (d.scheme === 'PMEGP') list.push('EDP training certificate (as required under PMEGP)');
    return list;
  }

  function render(m) {
    var d = m.d, Y1 = m.years[0];
    var h = [];
    var schemeName = SCHEME_NAMES[d.scheme] || SCHEME_NAMES.OTHER;
    var location = [d.unitAddress, d.district, d.state].filter(Boolean).join(', ');
    var employment = d.staff.reduce(function (s, st) { return s + st.count; }, 0) + 1;
    var sec = 0;
    function H(title) { sec++; return '<h2><span>' + sec + '.</span> ' + title + '</h2>'; }

    // ---------- Cover ----------
    h.push('<section class="cover">',
      '<div class="cover-tag">DETAILED PROJECT REPORT</div>',
      '<h1>' + esc(d.unitName) + '</h1>',
      '<div class="cover-sub">' + esc(d.productLine) + '</div>',
      '<div class="cover-box">',
      kv([
        ['Promoter', esc(d.applicantName)],
        ['Constitution', esc(d.constitution)],
        ['Activity', esc(d.activity)],
        ['Location', esc(location)],
        ['Scheme', esc(schemeName)],
        ['Total project cost', inr(m.totalCost) + ' (₹' + lakh(m.totalCost) + ' lakh)'],
        ['Bank finance required', inr(m.bankLoan) + ' (₹' + lakh(m.bankLoan) + ' lakh)'],
        ['Submitted to', esc([d.bankName, d.bankBranch].filter(Boolean).join(', '))]
      ]),
      '</div>',
      '<div class="cover-foot">Prepared with UdyamCare · ' + new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' }) + '</div>',
      '</section>');

    // ---------- 1. Project at a glance ----------
    h.push('<section>', H('Project at a Glance'), kv([
      ['Name of the unit', esc(d.unitName)],
      ['Name of the promoter', esc(d.applicantName)],
      ['Constitution', esc(d.constitution)],
      ['Nature of activity', esc(d.activity) + ' — ' + esc(d.productLine)],
      ['Location of unit', esc(location) + (d.area ? ' (' + esc(d.area) + ' area)' : '')],
      ['Udyam Registration No.', esc(d.udyamNo) || 'Applied / to be obtained'],
      ['Scheme', esc(schemeName)],
      ['Cost of project', inr(m.totalCost)],
      ['Promoter\'s contribution', inr(m.own) + ' (' + m.ownPct + '%)'],
      ['Term loan', inr(m.termLoan)],
      ['Working capital loan (CC)', inr(m.wcLoan)],
      m.subsidy ? ['Margin money subsidy (PMEGP)', inr(m.subsidy) + ' (' + d.subsidyPct + '% of project cost)'] : ['', ''],
      d.scheme === 'MUDRA' ? ['MUDRA category', m.mudraCategory] : ['', ''],
      ['Repayment', d.tenureYears + ' years incl. ' + d.moratorium + ' months moratorium'],
      ['Employment generation', employment + ' persons (including promoter)'],
      ['Sales in Year 1 / Year ' + m.N, inr(Y1.sales) + ' / ' + inr(m.years[m.N - 1].sales)],
      ['Net profit in Year 1', inr(Y1.pat)],
      ['Average DSCR', ratio(m.avgDscr)],
      ['Break-even point (Year 1)', pct(Y1.bepCapacity) + ' of installed capacity'],
      ['Payback period', m.payback !== null ? m.payback.toFixed(1) + ' years' : 'Beyond projection period'],
      ['Project IRR (post-tax)', m.projectIrr !== null ? pct(m.projectIrr) : '—']
    ]), '</section>');

    // ---------- 2. Promoter ----------
    h.push('<section>', H('Promoter Profile'), kv([
      ['Name', esc(d.applicantName)],
      ["Father's / Husband's name", esc(d.fatherName)],
      ['Age', d.age ? d.age + ' years' : ''],
      ['Gender', esc(d.gender)],
      ['Social category', esc(d.category)],
      ['Educational qualification', esc(d.qualification)],
      ['Experience', esc(d.experience)],
      ['Residential address', esc(d.resAddress)],
      ['Mobile', esc(d.mobile)],
      ['Email', esc(d.email)],
      ['PAN', esc((d.pan || '').toUpperCase())]
    ]),
    '<p>' + esc(d.applicantName) + (d.qualification ? ', educated up to ' + esc(d.qualification) + ',' : '') +
    ' is a first-generation entrepreneur' + (d.experience ? ' with experience in ' + esc(d.experience) : '') +
    '. The promoter has studied the market for ' + esc(d.productLine) + ' and has the required skills and commitment to ' +
    'run the unit successfully. The promoter will look after day-to-day management, purchase, production and marketing.</p>',
    '</section>');

    // ---------- 3. Introduction ----------
    h.push('<section>', H('Introduction &amp; Business Description'),
      '<p>The promoter proposes to set up a <b>' + esc(d.activity.toLowerCase()) + '</b> unit named <b>' + esc(d.unitName) +
      '</b> for <b>' + esc(d.productLine) + '</b> at ' + esc(location || 'the proposed location') + '. ' +
      'The unit will be registered as a Micro Enterprise under the MSMED Act, 2006 (Udyam). The project is proposed to be ' +
      'financed under the <b>' + esc(schemeName) + '</b>' + (d.bankName ? ' through ' + esc(d.bankName) : '') + '.</p>',
      '<p>The project will generate direct employment for ' + employment + ' persons and indirect employment in raw-material ' +
      'supply, transport and marketing. The total cost of the project is <b>' + inr(m.totalCost) + '</b> (' + inWords(m.totalCost) + ').</p>',
      '</section>');

    // ---------- 4. Market ----------
    h.push('<section>', H('Market Potential'), '<p>' + (d.marketText ? esc(d.marketText) : defaultMarket(d)) + '</p>',
      '<h3>Marketing strategy</h3><p>' + (d.marketingText ? esc(d.marketingText) : defaultMarketing(d)) + '</p>', '</section>');

    // ---------- 5. Process ----------
    h.push('<section>', H(d.activity === 'Manufacturing' ? 'Manufacturing Process' : d.activity === 'Service' ? 'Service Process' : 'Business Process'),
      '<p>' + (d.processText ? esc(d.processText) : defaultProcess(d)) + '</p>', '</section>');

    // ---------- 6. Location & infrastructure ----------
    h.push('<section>', H('Location &amp; Infrastructure'), kv([
      ['Address of unit', esc(location)],
      ['Area', esc(d.area)],
      ['Premises', esc(d.premises) + (d.premisesArea ? ' — ' + qty(d.premisesArea) + ' sq. ft.' : '')],
      ['Rent', d.rent ? inr(d.rent) + ' per month' : 'Not applicable'],
      ['Power', d.power ? 'Approx. ' + inr(d.power) + ' per month in Year 1' : 'As required'],
      ['Water', 'Available locally'],
      ['Transport &amp; communication', 'Unit is well connected by road; mobile & internet available'],
      ['Proposed commencement', d.startDate ? new Date(d.startDate + '-01').toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }) : 'Within 3 months of loan sanction']
    ]), '</section>');

    // ---------- 7. Machinery ----------
    var mRows = d.machinery.map(function (mc, i) {
      return [i + 1, esc(mc.name) + (mc.supplier ? '<br><small>' + esc(mc.supplier) + '</small>' : ''), qty(mc.qty), inr(mc.rate), inr(mc.qty * mc.rate)];
    });
    mRows.push({ cls: 'total', cells: ['', 'Total', '', '', inr(m.machineryCost)] });
    h.push('<section>', H('Plant &amp; Machinery / Equipment'),
      table(['S.No.', 'Description', 'Qty', 'Rate', 'Amount'], mRows, { cls: 'num-right' }),
      '<p class="note">Rates are as per quotations obtained from suppliers and include GST, transport and installation where applicable.</p>',
      '</section>');

    // ---------- 8. Manpower ----------
    var sRows = d.staff.map(function (s, i) { return [i + 1, esc(s.role), qty(s.count), inr(s.salary), inr(s.count * s.salary), inr(s.count * s.salary * 12)]; });
    var mw = d.staff.reduce(function (s, st) { return s + st.count * st.salary; }, 0);
    sRows.push({ cls: 'total', cells: ['', 'Total', qty(employment - 1), '', inr(mw), inr(mw * 12)] });
    h.push('<section>', H('Manpower Requirement'),
      d.staff.length ? table(['S.No.', 'Designation', 'No.', 'Salary / month', 'Monthly', 'Annual'], sRows, { cls: 'num-right' }) :
        '<p>The unit will be run by the promoter with family support; no paid staff is proposed in the initial years.</p>',
      '<p class="note">Promoter will manage the unit full-time. Salaries are escalated by ' + d.salaryInc + '% per year in the projections.</p>',
      '</section>');

    // ---------- 9. Capacity & sales ----------
    var cap = d.products.map(function (p) { return [esc(p.name), esc(p.unit), qty(p.capacity), inr(p.price), inr(p.rmCost)]; });
    var saleRows = [];
    d.products.forEach(function (p, pi) {
      saleRows.push(yRow(m, esc(p.name) + ' — qty (' + esc(p.unit) + ')', function (Y) { return qty(Y.products[pi].qty); }));
      saleRows.push(yRow(m, esc(p.name) + ' — sales (₹ lakh)', function (Y) { return lakh(Y.products[pi].sales); }));
    });
    saleRows.push(yRow(m, 'Total sales (₹ lakh)', function (Y) { return lakh(Y.sales); }, 'total'));
    h.push('<section>', H('Installed Capacity &amp; Sales Projection'),
      table(['Product / service', 'Unit', 'Capacity / year', 'Selling price', 'Raw material / unit'], cap, { cls: 'num-right' }),
      '<p>Capacity utilisation assumed: ' + m.years.map(function (Y) { return 'Y' + Y.year + ' ' + Math.round(Y.util * 100) + '%'; }).join(', ') +
      '. Selling price escalation ' + d.priceEsc + '% p.a.; cost escalation ' + d.costEsc + '% p.a.</p>',
      table(yearHead(m), [yRow(m, 'Capacity utilisation', function (Y) { return Math.round(Y.util * 100) + '%'; })].concat(saleRows), { cls: 'num-right' }),
      '</section>');

    // ---------- 10. Cost of project ----------
    var costRows = [];
    Object.keys(m.assets).forEach(function (k) {
      if (m.assets[k] > 0) costRows.push([m.assetLabels[k], inr(m.assets[k]), lakh(m.assets[k])]);
    });
    if (d.preop) costRows.push(['Pre-operative expenses', inr(d.preop), lakh(d.preop)]);
    if (m.contingency) costRows.push(['Contingency @ ' + d.contingencyPct + '%', inr(m.contingency), lakh(m.contingency)]);
    costRows.push({ cls: 'sub', cells: ['Total fixed capital', inr(m.fixedAssets + m.preopTotal), lakh(m.fixedAssets + m.preopTotal)] });
    costRows.push(['Working capital requirement', inr(m.wcRequirement), lakh(m.wcRequirement)]);
    costRows.push({ cls: 'total', cells: ['Total cost of project', inr(m.totalCost), lakh(m.totalCost)] });
    h.push('<section>', H('Cost of Project'), table(['Particulars', 'Amount (₹)', '₹ lakh'], costRows, { cls: 'num-right' }),
      '<p class="note">' + inWords(m.totalCost) + '</p>', '</section>');

    // ---------- 11. Means of finance ----------
    var mf = [
      ["Promoter's contribution (" + m.ownPct + '%)', inr(m.own), lakh(m.own)],
      ['Term loan from bank', inr(m.termLoan), lakh(m.termLoan)],
      ['Working capital loan (Cash Credit)', inr(m.wcLoan), lakh(m.wcLoan)],
      { cls: 'total', cells: ['Total', inr(m.totalCost), lakh(m.totalCost)] }
    ];
    h.push('<section>', H('Means of Finance'), table(['Source', 'Amount (₹)', '₹ lakh'], mf, { cls: 'num-right' }));
    if (m.subsidy) {
      h.push('<p><b>PMEGP margin money subsidy:</b> ' + inr(m.subsidy) + ' @ ' + d.subsidyPct + '% of project cost (' +
        esc(d.category) + ', ' + esc(d.gender) + ', ' + esc(d.area) + ' area). The subsidy will be kept as Term Deposit ' +
        'Receipt (TDR) in the name of the beneficiary for 3 years; no interest is charged on the loan to the extent of the TDR, ' +
        'and it is adjusted against the term loan after the lock-in period, as per KVIC guidelines.</p>');
    }
    h.push('</section>');

    // ---------- 12. Working capital ----------
    var wcRows = [
      ['Raw material / stock (' + d.rmDays + ' days)', inr(Y1.rmStock)],
      ['Finished goods (' + d.fgDays + ' days)', inr(Y1.fgStock)],
      ['Debtors / receivables (' + d.debtorDays + ' days)', inr(Y1.debtors)],
      { cls: 'sub', cells: ['Total current assets', inr(Y1.currentAssets)] },
      ['Less: Sundry creditors (' + d.creditorDays + ' days)', inr(Y1.creditors)],
      { cls: 'sub', cells: ['Net working capital (operating cycle)', inr(Y1.netWC)] },
      m.d.wcOverride > 0 ? { cls: 'sub', cells: ['Working capital requirement (as assessed)', inr(m.wcRequirement)] } : null,
      ["Promoter's margin" + (m.fixedLoans ? '' : ' (' + m.ownPct + '%)'), inr(m.wcRequirement - m.wcLoan)],
      { cls: 'total', cells: ['Working capital loan (CC limit)', inr(m.wcLoan)] }
    ].filter(Boolean);
    h.push('<section>', H('Assessment of Working Capital (Year 1)'), table(['Particulars', 'Amount (₹)'], wcRows, { cls: 'num-right' }), '</section>');

    // ---------- 13. Cost of production & profitability ----------
    var pl = [
      yRow(m, 'Capacity utilisation', function (Y) { return Math.round(Y.util * 100) + '%'; }),
      yRow(m, '<b>A. Sales / Revenue</b>', function (Y) { return '<b>' + lakh(Y.sales) + '</b>'; }),
      yRow(m, 'Raw material / purchases', function (Y) { return lakh(Y.rm); }),
      yRow(m, 'Salaries &amp; wages', function (Y) { return lakh(Y.wages); }),
      yRow(m, 'Power &amp; fuel', function (Y) { return lakh(Y.power); }),
      yRow(m, 'Rent', function (Y) { return lakh(Y.rent); }),
      yRow(m, 'Repairs &amp; maintenance', function (Y) { return lakh(Y.repairs); }),
      yRow(m, 'Insurance', function (Y) { return lakh(Y.insurance); }),
      yRow(m, 'Selling &amp; administrative exp.', function (Y) { return lakh(Y.selling); }),
      yRow(m, 'Other expenses', function (Y) { return lakh(Y.other); }),
      yRow(m, 'B. Total operating cost', function (Y) { return lakh(Y.opex); }, 'sub'),
      yRow(m, 'C. EBITDA (A − B)', function (Y) { return lakh(Y.ebitda); }, 'sub'),
      yRow(m, 'Interest on term loan', function (Y) { return lakh(Y.intTL); }),
      yRow(m, 'Interest on working capital', function (Y) { return lakh(Y.intWC); }),
      yRow(m, 'Depreciation', function (Y) { return lakh(Y.dep); }),
      yRow(m, 'Pre-operative exp. written off', function (Y) { return lakh(Y.amort); }),
      yRow(m, 'D. Profit before tax', function (Y) { return lakh(Y.pbt); }, 'sub'),
      yRow(m, 'Income tax', function (Y) { return lakh(Y.tax); }),
      yRow(m, 'E. Net profit after tax', function (Y) { return lakh(Y.pat); }, 'total'),
      yRow(m, 'Cash accruals (PAT + Dep. + W/off)', function (Y) { return lakh(Y.cashAccruals); }),
      yRow(m, 'EBITDA margin', function (Y) { return pct(Y.ebitdaMargin, 1); }),
      yRow(m, 'Net profit margin', function (Y) { return pct(Y.npMargin, 1); })
    ];
    h.push('<section class="wide">', H('Projected Profitability Statement <small>(₹ in lakh)</small>'), table(yearHead(m), pl, { cls: 'num-right' }),
      '<p class="note">Tax computed as per ' + (d.constitution === 'Proprietorship' ? 'new tax regime slabs for individuals (incl. sec 87A rebate) + 4% cess' :
        d.constitution === 'Private Limited' ? 'sec 115BAA @ 22% + surcharge + cess' : 'firm/LLP rate @ 30% + 4% cess') + '; losses carried forward.</p>',
      '</section>');

    // ---------- 14. Balance sheet ----------
    var bs = [
      { cls: 'head', cells: ['<b>Liabilities</b>'].concat(m.years.map(function () { return ''; })) },
      yRow(m, "Promoter's capital (incl. retained profit)", function (Y) { return lakh(Y.capital); }),
      m.subsidy ? yRow(m, 'Capital subsidy (PMEGP)', function (Y) { return lakh(Y.subsidyReserve); }) : null,
      yRow(m, 'Term loan', function (Y) { return lakh(Y.tlClosing); }),
      yRow(m, 'Working capital loan', function (Y) { return lakh(Y.wcLoan); }),
      yRow(m, 'Sundry creditors', function (Y) { return lakh(Y.creditors); }),
      yRow(m, 'Total liabilities', function (Y) { return lakh(Y.totalLiabilities); }, 'total'),
      { cls: 'head', cells: ['<b>Assets</b>'].concat(m.years.map(function () { return ''; })) },
      yRow(m, 'Gross fixed assets', function (Y) { return lakh(Y.grossBlock); }),
      yRow(m, 'Less: accumulated depreciation', function (Y) { return lakh(Y.accDep); }),
      yRow(m, 'Net fixed assets', function (Y) { return lakh(Y.netBlock); }, 'sub'),
      yRow(m, 'Pre-operative exp. (not written off)', function (Y) { return lakh(Y.preopLeft); }),
      m.subsidy ? yRow(m, 'Subsidy TDR with bank', function (Y) { return lakh(Y.tdr); }) : null,
      yRow(m, 'Stock (raw material + finished goods)', function (Y) { return lakh(Y.rmStock + Y.fgStock); }),
      yRow(m, 'Sundry debtors', function (Y) { return lakh(Y.debtors); }),
      yRow(m, 'Cash &amp; bank balance', function (Y) { return lakh(Y.closingCash); }),
      yRow(m, 'Total assets', function (Y) { return lakh(Y.totalAssets); }, 'total')
    ].filter(Boolean);
    h.push('<section class="wide">', H('Projected Balance Sheet <small>(₹ in lakh, as at end of year)</small>'), table(yearHead(m), bs, { cls: 'num-right' }), '</section>');

    // ---------- 15. Cash flow ----------
    function cfVal(list, label) {
      for (var i = 0; i < list.length; i++) if (list[i][0] === label) return list[i][1];
      return 0;
    }
    var srcLabels = [], useLabels = [];
    m.years.forEach(function (Y) {
      Y.cfSources.forEach(function (r) { if (srcLabels.indexOf(r[0]) < 0) srcLabels.push(r[0]); });
      Y.cfUses.forEach(function (r) { if (useLabels.indexOf(r[0]) < 0) useLabels.push(r[0]); });
    });
    var cfRows = [{ cls: 'head', cells: ['<b>Sources of funds</b>'].concat(m.years.map(function () { return ''; })) }];
    srcLabels.forEach(function (l) { cfRows.push(yRow(m, l, function (Y) { return lakh(cfVal(Y.cfSources, l)); })); });
    cfRows.push(yRow(m, 'Total sources', function (Y) { return lakh(Y.totalSources); }, 'sub'));
    cfRows.push({ cls: 'head', cells: ['<b>Application of funds</b>'].concat(m.years.map(function () { return ''; })) });
    useLabels.forEach(function (l) { cfRows.push(yRow(m, l, function (Y) { return lakh(cfVal(Y.cfUses, l)); })); });
    cfRows.push(yRow(m, 'Total application', function (Y) { return lakh(Y.totalUses); }, 'sub'));
    cfRows.push(yRow(m, 'Opening cash balance', function (Y) { return lakh(Y.openingCash); }));
    cfRows.push(yRow(m, 'Net surplus / (deficit)', function (Y) { return lakh(Y.totalSources - Y.totalUses); }));
    cfRows.push(yRow(m, 'Closing cash balance', function (Y) { return lakh(Y.closingCash); }, 'total'));
    h.push('<section class="wide">', H('Projected Cash Flow Statement <small>(₹ in lakh)</small>'), table(yearHead(m), cfRows, { cls: 'num-right' }), '</section>');

    // ---------- 16. Depreciation ----------
    var depRows = m.depreciable.filter(function (k) { return m.assets[k] > 0; }).map(function (k) {
      return [m.assetLabels[k] + ' @ ' + Math.round(m.depRates[k] * 100) + '%'].concat(m.years.map(function (Y) { return lakh(Y.depByClass[k]); }));
    });
    depRows.push(yRow(m, 'Total depreciation', function (Y) { return lakh(Y.dep); }, 'total'));
    h.push('<section class="wide">', H('Depreciation Schedule <small>(WDV method, Income-tax rates, ₹ in lakh)</small>'), table(yearHead(m, 'Asset'), depRows, { cls: 'num-right' }), '</section>');

    // ---------- 17. Repayment ----------
    var lr = m.loan.rows.filter(function (r) { return r.opening > 0.5 || r.interest > 0.5; }).map(function (r) {
      return ['Year ' + r.year, inr(r.opening), inr(r.interest), inr(r.principal), m.subsidy ? inr(r.subsidyAdj) : null, inr(r.closing)]
        .filter(function (c) { return c !== null; });
    });
    var lrHead = ['Year', 'Opening balance', 'Interest', 'Principal repaid'].concat(m.subsidy ? ['Subsidy adjusted'] : []).concat(['Closing balance']);
    h.push('<section>', H('Term Loan Repayment Schedule'),
      '<p>Term loan of <b>' + inr(m.termLoan) + '</b> @ ' + d.tlRate + '% p.a., repayable in ' + m.loan.repayMonths +
      ' equal monthly instalments of <b>' + inr(m.loan.instalment) + '</b> (principal) after a moratorium of ' + m.loan.moratorium +
      ' months, interest to be serviced as and when charged. Total tenure ' + d.tenureYears + ' years.</p>',
      table(lrHead, lr, { cls: 'num-right' }), '</section>');

    // ---------- 18. DSCR ----------
    var ds = [
      yRow(m, 'Net profit after tax', function (Y) { return lakh(Y.pat); }),
      yRow(m, 'Add: Depreciation &amp; write-offs', function (Y) { return lakh(Y.dep + Y.amort); }),
      yRow(m, 'Add: Interest on term loan', function (Y) { return lakh(Y.intTL); }),
      yRow(m, 'A. Funds available for debt service', function (Y) { return lakh(Y.pat + Y.dep + Y.amort + Y.intTL); }, 'sub'),
      yRow(m, 'Interest on term loan', function (Y) { return lakh(Y.intTL); }),
      yRow(m, 'Repayment of term loan', function (Y) { return lakh(Y.tlRepaid); }),
      yRow(m, 'B. Total debt service', function (Y) { return lakh(Y.debtService); }, 'sub'),
      yRow(m, 'DSCR (A ÷ B)', function (Y) { return ratio(Y.dscr); }, 'total')
    ];
    h.push('<section class="wide">', H('Debt Service Coverage Ratio <small>(₹ in lakh)</small>'), table(yearHead(m), ds, { cls: 'num-right' }),
      '<p><b>Average DSCR: ' + ratio(m.avgDscr) + '</b> &nbsp;|&nbsp; Minimum DSCR: ' + ratio(m.minDscr) + '</p>', '</section>');

    // ---------- 19. Break-even ----------
    var be = [
      yRow(m, 'Sales', function (Y) { return lakh(Y.sales); }),
      yRow(m, 'Variable cost (raw material, power, selling)', function (Y) { return lakh(Y.variableCost); }),
      yRow(m, 'Contribution', function (Y) { return lakh(Y.contribution); }, 'sub'),
      yRow(m, 'Fixed cost (salary, rent, repairs, insurance, interest, dep.)', function (Y) { return lakh(Y.fixedCost); }),
      yRow(m, 'BEP as % of installed capacity', function (Y) { return pct(Y.bepCapacity); }, 'total'),
      yRow(m, 'Cash BEP as % of installed capacity', function (Y) { return pct(Y.cashBep); }),
      yRow(m, 'Break-even sales (₹ lakh)', function (Y) { return Y.bep !== null ? lakh(Y.bep * Y.sales) : '—'; })
    ];
    h.push('<section class="wide">', H('Break-Even Analysis <small>(₹ in lakh)</small>'), table(yearHead(m), be, { cls: 'num-right' }), '</section>');

    // ---------- 20. Ratios ----------
    var ra = [
      yRow(m, 'Current ratio', function (Y) { return ratio(Y.currentRatio); }),
      yRow(m, 'Debt : equity (term loan / net worth)', function (Y) { return ratio(Y.debtEquity); }),
      yRow(m, 'Net profit margin', function (Y) { return pct(Y.npMargin, 1); }),
      yRow(m, 'Return on capital employed', function (Y) { return pct(Y.roce, 1); }),
      yRow(m, 'DSCR', function (Y) { return ratio(Y.dscr); })
    ];
    h.push('<section class="wide">', H('Key Financial Indicators'), table(yearHead(m, 'Ratio'), ra, { cls: 'num-right' }),
      kv([['Average DSCR', ratio(m.avgDscr)], ['Payback period', m.payback !== null ? m.payback.toFixed(1) + ' years' : 'Beyond projection period'],
        ['Project IRR (post-tax)', m.projectIrr !== null ? pct(m.projectIrr) : '—']]), '</section>');

    // ---------- 21. SWOT ----------
    h.push('<section>', H('SWOT Analysis'), '<div class="swot">',
      '<div><h4>Strengths</h4><ul><li>Promoter\'s commitment' + (d.experience ? ' and experience in ' + esc(d.experience) : '') + '</li><li>Low overheads and local presence</li><li>Simple, proven ' + (d.activity === 'Manufacturing' ? 'technology' : 'business model') + '</li><li>Products / services of regular demand</li></ul></div>',
      '<div><h4>Weaknesses</h4><ul><li>New unit — brand yet to be established</li><li>Limited initial working capital</li><li>Dependence on promoter for management</li></ul></div>',
      '<div><h4>Opportunities</h4><ul><li>Growing local demand and purchasing power</li><li>Government support for MSMEs (' + esc(d.scheme === 'OTHER' ? 'credit guarantee, Udyam benefits' : d.scheme) + ')</li><li>Online platforms (ONDC, GeM, social media) for wider reach</li><li>Scope to add new products / services later</li></ul></div>',
      '<div><h4>Threats</h4><ul><li>Competition from established players</li><li>Fluctuation in raw-material / input prices</li><li>Changes in market trends</li></ul></div>',
      '</div>',
      '<p>The promoter plans to overcome weaknesses and threats through quality, competitive pricing, customer service and prudent cost control.</p>',
      '</section>');

    // ---------- 22. Approvals ----------
    h.push('<section>', H('Statutory Registrations &amp; Approvals'), '<ul>' + approvals(d).map(function (a) { return '<li>' + a + '</li>'; }).join('') + '</ul>', '</section>');

    // ---------- 23. Assumptions ----------
    h.push('<section>', H('Key Assumptions'), '<ol>',
      '<li>The unit will work ' + (d.activity === 'Manufacturing' ? '300 days a year in a single shift' : 'throughout the year') + '; capacity utilisation as shown in the sales projection.</li>',
      '<li>Selling prices escalate by ' + d.priceEsc + '% and costs by ' + d.costEsc + '% every year; salaries by ' + d.salaryInc + '%.</li>',
      '<li>Repairs &amp; maintenance @ ' + d.repairsPct + '% of building &amp; machinery; insurance @ ' + d.insurancePct + '% of fixed assets; selling &amp; admin @ ' + d.sellingPct + '% of sales.</li>',
      '<li>Working capital assessed on operating-cycle basis: stock ' + d.rmDays + ' days, finished goods ' + d.fgDays + ' days, debtors ' + d.debtorDays + ' days, creditors ' + d.creditorDays + ' days.</li>',
      '<li>Interest on term loan @ ' + d.tlRate + '% and on working capital @ ' + d.wcRate + '% p.a.</li>',
      '<li>Depreciation on WDV method at Income-tax rates; pre-operative expenses &amp; contingency written off over 5 years.</li>',
      d.drawings ? '<li>Promoter drawings of ' + inr(d.drawings) + ' per year considered.</li>' : '',
      '</ol>', '</section>');

    // ---------- 24. Conclusion ----------
    var viable = m.avgDscr !== null && m.avgDscr >= 1.5 && Y1.pat > 0;
    h.push('<section>', H('Conclusion'),
      '<p>The proposed project of <b>' + esc(d.unitName) + '</b> is ' + (viable ? '<b>technically feasible and financially viable</b>' : 'technically feasible') +
      '. The unit is expected to earn a net profit of ' + inr(Y1.pat) + ' in the first year, rising to ' + inr(m.years[m.N - 1].pat) +
      ' by year ' + m.N + '. The average DSCR of ' + ratio(m.avgDscr) + ' and break-even at ' + pct(Y1.bepCapacity, 1) +
      ' of capacity in the first year indicate ' + (viable ? 'a comfortable' : 'an adequate') + ' capacity to repay the bank loan within the proposed tenure.</p>',
      '<p>The project will also generate employment for ' + employment + ' persons and contribute to the local economy. ' +
      'The bank is requested to sanction a term loan of <b>' + inr(m.termLoan) + '</b> and a working capital limit of <b>' + inr(m.wcLoan) + '</b>' +
      (m.subsidy ? ' under PMEGP with margin money subsidy of ' + inr(m.subsidy) : '') + '.</p>',
      '<div class="sign"><div>Place: ' + esc(d.district || '') + '<br>Date: ____________</div><div>(' + esc(d.applicantName) + ')<br>Signature of Promoter</div></div>',
      '</section>');

    return h.join('\n');
  }

  root.DPRReport = { render: render, inWords: inWords };
})(this);

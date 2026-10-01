/* UdyamCare DPR Generator — financial calculation engine.
 * Pure functions only: takes the form data and returns the full financial model.
 * Works in the browser (window.DPRCalc) and in Node (module.exports) for testing.
 */
(function (root) {
  'use strict';

  // Income-tax WDV depreciation rates (block of assets)
  var DEP_RATES = {
    building: 0.10, machinery: 0.15, furniture: 0.10,
    electrical: 0.10, computers: 0.40, vehicle: 0.15
  };
  var ASSET_LABELS = {
    land: 'Land', building: 'Building / Shed / Civil work', machinery: 'Plant & Machinery / Equipment',
    furniture: 'Furniture & Fixtures', electrical: 'Electrification & Installation',
    computers: 'Computer / IT Equipment', vehicle: 'Vehicle'
  };
  var PREOP_AMORT_YEARS = 5;
  var PMEGP_SUBSIDY_LOCK_MONTHS = 36;
  // CM YUVA (Uttar Pradesh): margin money subsidy 10% of project cost (max ₹50,000),
  // 100% interest subsidy for 4 years, loan up to ₹5 lakh
  var CMYUVA_SUBSIDY_CAP = 50000;
  var CMYUVA_INTEREST_YEARS = 4;
  var CMYUVA_LOAN_CAP = 500000;

  function num(v, dflt) {
    var n = parseFloat(v);
    return isFinite(n) ? n : (dflt || 0);
  }

  function isSpecialCategory(d) {
    return d.category !== 'General' || d.gender === 'Female' || d.gender === 'Transgender';
  }

  // Own contribution % and subsidy % as per scheme guidelines
  function schemeDefaults(d) {
    switch (d.scheme) {
      case 'PMEGP':
        var special = isSpecialCategory(d);
        var rural = d.area === 'Rural';
        return {
          ownPct: special ? 5 : 10,
          subsidyPct: special ? (rural ? 35 : 25) : (rural ? 25 : 15)
        };
      case 'CMYUVA':
        var reserved = ['SC', 'ST', 'Divyang'].indexOf(d.category) >= 0 || d.aspirational === 'Yes';
        return {
          ownPct: reserved ? 10 : d.category === 'OBC' ? 12.5 : 15,
          subsidyPct: 10, tenureYears: 4, moratorium: 6
        };
      case 'MUDRA': return { ownPct: 10, subsidyPct: 0 };
      case 'CGTMSE': return { ownPct: 15, subsidyPct: 0 };
      case 'STANDUP': return { ownPct: 15, subsidyPct: 0 };
      default: return { ownPct: 20, subsidyPct: 0 };
    }
  }

  function mudraCategory(loan) {
    if (loan <= 50000) return 'Shishu (up to ₹50,000)';
    if (loan <= 500000) return 'Kishore (₹50,001 – ₹5 lakh)';
    if (loan <= 1000000) return 'Tarun (₹5 lakh – ₹10 lakh)';
    if (loan <= 2000000) return 'Tarun Plus (₹10 lakh – ₹20 lakh)';
    return 'Above MUDRA limit (₹20 lakh)';
  }

  // Tax on business income (new regime for individuals, flat for firms/companies)
  function incomeTax(constitution, taxable) {
    if (taxable <= 0) return 0;
    var tax;
    if (constitution === 'Proprietorship') {
      if (taxable <= 1200000) return 0; // full rebate u/s 87A (new regime)
      var slabs = [[400000, 0], [800000, 0.05], [1200000, 0.10], [1600000, 0.15],
                   [2000000, 0.20], [2400000, 0.25], [Infinity, 0.30]];
      var prev = 0; tax = 0;
      for (var i = 0; i < slabs.length; i++) {
        var top = slabs[i][0];
        if (taxable > prev) tax += (Math.min(taxable, top) - prev) * slabs[i][1];
        prev = top;
      }
    } else if (constitution === 'Private Limited') {
      tax = taxable * 0.22 * 1.10; // sec 115BAA + 10% surcharge
    } else {
      tax = taxable * 0.30; // partnership / LLP
    }
    return tax * 1.04; // health & education cess
  }

  function irr(flows) {
    var npv = function (r) {
      return flows.reduce(function (s, cf, t) { return s + cf / Math.pow(1 + r, t); }, 0);
    };
    var lo = -0.99, hi = 5;
    if (npv(lo) * npv(hi) > 0) return null;
    for (var i = 0; i < 200; i++) {
      var mid = (lo + hi) / 2;
      if (npv(lo) * npv(mid) <= 0) hi = mid; else lo = mid;
    }
    return (lo + hi) / 2;
  }

  // Monthly term-loan schedule, aggregated year-wise.
  // Interest is serviced during moratorium; principal in equal monthly instalments after it.
  // PMEGP: subsidy is kept as interest-free TDR and adjusted against the loan after 3 years.
  function loanSchedule(amount, ratePct, tenureYears, moratorium, subsidyHeld, years) {
    var totalMonths = Math.round(tenureYears * 12);
    var mor = Math.min(moratorium, totalMonths - 1);
    var bal = amount;
    var held = Math.min(subsidyHeld, amount);
    var inst = (amount - held) / (totalMonths - mor);
    var rows = [];
    for (var y = 1; y <= years; y++) {
      rows.push({ year: y, opening: bal, interest: 0, principal: 0, subsidyAdj: 0, closing: 0 });
    }
    for (var m = 1; m <= totalMonths && bal > 0.005; m++) {
      var yi = Math.ceil(m / 12) - 1;
      var interestBase = Math.max(0, bal - (m <= PMEGP_SUBSIDY_LOCK_MONTHS ? held : 0));
      var interest = interestBase * ratePct / 100 / 12;
      var principal = m > mor ? Math.min(inst, bal - (m <= PMEGP_SUBSIDY_LOCK_MONTHS ? held : 0)) : 0;
      principal = Math.max(0, principal);
      bal -= principal;
      var adj = 0;
      if (held > 0 && m === PMEGP_SUBSIDY_LOCK_MONTHS) {
        adj = Math.min(held, bal);
        bal -= adj;
        held = 0;
      }
      if (m === totalMonths && held > 0) { // loan shorter than lock-in period
        adj = Math.min(held, bal); bal -= adj; held = 0;
      }
      if (yi < years) {
        rows[yi].interest += interest;
        rows[yi].principal += principal;
        rows[yi].subsidyAdj += adj;
      }
    }
    // closings / openings
    var b = amount;
    rows.forEach(function (r) {
      r.opening = b;
      b = Math.max(0, b - r.principal - r.subsidyAdj);
      if (b < 0.5) b = 0;
      r.closing = b;
    });
    return { rows: rows, instalment: inst, repayMonths: totalMonths - mor, totalMonths: totalMonths, moratorium: mor };
  }

  function normalise(raw) {
    var d = {};
    Object.keys(raw).forEach(function (k) { d[k] = raw[k]; });
    var numeric = ['age', 'premisesArea', 'ownPct', 'subsidyPct', 'tlRate', 'wcRate', 'tenureYears',
      'moratorium', 'land', 'building', 'furniture', 'electrical', 'computers', 'vehicle', 'preop',
      'contingencyPct', 'priceEsc', 'costEsc', 'years', 'salaryInc', 'rent', 'power', 'otherExp',
      'repairsPct', 'insurancePct', 'sellingPct', 'rmDays', 'fgDays', 'debtorDays', 'creditorDays', 'drawings',
      'termLoanAmt', 'wcLoanAmt', 'wcOverride'];
    numeric.forEach(function (k) { d[k] = num(raw[k]); });
    d.machinery = (raw.machinery || []).filter(function (r) { return r.name || num(r.rate); })
      .map(function (r) { return { name: r.name || 'Equipment', qty: num(r.qty, 1), rate: num(r.rate), supplier: r.supplier || '' }; });
    d.products = (raw.products || []).filter(function (r) { return r.name || num(r.capacity); })
      .map(function (r) { return { name: r.name || 'Product', unit: r.unit || 'Nos', capacity: num(r.capacity), price: num(r.price), rmCost: num(r.rmCost) }; });
    d.staff = (raw.staff || []).filter(function (r) { return r.role || num(r.salary); })
      .map(function (r) { return { role: r.role || 'Staff', count: num(r.count, 1), salary: num(r.salary) }; });
    d.utilisationArr = String(raw.utilisation || '60').split(/[,\s]+/).map(function (s) { return num(s); })
      .filter(function (n) { return n > 0; });
    if (!d.utilisationArr.length) d.utilisationArr = [60];
    if (!d.tenureYears) d.tenureYears = 5;
    if (!d.years) d.years = 5;
    return d;
  }

  function validate(d) {
    var errs = [];
    if (!d.applicantName) errs.push('Applicant name bharein.');
    if (!d.unitName) errs.push('Business / unit name bharein.');
    if (!d.productLine) errs.push('Product / service line bharein.');
    if (!d.machinery.length) errs.push('Kam se kam ek machine / equipment ki rate bharein.');
    if (!d.products.length) errs.push('Kam se kam ek product / service ki capacity aur price bharein.');
    d.products.forEach(function (p) {
      if (!p.capacity || !p.price) errs.push('"' + p.name + '" ki capacity aur selling price dono bharein.');
    });
    if (d.ownPct < 0 || d.ownPct >= 100) errs.push('Own contribution 0–99% ke beech hona chahiye.');
    if (d.moratorium >= d.tenureYears * 12) errs.push('Moratorium repayment period se kam hona chahiye.');
    return errs;
  }

  function compute(raw) {
    var d = normalise(raw);
    var errors = validate(d);
    if (errors.length) return { errors: errors, d: d };

    var N = Math.min(10, Math.max(d.years, Math.ceil(d.tenureYears)));
    var pe = d.priceEsc / 100, ce = d.costEsc / 100, si = d.salaryInc / 100;

    // ---------- Fixed capital ----------
    var machineryCost = d.machinery.reduce(function (s, m) { return s + m.qty * m.rate; }, 0);
    var assets = {
      land: d.land, building: d.building, machinery: machineryCost, furniture: d.furniture,
      electrical: d.electrical, computers: d.computers, vehicle: d.vehicle
    };
    var depreciable = ['building', 'machinery', 'furniture', 'electrical', 'computers', 'vehicle'];
    var fixedAssets = Object.keys(assets).reduce(function (s, k) { return s + assets[k]; }, 0);
    var contingency = depreciable.reduce(function (s, k) { return s + assets[k]; }, 0) * d.contingencyPct / 100;
    var preopTotal = d.preop + contingency;

    // ---------- Year-wise operations ----------
    var util1 = d.utilisationArr[0] / 100;
    var years = [];
    for (var y = 1; y <= N; y++) {
      var u = d.utilisationArr[Math.min(y - 1, d.utilisationArr.length - 1)] / 100;
      var pf = Math.pow(1 + pe, y - 1), cf = Math.pow(1 + ce, y - 1);
      var productRows = d.products.map(function (p) {
        var qty = p.capacity * u;
        return { name: p.name, unit: p.unit, qty: qty, price: p.price * pf, sales: qty * p.price * pf, rm: qty * p.rmCost * cf };
      });
      var sales = productRows.reduce(function (s, r) { return s + r.sales; }, 0);
      var rm = productRows.reduce(function (s, r) { return s + r.rm; }, 0);
      var wages = d.staff.reduce(function (s, st) { return s + st.count * st.salary * 12; }, 0) * Math.pow(1 + si, y - 1);
      var power = d.power * 12 * cf * (u / util1);
      var rent = d.rent * 12 * cf;
      var other = d.otherExp * 12 * cf;
      var repairs = (assets.machinery + assets.building) * d.repairsPct / 100 * cf;
      var selling = sales * d.sellingPct / 100;
      years.push({ year: y, util: u, products: productRows, sales: sales, rm: rm, wages: wages, power: power,
        rent: rent, other: other, repairs: repairs, selling: selling });
    }

    // ---------- Working capital (operating cycle method) ----------
    years.forEach(function (Y) {
      Y.rmStock = Y.rm / 365 * d.rmDays;
      Y.fgStock = (Y.rm + Y.wages + Y.power + Y.repairs) / 365 * d.fgDays;
      Y.debtors = Y.sales / 365 * d.debtorDays;
      Y.creditors = Y.rm / 365 * d.creditorDays;
      Y.currentAssets = Y.rmStock + Y.fgStock + Y.debtors;
      Y.netWC = Y.currentAssets - Y.creditors;
    });
    // A fixed working-capital figure (e.g. as appraised by the bank) overrides the operating-cycle estimate
    var wcRequirement = d.wcOverride > 0 ? d.wcOverride : Math.max(0, years[0].netWC);

    // ---------- Cost of project & means of finance ----------
    var totalCost = fixedAssets + preopTotal + wcRequirement;
    var ownPct = d.ownPct / 100;
    var own = totalCost * ownPct;
    var termLoan = (fixedAssets + preopTotal) * (1 - ownPct);
    var wcLoan = wcRequirement * (1 - ownPct);
    var fixedLoans = d.termLoanAmt > 0 || d.wcLoanAmt > 0;
    if (fixedLoans) { // loan amounts fixed by the bank; promoter brings the balance
      termLoan = d.termLoanAmt;
      wcLoan = d.wcLoanAmt;
      own = totalCost - termLoan - wcLoan;
      if (own < 0) {
        return { errors: ['Term loan + working capital loan (' + Math.round(termLoan + wcLoan) + ') project cost (' +
          Math.round(totalCost) + ') se zyada hai. Machinery / working capital badhayein ya loan amount ghatayein.'], d: d };
      }
    }
    var ownPctEff = totalCost ? Math.round(own / totalCost * 10000) / 100 : 0;
    var bankLoan = termLoan + wcLoan;
    var subsidy = 0;
    if (d.scheme === 'PMEGP') subsidy = totalCost * d.subsidyPct / 100;
    if (d.scheme === 'CMYUVA') subsidy = Math.min(totalCost * d.subsidyPct / 100, CMYUVA_SUBSIDY_CAP);
    // Only PMEGP keeps the subsidy as an interest-free TDR adjusted against the loan
    var subsidyHeld = d.scheme === 'PMEGP' ? Math.min(subsidy, termLoan) : 0;

    var loan = loanSchedule(termLoan, d.tlRate, d.tenureYears, d.moratorium, subsidyHeld, N);

    // ---------- Depreciation (WDV) ----------
    var wdv = {}; depreciable.forEach(function (k) { wdv[k] = assets[k]; });
    var preopLeft = preopTotal;
    var preopAmort = preopTotal / PREOP_AMORT_YEARS;

    // ---------- P&L, tax, balance sheet, cash flow ----------
    var lossCF = 0, capital = own, cash = 0, prevCA = 0, prevCred = 0, tdr = 0;
    years.forEach(function (Y, i) {
      var openingNet = depreciable.reduce(function (s, k) { return s + wdv[k]; }, 0) + assets.land;
      Y.depByClass = {};
      Y.dep = 0;
      depreciable.forEach(function (k) {
        var dep = wdv[k] * DEP_RATES[k];
        Y.depByClass[k] = dep; Y.dep += dep; wdv[k] -= dep;
      });
      Y.amort = Math.min(preopAmort, preopLeft); preopLeft -= Y.amort;
      Y.insurance = (openingNet - assets.land) * d.insurancePct / 100;

      Y.opex = Y.rm + Y.wages + Y.power + Y.rent + Y.other + Y.repairs + Y.insurance + Y.selling;
      Y.costOfProduction = Y.rm + Y.wages + Y.power + Y.repairs;
      Y.ebitda = Y.sales - Y.opex;
      Y.intTL = loan.rows[i].interest;
      Y.intWC = wcLoan * d.wcRate / 100;
      Y.interest = Y.intTL + Y.intWC;
      Y.interestSubsidy = d.scheme === 'CMYUVA' && Y.year <= CMYUVA_INTEREST_YEARS ? Y.interest : 0;
      Y.pbt = Y.ebitda + Y.interestSubsidy - Y.dep - Y.amort - Y.interest;
      var taxable = Y.pbt - lossCF;
      if (taxable < 0) { lossCF = -taxable; taxable = 0; } else { lossCF = 0; }
      Y.tax = incomeTax(d.constitution, taxable);
      Y.pat = Y.pbt - Y.tax;
      Y.cashAccruals = Y.pat + Y.dep + Y.amort;
      Y.drawings = d.drawings;

      // Term loan
      Y.tlOpening = loan.rows[i].opening;
      Y.tlRepaid = loan.rows[i].principal;
      Y.subsidyAdj = loan.rows[i].subsidyAdj;
      Y.tlClosing = loan.rows[i].closing;

      // Cash flow statement
      var src = [], use = [];
      if (i === 0) {
        src.push(['Own contribution (capital)', own], ['Term loan from bank', termLoan], ['Working capital loan', wcLoan]);
        if (subsidy) src.push(['Subsidy / margin money received', subsidy]);
        use.push(['Purchase of fixed assets', fixedAssets], ['Pre-operative exp. & contingency', preopTotal]);
        if (subsidyHeld) use.push(['Subsidy kept as TDR with bank', subsidy]);
        tdr = subsidyHeld ? subsidy : 0;
      }
      src.push(['Net profit after tax', Y.pat], ['Depreciation', Y.dep], ['Pre-operative exp. written off', Y.amort],
        ['Increase in sundry creditors', Y.creditors - prevCred]);
      use.push(['Increase in current assets (stock + debtors)', Y.currentAssets - prevCA],
        ['Repayment of term loan', Y.tlRepaid], ['Drawings', Y.drawings]);
      if (Y.subsidyAdj) tdr -= Y.subsidyAdj;
      Y.cfSources = src; Y.cfUses = use;
      Y.totalSources = src.reduce(function (s, r) { return s + r[1]; }, 0);
      Y.totalUses = use.reduce(function (s, r) { return s + r[1]; }, 0);
      Y.openingCash = cash;
      cash += Y.totalSources - Y.totalUses;
      Y.closingCash = cash;
      prevCA = Y.currentAssets; prevCred = Y.creditors;

      // Balance sheet
      capital += Y.pat - Y.drawings;
      Y.capital = capital;
      Y.subsidyReserve = subsidy;
      Y.wcLoan = wcLoan;
      Y.totalLiabilities = Y.capital + Y.subsidyReserve + Y.tlClosing + Y.wcLoan + Y.creditors;
      Y.grossBlock = fixedAssets;
      Y.netBlock = depreciable.reduce(function (s, k) { return s + wdv[k]; }, 0) + assets.land;
      Y.accDep = fixedAssets - Y.netBlock;
      Y.preopLeft = preopLeft;
      Y.tdr = tdr;
      Y.totalAssets = Y.netBlock + Y.preopLeft + Y.tdr + Y.currentAssets + Y.closingCash;
      Y.bsDiff = Y.totalAssets - Y.totalLiabilities;

      // Ratios
      Y.debtService = Y.intTL + Y.tlRepaid;
      Y.dscr = Y.debtService > 0 ? (Y.pat + Y.dep + Y.amort + Y.intTL) / Y.debtService : null;
      var currLiab = Y.creditors + Y.wcLoan;
      Y.currentRatio = currLiab > 0 ? (Y.currentAssets + Math.max(0, Y.closingCash)) / currLiab : null;
      var netWorth = Y.capital + Y.subsidyReserve;
      Y.debtEquity = netWorth > 0 ? Y.tlClosing / netWorth : null;
      Y.npMargin = Y.sales ? Y.pat / Y.sales : 0;
      Y.ebitdaMargin = Y.sales ? Y.ebitda / Y.sales : 0;
      var capEmployed = netWorth + Y.tlClosing;
      Y.roce = capEmployed > 0 ? (Y.pbt + Y.interest) / capEmployed : null;

      // Break-even
      Y.fixedCost = Y.wages + Y.rent + Y.insurance + Y.repairs + Y.other + Y.dep + Y.amort + Y.interest;
      Y.variableCost = Y.rm + Y.power + Y.selling;
      Y.contribution = Y.sales - Y.variableCost;
      Y.bep = Y.contribution > 0 ? Y.fixedCost / Y.contribution : null;          // fraction of this year's sales
      Y.bepCapacity = Y.bep !== null ? Y.bep * Y.util : null;                    // fraction of installed capacity
      Y.cashBep = Y.contribution > 0 ? (Y.fixedCost - Y.dep - Y.amort) / Y.contribution * Y.util : null;
    });

    var dscrYears = years.filter(function (Y) { return Y.dscr !== null; });
    var avgDscr = dscrYears.length ?
      dscrYears.reduce(function (s, Y) { return s + Y.pat + Y.dep + Y.amort + Y.intTL; }, 0) /
      dscrYears.reduce(function (s, Y) { return s + Y.debtService; }, 0) : null;
    var minDscr = dscrYears.length ? Math.min.apply(null, dscrYears.map(function (Y) { return Y.dscr; })) : null;

    // Payback on total project cost from cash accruals + interest
    var cum = 0, payback = null;
    for (var j = 0; j < years.length; j++) {
      var inflow = years[j].cashAccruals + years[j].interest;
      if (inflow > 0 && cum + inflow >= totalCost) { payback = j + (totalCost - cum) / inflow; break; }
      cum += inflow;
    }

    // Project IRR (pre-financing, post-tax), terminal value = net block + working capital
    var flows = [-(fixedAssets + preopTotal + wcRequirement)];
    years.forEach(function (Y, i) {
      var dWC = i === 0 ? 0 : Y.netWC - years[i - 1].netWC;
      var f = Y.ebitda - Y.tax - dWC;
      if (i === years.length - 1) f += Y.netBlock + Y.netWC;
      flows.push(f);
    });
    var projectIrr = irr(flows);

    var warnings = [];
    if (d.scheme === 'PMEGP') {
      var cap = d.activity === 'Manufacturing' ? 5000000 : 2000000;
      if (totalCost > cap) warnings.push('PMEGP me ' + d.activity + ' ke liye max project cost ₹' + (cap / 100000) + ' lakh hai; aapka project cost isse zyada hai.');
    }
    if (d.scheme === 'CMYUVA') {
      if (bankLoan > CMYUVA_LOAN_CAP) warnings.push('CM YUVA me pehle charan ka loan max ₹5 lakh hai; bank loan isse zyada aa raha hai.');
      if (d.age && (d.age < 21 || d.age > 40)) warnings.push('CM YUVA ke liye umr 21 se 40 saal honi chahiye.');
    }
    if (d.scheme === 'MUDRA' && bankLoan > 2000000) warnings.push('MUDRA loan ki seema ₹20 lakh hai; bank loan isse zyada aa raha hai.');
    if (avgDscr !== null && avgDscr < 1.5) warnings.push('Average DSCR ' + avgDscr.toFixed(2) + ' hai — bank aam taur par 1.5 se upar dekhte hain. Selling price, capacity ya expenses check karein.');
    if (years.some(function (Y) { return Y.closingCash < 0; })) warnings.push('Kisi saal closing cash negative aa raha hai — repayment ke liye cash kam pad raha hai. Moratorium / tenure badhayein ya kharche ghatayein.');
    if (years[0].pat < 0) warnings.push('Pehle saal loss dikh raha hai — yeh normal ho sakta hai, par bank isse explain karne ko kahega.');

    return {
      errors: [], warnings: warnings, d: d, N: N,
      assets: assets, assetLabels: ASSET_LABELS, depRates: DEP_RATES, depreciable: depreciable,
      machineryCost: machineryCost, fixedAssets: fixedAssets, contingency: contingency, preopTotal: preopTotal,
      wcRequirement: wcRequirement, totalCost: totalCost,
      own: own, ownPct: ownPctEff, fixedLoans: fixedLoans, termLoan: termLoan, wcLoan: wcLoan, bankLoan: bankLoan, subsidy: subsidy,
      loan: loan, years: years, avgDscr: avgDscr, minDscr: minDscr, payback: payback, projectIrr: projectIrr,
      mudraCategory: mudraCategory(bankLoan), specialCategory: isSpecialCategory(d)
    };
  }

  var api = { compute: compute, schemeDefaults: schemeDefaults, incomeTax: incomeTax, loanSchedule: loanSchedule };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.DPRCalc = api;
})(this);

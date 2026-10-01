/* UdyamCare DPR Generator — form handling, samples, save/load and tab switching. */
(function () {
  'use strict';

  var STORAGE_KEY = 'udyamcare-dpr-v1';
  var form = document.getElementById('dpr-form');
  var errorsEl = document.getElementById('form-errors');
  var reportEl = document.getElementById('report');
  var ROW_TABLES = { machinery: 'tbl-machinery', products: 'tbl-products', staff: 'tbl-staff' };

  // ---------- dynamic rows ----------
  function addRow(tableId, values) {
    var tpl = document.getElementById(tableId.replace('tbl-', 'tpl-'));
    var row = tpl.content.firstElementChild.cloneNode(true);
    if (values) {
      row.querySelectorAll('[data-k]').forEach(function (inp) {
        if (values[inp.dataset.k] !== undefined) inp.value = values[inp.dataset.k];
      });
    }
    document.querySelector('#' + tableId + ' tbody').appendChild(row);
  }
  function clearRows(tableId) { document.querySelector('#' + tableId + ' tbody').innerHTML = ''; }
  function readRows(tableId) {
    return Array.prototype.map.call(document.querySelectorAll('#' + tableId + ' tbody tr'), function (tr) {
      var o = {};
      tr.querySelectorAll('[data-k]').forEach(function (inp) { o[inp.dataset.k] = inp.value.trim(); });
      return o;
    });
  }
  document.querySelectorAll('.add-row').forEach(function (b) {
    b.addEventListener('click', function () { addRow(b.dataset.table); });
  });
  form.addEventListener('click', function (e) {
    if (e.target.classList.contains('del')) { e.target.closest('tr').remove(); persist(); }
  });

  // ---------- read / write whole form ----------
  function readForm() {
    var data = {};
    Array.prototype.forEach.call(form.elements, function (el) {
      if (el.name) data[el.name] = el.value.trim();
    });
    Object.keys(ROW_TABLES).forEach(function (k) { data[k] = readRows(ROW_TABLES[k]); });
    return data;
  }
  function writeForm(data) {
    form.reset();
    Array.prototype.forEach.call(form.elements, function (el) {
      if (el.name && data[el.name] !== undefined) el.value = data[el.name];
    });
    Object.keys(ROW_TABLES).forEach(function (k) {
      clearRows(ROW_TABLES[k]);
      (data[k] && data[k].length ? data[k] : [null]).forEach(function (r) { addRow(ROW_TABLES[k], r); });
    });
    if (data.ownPct === undefined || data.ownPct === '') applySchemeDefaults();
  }

  // ---------- scheme defaults (own % and subsidy %) ----------
  function applySchemeDefaults() {
    var d = readForm();
    var s = DPRCalc.schemeDefaults(d);
    form.elements.ownPct.value = s.ownPct;
    form.elements.subsidyPct.value = s.subsidyPct;
  }
  ['scheme', 'category', 'gender', 'area'].forEach(function (n) {
    form.elements[n].addEventListener('change', applySchemeDefaults);
  });
  form.elements.activity.addEventListener('change', function () {
    if (form.elements.activity.value !== 'Manufacturing') form.elements.fgDays.value = 0;
  });

  // ---------- local autosave ----------
  function persist() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(readForm())); } catch (e) { /* storage unavailable */ }
  }
  form.addEventListener('input', persist);
  form.addEventListener('change', persist);

  // ---------- samples ----------
  var SAMPLES = {
    mfg: {
      applicantName: 'Ramesh Kumar', fatherName: 'Shri Mohan Lal', age: '32', gender: 'Male', category: 'OBC',
      qualification: '12th Pass', experience: '4 years working in a flour mill', mobile: '9876543210',
      resAddress: 'Village Rampur, Tehsil Sadar, District Jhansi, Uttar Pradesh',
      unitName: 'Shree Ram Flour Mill', constitution: 'Proprietorship', activity: 'Manufacturing',
      productLine: 'Wheat flour (atta), besan and spices grinding', unitAddress: 'Main Road, Village Rampur',
      district: 'Jhansi', state: 'Uttar Pradesh', area: 'Rural', premises: 'Rented', premisesArea: '800',
      scheme: 'PMEGP', bankName: 'State Bank of India', bankBranch: 'Rampur', tlRate: '10.5', wcRate: '11',
      tenureYears: '7', moratorium: '6', land: '0', building: '250000', furniture: '25000', electrical: '60000',
      computers: '0', vehicle: '0', preop: '20000', contingencyPct: '2',
      utilisation: '60,65,70,75,80', priceEsc: '3', costEsc: '4', years: '5', salaryInc: '5',
      rent: '6000', power: '22000', otherExp: '4000', repairsPct: '2', insurancePct: '0.5', sellingPct: '2',
      rmDays: '15', fgDays: '7', debtorDays: '10', creditorDays: '7', drawings: '120000',
      machinery: [
        { name: 'Atta chakki 20" with 20 HP motor', qty: '2', rate: '185000', supplier: 'Local fabricator / quotation' },
        { name: 'Pulverizer (masala / besan) with 10 HP motor', qty: '1', rate: '95000' },
        { name: 'Wheat cleaning & sieving machine', qty: '1', rate: '65000' },
        { name: 'Electronic weighing scale & bag sealing machine', qty: '1', rate: '30000' }
      ],
      products: [
        { name: 'Wheat flour (atta)', unit: 'Qtl', capacity: '2400', price: '3300', rmCost: '2700' },
        { name: 'Besan', unit: 'Qtl', capacity: '300', price: '8500', rmCost: '7000' },
        { name: 'Job-work grinding', unit: 'Qtl', capacity: '1200', price: '250', rmCost: '0' }
      ],
      staff: [
        { role: 'Machine operator', count: '2', salary: '12000' },
        { role: 'Helper', count: '1', salary: '9000' }
      ]
    },
    svc: {
      applicantName: 'Sunita Devi', fatherName: 'Shri Rajesh Singh', age: '28', gender: 'Female', category: 'General',
      qualification: 'Graduate + Beautician course', experience: '3 years as beautician in a salon', mobile: '9876500000',
      resAddress: 'Ward No. 5, Civil Lines, Sitapur, Uttar Pradesh',
      unitName: 'Glow Beauty Parlour & Spa', constitution: 'Proprietorship', activity: 'Service',
      productLine: 'Beauty parlour, bridal makeup and spa services', unitAddress: 'Shop No. 12, Station Road',
      district: 'Sitapur', state: 'Uttar Pradesh', area: 'Urban', premises: 'Rented', premisesArea: '450',
      scheme: 'MUDRA', bankName: 'Punjab National Bank', bankBranch: 'Station Road', tlRate: '10', wcRate: '11',
      tenureYears: '5', moratorium: '3', land: '0', building: '120000', furniture: '90000', electrical: '40000',
      computers: '30000', vehicle: '0', preop: '15000', contingencyPct: '2',
      utilisation: '55,60,65,70,75', priceEsc: '5', costEsc: '5', years: '5', salaryInc: '6',
      rent: '10000', power: '4000', otherExp: '3000', repairsPct: '2', insurancePct: '0.5', sellingPct: '3',
      rmDays: '30', fgDays: '0', debtorDays: '0', creditorDays: '15', drawings: '90000',
      machinery: [
        { name: 'Hydraulic salon chairs', qty: '3', rate: '18000' },
        { name: 'Facial steamer & skin care machines', qty: '1', rate: '45000' },
        { name: 'Pedicure / spa station', qty: '1', rate: '35000' },
        { name: 'Hair dryers, straighteners & tools', qty: '1', rate: '30000' },
        { name: 'Mirrors, trolleys & sterilizer', qty: '1', rate: '25000' }
      ],
      products: [
        { name: 'Hair & skin services', unit: 'Customers', capacity: '3600', price: '300', rmCost: '60' },
        { name: 'Bridal / party makeup', unit: 'Bookings', capacity: '60', price: '8000', rmCost: '1800' },
        { name: 'Spa & body care', unit: 'Sessions', capacity: '500', price: '700', rmCost: '150' }
      ],
      staff: [{ role: 'Beautician', count: '2', salary: '11000' }, { role: 'Helper / receptionist', count: '1', salary: '8000' }]
    }
  };
  function loadSample(key) {
    writeForm(SAMPLES[key]);
    applySchemeDefaults();
    persist();
  }
  document.getElementById('btn-sample-mfg').addEventListener('click', function () { loadSample('mfg'); });
  document.getElementById('btn-sample-svc').addEventListener('click', function () { loadSample('svc'); });

  document.getElementById('btn-reset').addEventListener('click', function () {
    if (!confirm('Saara data clear kar dein?')) return;
    try { localStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
    writeForm({});
    applySchemeDefaults();
  });

  // ---------- save / load JSON ----------
  document.getElementById('btn-save').addEventListener('click', function () {
    var data = readForm();
    var blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = (data.unitName || 'dpr').replace(/[^\w-]+/g, '_') + '_dpr.json';
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
  });
  document.getElementById('file-load').addEventListener('change', function (e) {
    var f = e.target.files[0];
    if (!f) return;
    var reader = new FileReader();
    reader.onload = function () {
      try { writeForm(JSON.parse(reader.result)); persist(); }
      catch (err) { alert('File read nahi ho payi: ' + err.message); }
    };
    reader.readAsText(f);
    e.target.value = '';
  });

  // ---------- tabs ----------
  function showTab(name) {
    document.querySelectorAll('.tab').forEach(function (t) { t.classList.toggle('active', t.dataset.tab === name); });
    document.getElementById('tab-form').classList.toggle('hidden', name !== 'form');
    document.getElementById('tab-report').classList.toggle('hidden', name !== 'report');
    window.scrollTo(0, 0);
  }
  document.querySelectorAll('.tab').forEach(function (t) {
    t.addEventListener('click', function () {
      if (t.dataset.tab === 'report' && !generate()) return;
      showTab(t.dataset.tab);
    });
  });
  document.getElementById('btn-edit').addEventListener('click', function () { showTab('form'); });
  document.getElementById('btn-print').addEventListener('click', function () { window.print(); });

  // ---------- generate ----------
  function generate() {
    var model = DPRCalc.compute(readForm());
    if (model.errors.length) {
      errorsEl.innerHTML = model.errors.map(function (e) { return '• ' + e; }).join('<br>');
      showTab('form');
      errorsEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return false;
    }
    errorsEl.textContent = '';
    var warn = model.warnings.length ?
      '<div class="warnings no-print"><b>Dhyan dein:</b><ul>' + model.warnings.map(function (w) { return '<li>' + w + '</li>'; }).join('') + '</ul></div>' : '';
    reportEl.innerHTML = warn + DPRReport.render(model);
    document.title = model.d.unitName + ' – Project Report';
    return true;
  }
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    if (generate()) showTab('report');
  });

  // ---------- init ----------
  var saved = null;
  try { saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'); } catch (e) { saved = null; }
  writeForm(saved || {});
  if (!saved) applySchemeDefaults();
})();

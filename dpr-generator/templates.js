/* UdyamCare DPR Generator — business-type write-up templates.
 * Each template gives the Introduction, Market Potential points and Raw Material / Inputs text
 * for one kind of business, written around the applicant's own details (name, unit, place).
 * Works in the browser (window.DPRTemplates) and in Node (module.exports).
 */
(function (root) {
  'use strict';

  // c = { P: promoter, F: father, U: unit name, L: place (district, state), A: full unit address, line: product line }
  var T = {
    borewell: {
      label: 'Borewell drilling works',
      activity: 'Service',
      productLine: 'Borewell drilling & casing-pipe installation service',
      varLabel: 'Diesel, Casing Pipes, Bit Wear & Site Labour',
      fixedLabel: 'Helper Wages, Vehicle Running & Admin.',
      wcLabel: 'Casing Pipe Inventory (PVC/GI)',
      intro: function (c) {
        return [
          c.U + ' is a proposed venture by ' + c.P + c.F + ', resident of ' + c.A + ', to establish a borewell drilling and casing-pipe ' +
          'installation service catering to the agricultural and domestic water-supply needs of the local rural belt. The unit will ' +
          'undertake drilling of shallow and medium-depth borewells for irrigation tube wells, domestic hand-pumps and submersible pump ' +
          'installations, using a compressor-based drilling attachment along with supporting tools, casing pipes, and a diesel generator ' +
          'for on-site power at locations without grid electricity.',
          c.L + ' and the surrounding villages fall within a predominantly agrarian belt, where dependable groundwater access through ' +
          'borewells is essential for irrigation, particularly during the dry Rabi season and in years of deficient rainfall. Government ' +
          'schemes promoting micro-irrigation and farm mechanisation, along with the steady expansion of submersible pump-based domestic ' +
          'water supply in rural households, continue to sustain strong and recurring demand for professional drilling and casing services.',
          'The unit will serve farmers, individual households and small institutional clients (schools, panchayat buildings, community ' +
          'structures) requiring new borewells, re-drilling of failed or low-yield borewells, and casing-pipe replacement. Since successful ' +
          'completion depends on prompt availability of casing pipes matched to the bore diameter and depth, the promoter will maintain a ' +
          'running stock of PVC and GI casing pipes of various sizes rather than sourcing pipes job-by-job.',
          c.P + ' is familiar with local hydro-geological conditions, typical bore depths and water-table behaviour in the area, which is ' +
          'valuable for efficient and reliable service. Being based locally, the promoter is well placed to build a strong reputation through ' +
          'farmer-to-farmer referrals, which remain the main way drilling contractors are chosen in rural markets.'
        ];
      },
      market: [
        ['Non-discretionary agrarian demand', 'Groundwater-dependent irrigation and domestic water supply make borewell drilling an essential, recurring rural need rather than a discretionary purchase.'],
        ['Limited organised competition', 'Drilling services in smaller village clusters are often provided by contractors from distant towns with limited local presence; a locally based operator has a mobilisation-speed advantage.'],
        ['Value-addition opportunities', 'Casing-pipe supply, submersible pump installation and wiring, and annual borewell servicing / re-drilling can be bundled with the core drilling contract to raise revenue per job.'],
        ['Government-scheme linkage', 'Micro-irrigation and farm mechanisation schemes and rural piped water-supply programmes continue to expand the addressable market.'],
        ['Seasonal but recurring cash flow', 'Demand peaks ahead of the Rabi sowing season and during summer water-scarcity months, but recurs predictably every year across the service area.'],
        ['Employment impact', 'The project provides self-employment to the promoter and creates work for local drilling helpers on a per-job basis.']
      ],
      inputs: function (c) {
        return 'A borewell drilling business is an equipment- and inventory-intensive service trade. The core equipment is a compressor-based ' +
          'drilling attachment (used with rods and drill bits for DTH / percussion drilling of shallow and medium-depth borewells), a diesel ' +
          'generator to power the compressor and tools at sites without grid electricity, and a set of drilling rods, bits and hand tools that ' +
          'need periodic replacement due to wear. A water-testing and submersible pump-installation kit is required to commission completed ' +
          'borewells. The single largest recurring input is casing-pipe inventory (PVC and GI pipes of various diameters and lengths), which ' +
          'must be kept as ready stock since customers expect drilling and casing to be completed within a day or two of confirmation. Pipes ' +
          'and consumables are available from wholesale pipe and borewell-equipment dealers in nearby towns. A small yard / office is needed ' +
          'for storage and record-keeping. ' + c.P + ' will supervise and operate the drilling equipment, supported by one to two local ' +
          'helpers engaged per job for rod handling, pipe lowering and site labour.';
      }
    },

    coaching: {
      label: 'Computer coaching institute',
      activity: 'Service',
      productLine: 'Computer training & coaching institute (basic computer, DCA/ADCA, Tally, typing, CCC)',
      varLabel: 'Course Material, Exam / Certification Fees & Consumables',
      fixedLabel: 'Faculty Salary, Rent, Electricity, Internet & Admin.',
      wcLabel: 'Working Capital (study material, fees float & running expenses)',
      intro: function (c) {
        return [
          c.U + ' is a proposed computer training institute to be set up by ' + c.P + c.F + ' at ' + c.A + '. The institute will offer ' +
          'job-oriented computer courses such as Basic Computer, DCA / ADCA, Tally with GST, Hindi / English typing, MS Office, internet ' +
          'and digital payments, and preparation for the CCC / O-Level certification required for many government jobs.',
          'Computer literacy has become a basic requirement for students, job seekers and small business owners in ' + c.L + '. ' +
          'Government recruitment, online applications, banking, GST filing and almost every office job now require working knowledge ' +
          'of computers, yet quality training facilities with sufficient machines and trained faculty remain limited in the area.',
          'The institute will be equipped with networked computers, a projector-based classroom, power backup and internet connectivity, ' +
          'and will run multiple batches through the day so that school and college students, working youth and women can attend at ' +
          'convenient times. Affordable fees, practical hands-on training and placement / certification support will be the key focus.',
          c.P + ' has the required computer knowledge and teaching aptitude and will personally manage the institute, supported by ' +
          'trained faculty. Being a local resident, the promoter can build trust with parents and students, which is the main driver of ' +
          'admissions in coaching businesses.'
        ];
      },
      market: [
        ['Mandatory digital skills', 'Computer knowledge (CCC / O-Level, typing) is now a stated requirement for many government and private jobs, creating steady demand from students and job seekers.'],
        ['Large student base', 'Schools, intermediate colleges and degree colleges in and around the area produce a fresh batch of potential learners every year.'],
        ['Limited quality competition', 'Existing centres often have few machines, outdated software or irregular faculty; a well-equipped institute with regular batches can attract students quickly.'],
        ['Multiple revenue streams', 'Short-term courses, long-term diplomas, typing practice, exam form-filling and printing / photocopy services add to the income per student.'],
        ['Government push', 'Digital India, Skill India and online public services continue to expand the need for basic computer training in rural and semi-urban areas.'],
        ['Employment impact', 'The institute provides self-employment to the promoter and jobs for local faculty and support staff, and improves the employability of local youth.']
      ],
      inputs: function (c) {
        return 'The main fixed inputs are desktop computers, a server / networking setup, a printer-cum-scanner, a projector and screen for ' +
          'classroom teaching, a power backup (inverter and batteries / UPS), licensed software, and furniture such as computer tables, chairs ' +
          'and a reception counter. Recurring inputs are course books and study material, certification / examination fees payable to the ' +
          'affiliating body, stationery, internet and electricity. Computers and accessories will be purchased from authorised dealers with ' +
          'warranty; study material is available from publishers and the affiliating organisation. ' + c.P + ' will manage admissions, ' +
          'teaching and administration, supported by trained faculty for different courses.';
      }
    },

    restaurant: {
      label: 'Restaurant / food outlet',
      activity: 'Service',
      productLine: 'Family restaurant (veg meals, fast food, snacks & beverages)',
      varLabel: 'Food Raw Material, Gas & Packaging',
      fixedLabel: 'Staff Salary, Rent, Electricity & Admin.',
      wcLabel: 'Working Capital (grocery stock & running expenses)',
      intro: function (c) {
        return [
          c.U + ' is a proposed family restaurant to be set up by ' + c.P + c.F + ' at ' + c.A + '. The restaurant will serve ' +
          'freshly cooked vegetarian meals (thali), North Indian and Chinese dishes, fast food, snacks, sweets and beverages for dine-in, ' +
          'takeaway and home delivery.',
          'Eating out and ordering food have become a regular habit in ' + c.L + ' with rising incomes, more working families, students ' +
          'and travellers. Customers increasingly look for clean, hygienic and reasonably priced places to eat with family, while many ' +
          'existing eateries are small dhabas with limited seating and hygiene.',
          'The restaurant will be located at a busy spot with good footfall and will offer a comfortable seating area, an open and clean ' +
          'kitchen, quick service and affordable pricing. Catering orders for small functions, tiffin service and listing on online food ' +
          'delivery platforms will provide additional business.',
          c.P + ' has practical exposure to food preparation and local tastes and will personally supervise purchase, kitchen operations ' +
          'and customer service, supported by a cook, helpers and service staff.'
        ];
      },
      market: [
        ['Daily, recurring demand', 'Food is a daily need; office-goers, students, travellers and families provide footfall throughout the day and week.'],
        ['Rising eating-out culture', 'Higher disposable income and changing lifestyles have increased spending on restaurants, fast food and home delivery.'],
        ['Gap in hygienic family dining', 'Most local eateries are small dhabas or carts; a clean family restaurant with seating and a varied menu fills a clear gap.'],
        ['Additional channels', 'Online delivery platforms, tiffin service and catering for small functions add revenue beyond walk-in customers.'],
        ['Quick cash cycle', 'Sales are mostly in cash / UPI with negligible credit, giving fast turnover of working capital.'],
        ['Employment impact', 'The restaurant provides self-employment to the promoter and regular jobs for cooks, helpers and service staff.']
      ],
      inputs: function (c) {
        return 'The main equipment includes commercial gas burners / bhatti, tandoor, deep freezer and refrigerator, chimney / exhaust, ' +
          'stainless-steel kitchen platform and utensils, mixer-grinder, water purifier, and dining furniture (tables, chairs, counter). ' +
          'Recurring raw materials are flour, rice, pulses, edible oil, spices, vegetables, milk and dairy products, LPG cylinders and ' +
          'packaging material, all easily available daily from local wholesale markets and dairies. FSSAI registration and hygiene ' +
          'norms will be followed. ' + c.P + ' will supervise purchases and kitchen operations, supported by a cook, helpers and waiters.';
      }
    },

    jansewa: {
      label: 'Jan Seva Kendra / CSC',
      activity: 'Service',
      productLine: 'Jan Seva Kendra / Common Service Centre (online forms, certificates, banking, printing & photocopy)',
      varLabel: 'Paper, Toner, Service Charges to Portals & Consumables',
      fixedLabel: 'Operator Salary, Rent, Electricity, Internet & Admin.',
      wcLabel: 'Working Capital (cash float for banking / bill-payment services & stock)',
      intro: function (c) {
        return [
          c.U + ' is a proposed Jan Seva Kendra / Common Service Centre to be set up by ' + c.P + c.F + ' at ' + c.A + '. The centre ' +
          'will provide government-to-citizen and business services such as income, caste and domicile certificates, Aadhaar and PAN ' +
          'related services, online application forms, scholarship and pension forms, ration card services, utility bill payments, banking ' +
          '(AEPS / cash withdrawal and deposit), insurance, ticket booking, printing, scanning, lamination and photocopy.',
          'Most government services and job / admission applications in ' + c.L + ' are now available only online. Rural and semi-urban ' +
          'residents, many of whom lack computers, internet or the know-how to apply, depend on a nearby trusted centre for these services, ' +
          'which creates steady daily footfall.',
          'The centre will be equipped with computers, printers, biometric devices, internet connectivity and power backup, and will work ' +
          'through authorised portals (CSC / e-District / bank BC as applicable). Quick service, fair charges and correct guidance will be ' +
          'the main strengths of the unit.',
          c.P + ' has working knowledge of computers and online portals and will personally run the centre, supported by an operator during ' +
          'peak hours. Being a local resident, the promoter is well known in the area, which helps build trust for document and banking services.'
        ];
      },
      market: [
        ['Mandatory online services', 'Certificates, scholarships, pensions, recruitments and admissions are applied for online, making assisted-service centres essential.'],
        ['Daily footfall', 'Banking, bill payment, printing and photocopy needs bring customers every day, not just in admission or recruitment seasons.'],
        ['Under-served area', 'Many villages lack a well-equipped centre with reliable internet and power backup; residents travel to the tehsil for basic services.'],
        ['Multiple income sources', 'Service charges, banking commissions, insurance and ticket commissions, printing and stationery sales add up to a stable income.'],
        ['Government push', 'Digital India, e-District and Direct Benefit Transfer programmes continue to add new online services every year.'],
        ['Employment impact', 'The centre provides self-employment to the promoter and a job for a local operator.']
      ],
      inputs: function (c) {
        return 'The main equipment includes desktop computers, a laser printer, a colour printer / scanner, photocopier, lamination machine, ' +
          'biometric fingerprint / iris device, webcam, internet router, inverter with batteries, CCTV and office furniture. Recurring inputs ' +
          'are paper, toner / ink, lamination pouches, stationery and portal / service charges, all available from local stationery and computer ' +
          'dealers. A cash float is kept for banking and bill-payment services. ' + c.P + ' will run the centre, supported by one operator.';
      }
    },

    shuttering: {
      label: 'Shuttering store (centering material on rent)',
      activity: 'Service',
      productLine: 'Shuttering / centering material on hire (steel plates, props, pipes & accessories)',
      varLabel: 'Transport, Loading, Repairs & Maintenance of Material',
      fixedLabel: 'Helper Wages, Rent, Electricity & Admin.',
      wcLabel: 'Working Capital (running expenses & receivables)',
      intro: function (c) {
        return [
          c.U + ' is a proposed shuttering / centering store to be set up by ' + c.P + c.F + ' at ' + c.A + '. The unit will supply steel ' +
          'shuttering plates, adjustable props, scaffolding pipes, couplers, channels, jack and accessories on daily / monthly rent to house ' +
          'builders, masons, contractors and government construction works for casting of roofs (lintel and slab), beams and columns.',
          'Construction activity in ' + c.L + ' has grown steadily with new houses, shops, schools, PM Awas Yojana houses and rural roads ' +
          'and buildings. Every RCC roof needs shuttering material for 2–4 weeks, and most builders prefer hiring it rather than buying, ' +
          'which creates regular rental demand.',
          'The store will maintain an adequate stock of good-quality steel plates and props so that orders can be supplied quickly, along ' +
          'with transport for delivery and pick-up from site. Rental income is earned repeatedly on the same material, which keeps the ' +
          'running cost low once the stock is purchased.',
          c.P + ' knows the local builders, masons and contractors and will personally manage bookings, delivery and recovery of material, ' +
          'supported by helpers for loading and unloading.'
        ];
      },
      market: [
        ['Steady construction demand', 'Housing, PM Awas Yojana, shops and public works keep RCC roof casting going round the year in the area.'],
        ['Hire preferred over purchase', 'Individual house builders and small contractors prefer to rent shuttering for a few weeks rather than invest in their own material.'],
        ['Repeated income on same asset', 'Each plate and prop earns rent many times a year, giving good returns on the initial investment.'],
        ['Limited local suppliers', 'Builders often have to arrange material from far-off towns; a local store with ready stock and transport gets preference.'],
        ['Low operating cost', 'After the material is purchased, running costs are mainly transport, helpers and minor repairs.'],
        ['Employment impact', 'The store provides self-employment to the promoter and work for local helpers and transporters.']
      ],
      inputs: function (c) {
        return 'The main fixed inputs are steel shuttering plates of standard sizes, adjustable steel props / jacks, scaffolding pipes and ' +
          'couplers, MS channels and angles, wooden ballies / planks where needed, and basic tools. These are available from steel and ' +
          'shuttering-material dealers in nearby towns. Recurring inputs are transport for delivery and pick-up, labour for loading, cleaning ' +
          'and oiling of plates, and repair / welding of damaged material. An open yard with a small store-room is needed for keeping the ' +
          'material. ' + c.P + ' will manage bookings and recovery, supported by helpers.';
      }
    },

    erickshaw: {
      label: 'E-rickshaw (passenger / loader)',
      activity: 'Service',
      productLine: 'E-rickshaw passenger / goods transport service',
      varLabel: 'Battery Charging (Electricity), Repairs & Tyres',
      fixedLabel: 'Insurance, Permit / Registration, Parking & Admin.',
      wcLabel: 'Working Capital (running expenses)',
      intro: function (c) {
        return [
          c.U + ' is a proposed e-rickshaw transport service to be started by ' + c.P + c.F + ', resident of ' + c.A + '. The promoter ' +
          'will purchase a battery-operated e-rickshaw and run it on local routes for passenger travel and small goods transport between ' +
          'villages, markets, bus stands, railway stations, schools and hospitals.',
          'E-rickshaws have become the most popular low-cost means of short-distance travel in ' + c.L + '. They are cheaper to run than ' +
          'auto-rickshaws, pollution-free, and convenient for daily commuters, students, women and elderly passengers, so demand remains ' +
          'steady throughout the year.',
          'The vehicle will be registered with the RTO, insured, and charged overnight at home / at a charging point. Running cost is very ' +
          'low compared to petrol or diesel vehicles, so a good part of the daily earning is available for loan repayment and family needs.',
          c.P + ' knows the local routes well and will drive the e-rickshaw personally, ensuring regular daily income and careful maintenance of the vehicle.'
        ];
      },
      market: [
        ['Daily commuting need', 'Short-distance travel to markets, stations, schools and hospitals is a daily need of local residents.'],
        ['Low fare, high preference', 'E-rickshaw fares are lower than autos, making them the first choice of most local passengers.'],
        ['Very low running cost', 'Electric charging costs a fraction of petrol / diesel, leaving a larger net margin per trip.'],
        ['Goods transport option', 'Shopkeepers and households also hire e-rickshaws for carrying small loads, adding to income.'],
        ['Government support', 'Clean-mobility policies and subsidies encourage electric vehicles in towns and villages.'],
        ['Employment impact', 'The project provides direct self-employment to the promoter with a regular daily income.']
      ],
      inputs: function (c) {
        return 'The main investment is a battery-operated e-rickshaw (with lead-acid or lithium battery set and charger) purchased from an ' +
          'authorised dealer, along with registration, insurance and basic accessories. The main running inputs are electricity for daily ' +
          'charging, periodic servicing, tyre replacement and battery replacement after its useful life. Spare parts and service are available ' +
          'with local dealers and mechanics. ' + c.P + ' will drive and maintain the vehicle personally.';
      }
    }
  };

  function ctx(d) {
    var place = [d.district, d.state].filter(Boolean).join(', ') || 'the proposed area';
    return {
      P: (d.gender === 'Female' ? 'Ms. ' : 'Mr. ') + (d.applicantName || 'the promoter'),
      F: d.fatherName ? ', ' + (/^(s\/o|d\/o|w\/o)/i.test(d.fatherName) ? d.fatherName : (d.gender === 'Female' ? 'D/o ' : 'S/o ') + d.fatherName) : '',
      U: d.unitName || 'The unit',
      L: place,
      A: [d.unitAddress, d.district, d.state].filter(Boolean).join(', ') || place,
      line: d.productLine || ''
    };
  }

  function get(key) { return T[key] || null; }

  // Plain-text write-up for a template, ready to paste into the form textareas
  function plainText(key, d) {
    var t = get(key);
    if (!t) return null;
    var c = ctx(d);
    return {
      introText: t.intro(c).join('\n\n'),
      marketPoints: t.market.map(function (p) { return p[0] + ': ' + p[1]; }).join('\n'),
      inputsText: t.inputs(c)
    };
  }

  var list = Object.keys(T).map(function (k) { return { key: k, label: T[k].label }; });
  var api = { get: get, ctx: ctx, plainText: plainText, list: list, templates: T };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.DPRTemplates = api;
})(this);

'use strict';

const TRANSACTIONS = [
  {id:'TXN-8821A',amount:'$2,847.00',customer:'Sarah K.',merchant:'Electronics Hub',score:87,status:'BLOCKED',time:'14:32:11',card:'****4821',ip:'185.234.12.88'},
  {id:'TXN-9912B',amount:'$124.50',customer:'Mike R.',merchant:'Coffee Bean',score:12,status:'PASSED',time:'14:32:09',card:'****2211',ip:'72.14.204.99'},
  {id:'TXN-7743C',amount:'$5,200.00',customer:'Emma L.',merchant:'Luxury Goods',score:68,status:'FLAGGED',time:'14:32:07',card:'****8833',ip:'46.101.78.22'},
  {id:'TXN-6652D',amount:'$88.20',customer:'John M.',merchant:'Grocery Plus',score:8,status:'PASSED',time:'14:32:05',card:'****1199',ip:'192.168.1.50'},
  {id:'TXN-5541E',amount:'$9,999.00',customer:'Anna B.',merchant:'Wire Transfer',score:95,status:'BLOCKED',time:'14:32:03',card:'****6677',ip:'94.102.49.190'},
  {id:'TXN-4430F',amount:'$340.00',customer:'Tom P.',merchant:'Travel Agency',score:52,status:'FLAGGED',time:'14:32:01',card:'****3344',ip:'104.16.85.20'},
  {id:'TXN-3329G',amount:'$15.99',customer:'Lisa W.',merchant:'Streaming SVC',score:5,status:'PASSED',time:'14:31:59',card:'****8822',ip:'208.67.222.22'},
  {id:'TXN-2218H',amount:'$1,850.00',customer:'Dave N.',merchant:'Forex Exchange',score:74,status:'BLOCKED',time:'14:31:57',card:'****5566',ip:'195.148.127.11'},
  {id:'TXN-1107I',amount:'$67.30',customer:'Kate S.',merchant:'Pharmacy',score:10,status:'PASSED',time:'14:31:55',card:'****9900',ip:'8.8.4.4'},
  {id:'TXN-0996J',amount:'$420.00',customer:'Ben Y.',merchant:'Crypto ATM',score:61,status:'FLAGGED',time:'14:31:53',card:'****1122',ip:'185.220.101.5'},
];

const ALERTS = [
  {id:'ALT-001',txn:'TXN-8821A',rule:'High Amount + Geo Anomaly',score:87,status:'OPEN',created:'14:32:11'},
  {id:'ALT-002',txn:'TXN-5541E',rule:'Velocity + ML Score',score:95,status:'INVESTIGATING',created:'14:32:03'},
  {id:'ALT-003',txn:'TXN-2218H',rule:'Geo Anomaly',score:74,status:'OPEN',created:'14:31:57'},
  {id:'ALT-004',txn:'TXN-7743C',rule:'High Amount',score:68,status:'OPEN',created:'14:32:07'},
  {id:'ALT-005',txn:'TXN-4430F',rule:'Device Fingerprint',score:52,status:'INVESTIGATING',created:'14:32:01'},
  {id:'ALT-006',txn:'TXN-0996J',rule:'Velocity Check',score:61,status:'OPEN',created:'14:31:53'},
];

const RULES = [
  {name:'Velocity Check',type:'VELOCITY',threshold:'10 txn / 1h',enabled:true,triggered:8203,desc:'Blocks when same card makes >10 transactions per hour.'},
  {name:'High Amount',type:'AMOUNT',threshold:'> $5,000',enabled:true,triggered:3421,desc:'Flags transactions exceeding $5,000 for manual review.'},
  {name:'Geo Anomaly',type:'GEO',threshold:'500 km / 2h',enabled:true,triggered:2819,desc:'Flags transactions from locations >500km apart within 2 hours.'},
  {name:'Device Fingerprint',type:'DEVICE',threshold:'New device',enabled:true,triggered:4102,desc:'Flags payments from unrecognized device fingerprints.'},
  {name:'ML Fraud Score',type:'ML_SCORE',threshold:'Score > 80',enabled:true,triggered:1987,desc:'Blocks when ML model returns fraud probability >80%.'},
  {name:'Night Hours',type:'VELOCITY',threshold:'12am - 5am',enabled:false,triggered:892,desc:'Flags high-value transactions placed between midnight and 5am.'},
];

const RULE_STATS = [
  {name:'Velocity Check',count:8203,pct:100},
  {name:'Device Fingerprint',count:4102,pct:50},
  {name:'High Amount',count:3421,pct:42},
  {name:'Geo Anomaly',count:2819,pct:34},
  {name:'ML Fraud Score',count:1987,pct:24},
];

let currentFilter = 'all';

function scoreClass(s) { return s > 70 ? 'high' : s > 40 ? 'med' : 'low'; }
function badge(status) { return `<span class="badge badge-${status}">${status}</span>`; }

function renderTxnTable(filter) {
  const data = filter === 'all' ? TRANSACTIONS : TRANSACTIONS.filter(t => t.status === filter);
  document.getElementById('txnBody').innerHTML = data.map(t => `<tr>
    <td class="mono">${t.id}</td>
    <td><strong>${t.amount}</strong></td>
    <td>${t.customer}</td>
    <td>${t.merchant}</td>
    <td><span class="score-val ${scoreClass(t.score)}">${t.score}</span></td>
    <td>${badge(t.status)}</td>
    <td class="mono">${t.time}</td>
  </tr>`).join('');

  document.getElementById('fullTxnBody').innerHTML = TRANSACTIONS.map(t => `<tr>
    <td class="mono">${t.id}</td>
    <td><strong>${t.amount}</strong></td>
    <td class="mono">${t.card}</td>
    <td class="mono">${t.ip}</td>
    <td>${t.merchant}</td>
    <td><span class="score-val ${scoreClass(t.score)}">${t.score}</span></td>
    <td>${badge(t.status)}</td>
    <td class="mono">${Math.floor(Math.random()*15)+2}ms</td>
  </tr>`).join('');
}

function renderAlerts() {
  document.getElementById('alertsBody').innerHTML = ALERTS.map(a => `<tr>
    <td class="mono">${a.id}</td>
    <td class="mono">${a.txn}</td>
    <td>${a.rule}</td>
    <td><span class="score-val ${scoreClass(a.score)}">${a.score}</span></td>
    <td>${badge(a.status)}</td>
    <td class="mono">${a.created}</td>
    <td><button class="action-btn btn-investigate" onclick="investigateAlert('${a.id}')">Investigate</button></td>
  </tr>`).join('');
  document.getElementById('openAlertsCount').textContent = ALERTS.filter(a => a.status === 'OPEN').length + ' Open';
}

function investigateAlert(id) {
  const a = ALERTS.find(x => x.id === id);
  if (a && a.status === 'OPEN') { a.status = 'INVESTIGATING'; renderAlerts(); }
}

function renderRulesChart() {
  document.getElementById('rulesChart').innerHTML = RULE_STATS.map(r => `
    <div class="rule-bar-row">
      <div class="rule-bar-label"><span>${r.name}</span><span>${r.count.toLocaleString()}</span></div>
      <div class="rule-bar-track"><div class="rule-bar-fill" style="width:${r.pct}%"></div></div>
    </div>`).join('');
}

function renderRuleCards() {
  document.getElementById('rulesGrid').innerHTML = RULES.map((r, i) => `
    <div class="rule-card">
      <div class="rule-card-top">
        <div><div class="rule-name">${r.name}</div><div class="rule-type">${r.type}</div></div>
        <label class="toggle"><input type="checkbox" ${r.enabled ? 'checked' : ''} onchange="toggleRule(${i},this.checked)"><span class="toggle-slider"></span></label>
      </div>
      <div style="font-size:13px;color:var(--muted);margin-bottom:12px">${r.desc}</div>
      <div class="rule-stat">Threshold: <span>${r.threshold}</span></div>
      <div class="rule-stat">Triggered: <span>${r.triggered.toLocaleString()} times</span></div>
    </div>`).join('');
}

function toggleRule(i, val) { RULES[i].enabled = val; }

function filterTxns(filter, btn) {
  currentFilter = filter;
  document.querySelectorAll('.fbtn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  renderTxnTable(filter);
}

function showTab(tab) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  event.target.classList.add('active');
}

let counter = 0;
setInterval(() => {
  counter++;
  const el = document.getElementById('totalTxns');
  if (el) {
    const base = 1247832 + counter * 3;
    el.textContent = base.toLocaleString();
  }
}, 1200);

renderTxnTable('all');
renderAlerts();
renderRulesChart();
renderRuleCards();

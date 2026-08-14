const API = 'http://localhost:8080/api';
const $ = id => document.getElementById(id);

/* ---------- pomocne ---------- */

function log(msg, cls) {
  const row = document.createElement('div');
  row.className = cls || '';
  row.innerHTML = `<span class="l-time">${new Date().toLocaleTimeString('sr-RS')}</span> ${msg}`;
  $('log').prepend(row);
}

async function api(method, path, body) {
  const res = await fetch(API + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(`${res.status}: ${(data && data.message) || res.statusText}`);
  return data;
}

const date = s => (s ? s.slice(0, 16).replace('T', ' ') : '-');
const tag = (text, cls) => `<span class="tag ${cls}">${text}</span>`;

/* ---------- ucitavanje tabela ---------- */

// Svaki panel opisuje odakle vuce podatke, kako crta red i koliko ima kolona.
const PANELS = {
  members: {
    path: '/members',
    cols: 4,
    empty: 'Nema clanova',
    row: m => `<td>${m.id}</td><td>${m.firstName} ${m.lastName}</td>
               <td>${m.email}</td><td>${date(m.membershipDate)}</td>`
  },
  books: {
    path: '/books',
    cols: 4,
    empty: 'Nema knjiga',
    row: b => `<td>${b.id}</td><td>${b.title}</td><td>${b.author}</td>
               <td>${b.available ? tag('slobodna', 'tag-ok') : tag('pozajmljena', 'tag-busy')}</td>`
  },
  loans: {
    path: '/loans',
    cols: 6,
    empty: 'Nema pozajmica',
    row: l => `<td>${l.id}</td><td>#${l.memberId}</td><td>#${l.bookId}</td><td>${date(l.dueDate)}</td>
               <td>${l.status === 'ACTIVE' ? tag('aktivna', 'tag-busy') : tag('vracena', 'tag-off')}</td>
               <td>${l.status === 'ACTIVE'
                 ? `<button class="small" onclick="returnLoan(${l.id})">Vrati</button>` : ''}</td>`
  },
  reservations: {
    path: '/reservations',
    cols: 6,
    empty: 'Niko ne ceka u redu',
    row: r => {
      const status = { WAITING: tag('ceka', 'tag-busy'), NOTIFIED: tag('obavesten', 'tag-ok') }[r.status]
        || tag('otkazana', 'tag-off');
      return `<td>${r.id}</td><td>#${r.memberId}</td><td>#${r.bookId}</td><td>${date(r.requestDate)}</td>
              <td>${status}</td>
              <td>${r.status === 'WAITING'
                ? `<button class="small ghost" onclick="cancelReservation(${r.id})">Otkazi</button>` : ''}</td>`;
    }
  },
  notifications: {
    path: '/notifications',
    cols: 5,
    empty: 'Nema obavestenja, kreiraj pozajmicu pa sacekaj koji trenutak',
    reverse: true,
    row: n => `<td>${n.id}</td>
               <td>${n.type === 'LOAN_CREATED' ? tag('pozajmica', 'tag-busy') : tag('vracanje', 'tag-ok')}</td>
               <td>${n.message}</td><td>#${n.relatedLoanId}</td><td>${date(n.createdAt)}</td>`
  }
};

async function load(name) {
  const panel = PANELS[name];
  const body = $(name);
  try {
    let items = await api('GET', panel.path);
    if (panel.reverse) items = items.slice().reverse();
    body.innerHTML = items.length
      ? items.map(x => `<tr>${panel.row(x)}</tr>`).join('')
      : `<tr><td colspan="${panel.cols}" class="empty">${panel.empty}</td></tr>`;
    return items;
  } catch (err) {
    body.innerHTML = `<tr><td colspan="${panel.cols}" class="empty">Greska: ${err.message}</td></tr>`;
    return [];
  }
}

function refreshAll() {
  Object.keys(PANELS).forEach(load);
}

/* ---------- akcije ---------- */

// Zajednicki obrazac: posalji formu, ispisi rezultat u log, osvezi tabele.
async function submit(event, path, payload, onSuccess, panels) {
  event.preventDefault();
  try {
    const result = await api('POST', path, payload());
    log(onSuccess(result), 'l-ok');
    event.target.reset();
    panels.forEach(load);
  } catch (err) {
    log(err.message, 'l-err');
  }
}

function addMember(e) {
  return submit(e, '/members',
    () => ({ firstName: $('m-first').value, lastName: $('m-last').value, email: $('m-email').value }),
    m => `Clan #${m.id} ${m.firstName} ${m.lastName} kreiran`,
    ['members']);
}

function addBook(e) {
  return submit(e, '/books',
    () => ({ title: $('b-title').value, author: $('b-author').value, isbn: $('b-isbn').value }),
    b => `Knjiga #${b.id} "${b.title}" dodata`,
    ['books']);
}

function addLoan(e) {
  return submit(e, '/loans',
    () => ({ memberId: +$('l-member').value, bookId: +$('l-book').value }),
    l => `Pozajmica #${l.id} kreirana, knjiga #${l.bookId} zauzeta preko Feign-a`,
    ['loans', 'books']);
}

function addReservation(e) {
  return submit(e, '/reservations',
    () => ({ memberId: +$('r-member').value, bookId: +$('r-book').value }),
    r => `Rezervacija #${r.id}, clan #${r.memberId} je u redu za knjigu #${r.bookId}`,
    ['reservations']);
}

async function returnLoan(id) {
  try {
    const loan = await api('PUT', `/loans/${id}/return`);
    log(`Pozajmica #${loan.id} vracena, knjiga oslobodjena`, 'l-ok');
    log('ceka se asinhrona obrada (obavestenje i pomeranje reda cekanja)...', 'l-async');
    load('loans');
    load('books');
  } catch (err) {
    log(err.message, 'l-err');
  }
}

async function cancelReservation(id) {
  try {
    await api('DELETE', `/reservations/${id}`);
    log(`Rezervacija #${id} otkazana`, 'l-ok');
    load('reservations');
  } catch (err) {
    log(err.message, 'l-err');
  }
}

/* ---------- asinhroni paneli ---------- */

// Osvezavaju se sami da bi se video trenutak kada RabbitMQ poruka stigne.
let seen = { notifications: 0, notified: 0 };

async function pollAsyncPanels() {
  const notifications = await load('notifications');
  if (notifications.length > seen.notifications && seen.notifications > 0) {
    log(`RabbitMQ: stiglo ${notifications.length - seen.notifications} novo obavestenje`, 'l-async');
  }
  seen.notifications = notifications.length;

  const reservations = await load('reservations');
  const notified = reservations.filter(r => r.status === 'NOTIFIED').length;
  if (notified > seen.notified && seen.notified > 0) {
    log('RabbitMQ: red cekanja pomeren, clan je obavesten da je knjiga slobodna', 'l-async');
  }
  seen.notified = notified;
}

refreshAll();
log('Konzola spremna. Redosled za demo: clan, knjiga, pozajmica, rezervacija, vracanje.');
setInterval(pollAsyncPanels, 3000);

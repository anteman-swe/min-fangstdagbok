/*
 * FiskeExperten – kartsidan
 * - Ritar fångsterna på en Leaflet-karta (OpenStreetMap)
 * - Klick på ett fångstkort öppnar fångsten på kartan
 * - Öppnar/stänger panelen "Registrera fångst"
 * - Liten karta i panelen där man klickar för att välja position
 * - Förhandsvisning av vald bild
 */
(function () {
  'use strict';

  const SVERIGE = [62.0, 15.0];

  // Lucide-ikoner som text (behövs för Leaflet, som bygger HTML själv)
  const SVG_START = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"';
  const PIN_SVG = SVG_START + '><path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"/><circle cx="12" cy="10" r="3"/></svg>';
  const FISK_SVG = SVG_START + ' fill="none" stroke="currentColor" class="icon icon-xl"><path d="M6.5 12c.94-3.46 4.94-6 8.5-6 3.56 0 6.06 2.54 7 6-.94 3.47-3.44 6-7 6s-7.56-2.53-8.5-6Z"/><path d="M18 12v.5"/><path d="M16 17.93a9.77 9.77 0 0 1 0-11.86"/><path d="M7 10.67C7 8 5.58 5.97 2.73 5.5c-1 1.5-1 5 .23 6.5-1.24 1.5-1.24 5-.23 6.5C5.58 18.03 7 16 7 13.33"/><path d="M10.46 7.26C10.2 5.88 9.17 4.24 8 3h5.8a2 2 0 0 1 1.98 1.67l.23 1.4"/><path d="m16.01 17.93-.23 1.4A2 2 0 0 1 13.8 21H9.5a5.96 5.96 0 0 0 1.49-3.98"/></svg>';

  /** Skyddar mot att användarens text tolkas som HTML. */
  function esc(text) {
    return String(text == null ? '' : text).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function kartlager() {
    return L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      noWrap: true, // rita inte jordklotet flera gånger bredvid varandra
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });
  }

  /**
   * Hindrar att man zoomar ut så långt att världskartan upprepas,
   * och att man drar kartan utanför jordklotet.
   */
  const VARLDEN = L.latLngBounds([-85, -180], [85, 180]);
  function begransaKarta(map) {
    map.setMaxBounds(VARLDEN);
    map.options.maxBoundsViscosity = 1.0;
    function uppdateraMinZoom() {
      // Minsta zoom där världen är minst lika bred som kartrutan
      const minZoom = Math.ceil(Math.log2(map.getSize().x / 256));
      map.setMinZoom(Math.max(minZoom, 2));
    }
    uppdateraMinZoom();
    map.on('resize', uppdateraMinZoom);
  }

  /**
   * Zooma med Ctrl + scroll (Cmd + scroll på Mac, eller nyp på touchpad).
   * Vanlig scroll scrollar sidan och visar en kort tips-ruta.
   */
  function ctrlScrollZoom(map) {
    const behallare = map.getContainer();
    const tips = document.createElement('div');
    tips.className = 'map-tips';
    tips.textContent = navigator.platform.toUpperCase().includes('MAC')
        ? 'Håll ⌘ Cmd och scrolla för att zooma'
        : 'Håll Ctrl och scrolla för att zooma';
    behallare.appendChild(tips);

    let tipsTimer = null;
    let senasteZoom = 0;

    behallare.addEventListener('wheel', function (e) {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault(); // stoppa webbläsarens egen sidzoom
        tips.classList.remove('is-synlig');
        const nu = Date.now();
        if (nu - senasteZoom < 120) return; // en zoomnivå åt gången
        senasteZoom = nu;
        const steg = e.deltaY < 0 ? 1 : -1;
        map.setZoomAround(map.mouseEventToContainerPoint(e), map.getZoom() + steg);
      } else {
        tips.classList.add('is-synlig');
        clearTimeout(tipsTimer);
        tipsTimer = setTimeout(function () { tips.classList.remove('is-synlig'); }, 1200);
      }
    }, { passive: false });
  }

  const fiskIkon = L.divIcon({
    className: 'fisk-marker',
    html: PIN_SVG,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -38]
  });

  function popupHtml(d) {
    const bild = d.bild
        ? '<div class="fisk-bild popup-bild"><img class="fisk-foto" src="' + esc(d.bild) + '" alt=""></div>'
        : '<div class="fisk-bild popup-bild ' + esc(d.stil) + '">' + FISK_SVG + '</div>';
    return bild +
        '<div class="popup-body">' +
        '<h3>' + esc(d.art) + '</h3>' +
        '<p class="small muted">' + esc(d.matt) + ' · ' + esc(d.datum) + '</p>' +
        '<p class="small muted mt-1">' + esc(d.plats) + '</p>' +
        '</div>';
  }

  // ===== Huvudkartan =====
  const kartElement = document.getElementById('karta');
  const karta = L.map(kartElement, { scrollWheelZoom: false }).setView(SVERIGE, 5);
  kartlager().addTo(karta);
  begransaKarta(karta);
  ctrlScrollZoom(karta);

  const markorer = {};
  const grupp = L.featureGroup().addTo(karta);

  document.querySelectorAll('.catch-card').forEach(function (kort) {
    const d = kort.dataset;
    const lat = parseFloat(d.lat);
    const lng = parseFloat(d.lng);
    if (Number.isNaN(lat) || Number.isNaN(lng)) return;

    const markor = L.marker([lat, lng], { icon: fiskIkon, title: d.art })
        .bindPopup(popupHtml(d), { className: 'fisk-popup', minWidth: 240, maxWidth: 240 })
        .addTo(grupp);
    markorer[d.id] = markor;

    function visaPaKartan() {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      karta.setView([lat, lng], Math.max(karta.getZoom(), 9));
      markor.openPopup();
    }
    kort.addEventListener('click', function (e) {
      if (e.target.closest('[data-ingen-karta]')) return; // klick på redigera/radera
      visaPaKartan();
    });
    kort.addEventListener('keydown', function (e) {
      if (e.target !== kort) return;
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); visaPaKartan(); }
    });
  });

  if (Object.keys(markorer).length > 0) {
    karta.fitBounds(grupp.getBounds(), { padding: [60, 60], maxZoom: 10 });
  }

  // Öppna en viss fångst direkt (t.ex. ?vald=5 efter att man sparat)
  const vald = kartElement.dataset.vald;
  if (vald && markorer[vald]) {
    karta.setView(markorer[vald].getLatLng(), 9);
    markorer[vald].openPopup();
  }

  // ===== Panelen "Registrera fångst" =====
  const drawer = document.getElementById('drawer');
  const latInput = document.getElementById('lat');
  const lngInput = document.getElementById('lng');
  const positionVarde = document.getElementById('position-varde');
  let positionKarta = null;
  let positionMarkor = null;

  function sattPosition(lat, lng) {
    latInput.value = lat.toFixed(6);
    lngInput.value = lng.toFixed(6);
    if (positionMarkor) {
      positionMarkor.setLatLng([lat, lng]);
    } else {
      positionMarkor = L.marker([lat, lng], { icon: fiskIkon }).addTo(positionKarta);
    }
    positionVarde.textContent = 'Vald position: ' + lat.toFixed(4) + ', ' + lng.toFixed(4);
  }

  function skapaPositionKarta() {
    if (positionKarta) return;
    positionKarta = L.map('position-karta').setView(SVERIGE, 4);
    kartlager().addTo(positionKarta);
    begransaKarta(positionKarta);

    // Visa tidigare vald position (om formuläret visas igen efter valideringsfel)
    const lat = parseFloat(latInput.value);
    const lng = parseFloat(lngInput.value);
    if (!Number.isNaN(lat) && !Number.isNaN(lng)) {
      sattPosition(lat, lng);
      positionKarta.setView([lat, lng], 9);
    }

    positionKarta.on('click', function (e) {
      sattPosition(e.latlng.lat, e.latlng.lng);
    });
  }

  function oppnaPanel() {
    drawer.classList.add('is-open');
    document.body.classList.add('no-scroll');
    skapaPositionKarta();
    // Leaflet behöver räkna om storleken när kartan blir synlig
    setTimeout(function () { positionKarta.invalidateSize(); }, 50);
  }

  function stangPanel() {
    // Vid redigering: gå tillbaka till kartan så att formuläret töms
    if (drawer.dataset.avbrytUrl) { window.location.href = drawer.dataset.avbrytUrl; return; }
    drawer.classList.remove('is-open');
    document.body.classList.remove('no-scroll');
  }

  document.querySelectorAll('[data-oppna-form]').forEach(function (b) { b.addEventListener('click', oppnaPanel); });
  document.querySelectorAll('[data-stang-form]').forEach(function (b) { b.addEventListener('click', stangPanel); });
  drawer.addEventListener('click', function (e) { if (e.target === drawer) stangPanel(); });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && drawer.classList.contains('is-open')) stangPanel();
  });

  // Servern renderade sidan med panelen öppen (valideringsfel)
  if (drawer.classList.contains('is-open')) oppnaPanel();

  // ===== Bild: förhandsvisning + dra-och-släpp =====
  const bildInput = document.getElementById('bild');
  const dropzone = document.getElementById('dropzone');
  const forhandsvisning = document.getElementById('bild-preview');

  bildInput.addEventListener('change', function () {
    const fil = bildInput.files[0];
    if (fil) forhandsvisning.src = URL.createObjectURL(fil);
  });
  ['dragenter', 'dragover'].forEach(function (typ) {
    dropzone.addEventListener(typ, function () { dropzone.classList.add('is-dragover'); });
  });
  ['dragleave', 'drop'].forEach(function (typ) {
    dropzone.addEventListener(typ, function () { dropzone.classList.remove('is-dragover'); });
  });
})();

// ===== Fråga innan en fångst raderas =====
document.querySelectorAll('[data-bekrafta-radera]').forEach(function (form) {
  form.addEventListener('submit', function (e) {
    if (!window.confirm('Radera ' + form.dataset.art + '? Det går inte att ångra.')) e.preventDefault();
  });
});

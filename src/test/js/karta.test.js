// Tester för src/main/resources/static/js/karta.js
// Körs med:  node --test "src/test/js/**/*.test.js"
// Leaflet (L) och webbläsarens DOM ersätts av enkla låtsasobjekt, så inget behöver installeras.
const { readFileSync } = require("node:fs");
const { runInNewContext } = require("node:vm");
const { test } = require("node:test");
const assert = require("node:assert/strict");

function element(dataset = {}) {
    const classes = new Set();
    return {
        dataset,
        value: "",
        textContent: "",
        listeners: {},
        classList: {
            add: (c) => classes.add(c),
            remove: (c) => classes.delete(c),
            contains: (c) => classes.has(c)
        },
        addEventListener(type, callback) { this.listeners[type] = callback; },
        appendChild() {},
        closest() { return null; },
        focus() { this.document.activeElement = this; }
    };
}

/** Låtsas-Leaflet som sparar det som skriptet gör med kartorna. */
function fakeLeaflet() {
    const L = { maps: [], markers: [] };
    L.map = (container, options) => {
        const map = {
            container, options: options || {}, zoom: 5, view: null, listeners: {},
            setView(latlng, zoom) { this.view = latlng; this.zoom = zoom; return this; },
            getZoom() { return this.zoom; },
            fitBounds(bounds) { this.fitted = bounds; return this; },
            on(type, callback) { this.listeners[type] = callback; return this; },
            getContainer() { return element(); },
            getSize() { return { x: 1400, y: 600 }; },
            setMaxBounds() {}, setMinZoom(z) { this.minZoom = z; }, invalidateSize() {}
        };
        L.maps.push(map);
        return map;
    };
    L.tileLayer = () => ({ addTo() { return this; } });
    L.divIcon = (options) => options;
    L.latLngBounds = () => ({});
    L.featureGroup = () => ({ addTo() { return this; }, getBounds() { return "bounds"; } });
    L.marker = (latlng, options) => {
        const marker = {
            latlng, options, popup: null, opened: false,
            bindPopup(html) { this.popup = html; return this; },
            addTo() { return this; },
            openPopup() { this.opened = true; },
            getLatLng() { return this.latlng; },
            setLatLng(ll) { this.latlng = ll; }
        };
        L.markers.push(marker);
        return marker;
    };
    return L;
}

function setup({ cards = [], drawerOpen = false, editing = false, confirmAnswer = true } = {}) {
    const document = { listeners: {}, activeElement: null };
    document.addEventListener = (type, callback) => { document.listeners[type] = callback; };
    const el = (dataset) => { const e = element(dataset); e.document = document; return e; };
    document.createElement = () => el();
    document.body = el();

    const fangstKort = cards.map((c) => el(c));
    const oppna = el();
    const stang = el();
    const forstaFalt = el();
    const sparaKnapp = el();
    const drawer = el(editing ? { avbrytUrl: "/karta" } : {});
    if (drawerOpen) drawer.classList.add("is-open");
    drawer.querySelector = () => stang;
    drawer.querySelectorAll = () => [stang, forstaFalt, sparaKnapp];
    const raderaForm = el({ art: "Gädda" });

    const byId = {
        karta: el({}), drawer, lat: el(), lng: el(), "position-varde": el(),
        bild: Object.assign(el(), { files: [] }), dropzone: el(), "bild-preview": el()
    };
    document.getElementById = (id) => byId[id];
    const lists = {
        "[data-fangst]": fangstKort,
        "[data-oppna-form]": [oppna],
        "[data-stang-form]": [stang],
        "[data-bekrafta-radera]": [raderaForm]
    };
    document.querySelectorAll = (selector) => lists[selector] || [];

    const window = { location: { href: "" }, scrollTo() {}, confirm: () => confirmAnswer };
    const L = fakeLeaflet();
    runInNewContext(readFileSync("src/main/resources/static/js/karta.js", "utf8"), {
        document, window, L, navigator: { platform: "Win32" },
        setTimeout: (fn) => fn(), clearTimeout() {}, URL: { createObjectURL: () => "blob:x" }
    });
    return { document, window, L, fangstKort, oppna, stang, forstaFalt, sparaKnapp, drawer, raderaForm, byId };
}

function key(ui, key, shiftKey = false) {
    const event = { key, shiftKey, prevented: false, preventDefault() { this.prevented = true; } };
    ui.document.listeners.keydown(event);
    return event;
}

const GADDA = { id: "1", art: "Gädda", matt: "4,2 kg · 82 cm", datum: "12 nov", plats: "Mälaren", stil: "grad-1", lat: "59.585", lng: "16.585" };
const ABBORRE = { id: "2", art: "Abborre", matt: "0,9 kg", datum: "8 nov", plats: "Vättern", stil: "grad-2", lat: "58.305", lng: "14.245" };

test("varje fångstkort får en nål på rätt position", () => {
    const ui = setup({ cards: [GADDA, ABBORRE] });
    assert.equal(ui.L.markers.length, 2);
    assert.deepEqual(Array.from(ui.L.markers[0].latlng), [59.585, 16.585]);
    assert.deepEqual(Array.from(ui.L.markers[1].latlng), [58.305, 14.245]);
    assert.match(ui.L.markers[0].popup, /Gädda/);
});

test("text i popupen tolkas inte som HTML", () => {
    const ui = setup({ cards: [{ ...GADDA, art: "<script>alert(1)</script>" }] });
    assert.doesNotMatch(ui.L.markers[0].popup, /<script>/);
    assert.match(ui.L.markers[0].popup, /&lt;script&gt;/);
});

test("klick på ett kort öppnar fångsten, men inte klick på redigera/radera", () => {
    const ui = setup({ cards: [GADDA] });
    ui.fangstKort[0].listeners.click({ target: { closest: () => ({}) } });
    assert.equal(ui.L.markers[0].opened, false);
    ui.fangstKort[0].listeners.click({ target: { closest: () => null } });
    assert.equal(ui.L.markers[0].opened, true);
});

test("hovkartan zoomar inte med vanlig scroll", () => {
    const ui = setup();
    assert.equal(ui.L.maps[0].options.scrollWheelZoom, false);
});

test("panelen öppnas, håller kvar fokus och stängs med Escape", () => {
    const ui = setup();
    ui.document.activeElement = ui.oppna;
    ui.oppna.listeners.click();
    assert.equal(ui.drawer.classList.contains("is-open"), true);
    assert.equal(ui.document.body.classList.contains("no-scroll"), true);
    assert.equal(ui.document.activeElement, ui.stang);

    // Tab från sista elementet går till första, Shift+Tab från första går till sista
    ui.document.activeElement = ui.sparaKnapp;
    assert.equal(key(ui, "Tab").prevented, true);
    assert.equal(ui.document.activeElement, ui.stang);
    assert.equal(key(ui, "Tab", true).prevented, true);
    assert.equal(ui.document.activeElement, ui.sparaKnapp);

    key(ui, "Escape");
    assert.equal(ui.drawer.classList.contains("is-open"), false);
    assert.equal(ui.document.body.classList.contains("no-scroll"), false);
    assert.equal(ui.document.activeElement, ui.oppna);
});

test("klick bredvid panelen stänger den", () => {
    const ui = setup();
    ui.oppna.listeners.click();
    ui.drawer.listeners.click({ target: ui.stang });
    assert.equal(ui.drawer.classList.contains("is-open"), true);
    ui.drawer.listeners.click({ target: ui.drawer });
    assert.equal(ui.drawer.classList.contains("is-open"), false);
});

test("klick på positionskartan fyller i lat och lng", () => {
    const ui = setup();
    ui.oppna.listeners.click();
    const positionKarta = ui.L.maps[1];
    positionKarta.listeners.click({ latlng: { lat: 59.1234567, lng: 16.7654321 } });
    assert.equal(ui.byId.lat.value, "59.123457");
    assert.equal(ui.byId.lng.value, "16.765432");
    assert.match(ui.byId["position-varde"].textContent, /59\.1235, 16\.7654/);
});

test("panelen öppnas direkt när servern visar valideringsfel", () => {
    const ui = setup({ drawerOpen: true });
    assert.equal(ui.document.body.classList.contains("no-scroll"), true);
    assert.equal(ui.L.maps.length, 2);
});

test("att stänga panelen vid redigering går tillbaka till kartan", () => {
    const ui = setup({ drawerOpen: true, editing: true });
    key(ui, "Escape");
    assert.equal(ui.window.location.href, "/karta");
});

test("radering måste bekräftas", () => {
    const avbryt = setup({ confirmAnswer: false });
    const e1 = { prevented: false, preventDefault() { this.prevented = true; } };
    avbryt.raderaForm.listeners.submit(e1);
    assert.equal(e1.prevented, true);

    const ok = setup({ confirmAnswer: true });
    const e2 = { prevented: false, preventDefault() { this.prevented = true; } };
    ok.raderaForm.listeners.submit(e2);
    assert.equal(e2.prevented, false);
});

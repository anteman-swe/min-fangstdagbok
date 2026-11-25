import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { Fish, MapPin, Plus, Sparkles, Upload, X, LogOut, Search } from "lucide-react";

export const Route = createFileRoute("/karta")({
  head: () => ({
    meta: [
      { title: "Karta & fångster – FiskeExperten" },
      { name: "description", content: "Se dina fångster på kartan och registrera nya." },
      { property: "og:title", content: "Karta & fångster – FiskeExperten" },
      { property: "og:description", content: "Se dina fångster på kartan och registrera nya." },
    ],
  }),
  component: MapPage,
});

const catches = [
  { id: 1, art: "Gädda", vikt: "4,2 kg", langd: "82 cm", plats: "Mälaren, Västerås", datum: "12 nov", x: 30, y: 40, hue: "from-primary to-lake-light" },
  { id: 2, art: "Abborre", vikt: "0,9 kg", langd: "38 cm", plats: "Vättern, Hjo", datum: "8 nov", x: 55, y: 62, hue: "from-accent to-primary" },
  { id: 3, art: "Gös", vikt: "2,7 kg", langd: "64 cm", plats: "Hjälmaren", datum: "2 nov", x: 70, y: 30, hue: "from-lake-light to-lake" },
  { id: 4, art: "Öring", vikt: "1,6 kg", langd: "51 cm", plats: "Klarälven", datum: "28 okt", x: 42, y: 20, hue: "from-primary to-accent" },
];

function MapPage() {
  const [open, setOpen] = useState<number | null>(1);
  const [form, setForm] = useState(false);
  const sel = catches.find((c) => c.id === open);

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 flex items-center gap-6 bg-lake px-6 py-4 text-lake-foreground">
        <div className="flex items-center gap-2 font-semibold"><Fish className="h-5 w-5" /> FiskeExperten</div>
        <nav className="hidden gap-5 text-sm opacity-80 sm:flex"><span className="opacity-100">Karta</span><span>Mina fångster</span><span>Om</span></nav>
        <Link to="/" className="ml-auto flex items-center gap-1 text-sm opacity-80"><LogOut className="h-4 w-4" /> Logga ut</Link>
      </header>

      <section className="relative h-[60vh] overflow-hidden bg-land">
        <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute inset-0 h-full w-full text-water">
          <path fill="currentColor" d="M10 30 Q25 20 40 35 T60 40 Q75 30 85 45 Q80 60 65 58 Q50 75 35 65 Q15 60 10 30Z" />
          <path fill="currentColor" d="M60 10 Q70 5 80 15 Q78 25 68 22Z" />
          <path fill="none" stroke="currentColor" strokeWidth=".8" d="M0 80 Q30 70 50 85 T100 75" />
        </svg>
        {catches.map((c) => (
          <button key={c.id} onClick={() => setOpen(c.id)} style={{ left: `${c.x}%`, top: `${c.y}%` }}
            className="absolute -translate-x-1/2 -translate-y-full text-accent drop-shadow-lg transition hover:scale-110">
            <MapPin className="h-10 w-10 fill-accent stroke-lake" />
          </button>
        ))}
        {sel && (
          <div style={{ left: `${sel.x}%`, top: `${sel.y}%` }} className="absolute z-10 w-60 -translate-x-1/2 translate-y-3 overflow-hidden rounded-2xl bg-card shadow-soft">
            <div className={`flex h-28 items-center justify-center bg-gradient-to-br ${sel.hue} text-lake-foreground`}><Fish className="h-12 w-12" /></div>
            <div className="p-3">
              <div className="flex items-center justify-between"><h3 className="text-lg font-bold">{sel.art}</h3><button onClick={() => setOpen(null)}><X className="h-4 w-4" /></button></div>
              <p className="text-sm text-muted-foreground">{sel.vikt} · {sel.langd} · {sel.datum}</p>
              <button className="mt-2 text-sm font-semibold text-primary">Öppna fångst →</button>
            </div>
          </div>
        )}
        <button onClick={() => setForm(true)} className="absolute bottom-6 right-6 flex items-center gap-2 rounded-full bg-accent px-5 py-3 font-semibold text-accent-foreground shadow-soft hover:scale-105 transition">
          <Plus className="h-5 w-5" /> Ny fångst
        </button>
      </section>

      <section className="mx-auto max-w-6xl p-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div><h2 className="text-3xl font-bold">Alla fångster</h2><p className="text-muted-foreground">{catches.length} registrerade</p></div>
          <div className="flex items-center gap-2 rounded-xl border border-input bg-card px-3"><Search className="h-4 w-4 text-muted-foreground" /><input placeholder="Sök art eller plats" className="bg-transparent py-2 outline-none" /></div>
        </div>
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {catches.map((c) => (
            <article key={c.id} onClick={() => setOpen(c.id)} className="cursor-pointer overflow-hidden rounded-2xl bg-card shadow-soft transition hover:-translate-y-1">
              <div className={`flex h-36 items-center justify-center bg-gradient-to-br ${c.hue} text-lake-foreground`}><Fish className="h-14 w-14" /></div>
              <div className="p-4">
                <div className="flex justify-between"><h3 className="text-xl font-bold">{c.art}</h3><span className="text-sm text-muted-foreground">{c.datum}</span></div>
                <p className="mt-1 text-sm">{c.vikt} · {c.langd}</p>
                <p className="mt-2 flex items-center gap-1 text-sm text-muted-foreground"><MapPin className="h-3 w-3" /> {c.plats}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      {form && (
        <div className="fixed inset-0 z-30 flex justify-end bg-lake/40" onClick={() => setForm(false)}>
          <aside onClick={(e) => e.stopPropagation()} className="h-full w-full max-w-md overflow-y-auto bg-background p-6 shadow-soft">
            <div className="flex items-center justify-between"><h2 className="text-2xl font-bold">Registrera fångst</h2><button onClick={() => setForm(false)}><X /></button></div>
            <div className="mt-6 flex h-40 flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-primary/40 bg-muted text-muted-foreground">
              <Upload /> <span className="text-sm">Dra hit en bild eller klicka</span>
            </div>
            <button className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-secondary py-2 text-sm font-semibold"><Sparkles className="h-4 w-4 text-accent" /> Identifiera art med AI</button>
            {[["Art", "t.ex. Gädda"], ["Vikt (kg)", "4,2"], ["Längd (cm)", "82"], ["Datum", "2026-11-25"]].map(([l, p]) => (
              <div key={l} className="mt-4"><label className="text-sm font-medium">{l}</label><input placeholder={p} className="mt-1 w-full rounded-xl border border-input bg-card px-3 py-2 outline-none" /></div>
            ))}
            <div className="mt-4"><label className="text-sm font-medium">Position</label>
              <div className="mt-1 flex h-24 items-center justify-center rounded-xl bg-land text-sm"><MapPin className="mr-1 h-4 w-4 text-accent" /> Läst från bildens GPS – klicka för att justera</div></div>
            <button className="mt-6 w-full rounded-xl bg-primary py-3 font-semibold text-primary-foreground">Spara fångst</button>
          </aside>
        </div>
      )}
    </div>
  );
}

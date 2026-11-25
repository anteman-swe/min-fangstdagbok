import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Fish, Lock, Mail } from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Logga in – FiskeExperten" },
      { name: "description", content: "Logga in på FiskeExperten och se dina fångster på kartan." },
      { property: "og:title", content: "Logga in – FiskeExperten" },
      { property: "og:description", content: "Logga in och se dina fångster på kartan." },
    ],
  }),
  component: Login,
});

function Waves() {
  return (
    <svg viewBox="0 0 600 120" className="absolute bottom-0 left-0 w-full text-lake" preserveAspectRatio="none">
      <path fill="currentColor" opacity=".5" d="M0 60 Q75 20 150 60 T300 60 T450 60 T600 60 V120 H0Z" />
      <path fill="currentColor" d="M0 85 Q75 50 150 85 T300 85 T450 85 T600 85 V120 H0Z" />
    </svg>
  );
}

function Login() {
  const nav = useNavigate();
  return (
    <main className="grid min-h-screen lg:grid-cols-2">
      <section className="relative hidden overflow-hidden bg-dawn p-12 text-lake-foreground lg:flex lg:flex-col">
        <div className="flex items-center gap-2 text-lg font-semibold"><Fish className="h-6 w-6" /> FiskeExperten</div>
        <div className="mt-auto mb-32 max-w-md">
          <h1 className="text-5xl font-bold leading-tight">Varje fångst har en plats.</h1>
          <p className="mt-4 text-lg opacity-85">Spara bild, art och position. Se alla dina fiskeplatser på kartan.</p>
        </div>
        <div className="absolute right-24 top-32 h-28 w-28 rounded-full bg-accent/70 blur-sm" />
        <Waves />
      </section>

      <section className="flex items-center justify-center p-6">
        <form
          onSubmit={(e) => { e.preventDefault(); nav({ to: "/karta" }); }}
          className="w-full max-w-sm rounded-3xl bg-card p-8 shadow-soft"
        >
          <div className="mb-6 flex items-center gap-2 text-primary lg:hidden"><Fish /> <span className="font-semibold">FiskeExperten</span></div>
          <h2 className="text-3xl font-bold">Välkommen tillbaka</h2>
          <p className="mt-1 text-sm text-muted-foreground">Logga in för att se dina fångster.</p>
          <label className="mt-6 block text-sm font-medium">E-post</label>
          <div className="mt-1 flex items-center gap-2 rounded-xl border border-input bg-background px-3">
            <Mail className="h-4 w-4 text-muted-foreground" />
            <input type="email" placeholder="du@exempel.se" className="w-full bg-transparent py-3 outline-none" />
          </div>
          <label className="mt-4 block text-sm font-medium">Lösenord</label>
          <div className="mt-1 flex items-center gap-2 rounded-xl border border-input bg-background px-3">
            <Lock className="h-4 w-4 text-muted-foreground" />
            <input type="password" placeholder="••••••••" className="w-full bg-transparent py-3 outline-none" />
          </div>
          <button className="mt-6 w-full rounded-xl bg-primary py-3 font-semibold text-primary-foreground transition hover:opacity-90">Logga in</button>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Inget konto? <Link to="/karta" className="font-semibold text-primary">Skapa konto</Link>
          </p>
        </form>
      </section>
    </main>
  );
}

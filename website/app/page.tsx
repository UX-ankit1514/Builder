import Link from "next/link";

export default function Home() {
  return (
    <section className="hero">
      <h1>Stop organising. Start the next thing.</h1>
      <p>
        Stepwise turns what you’ve been avoiding into small steps — one at a time. Capture a task, pick what
        matters today, and do one step in Focus Mode.
      </p>
      <div className="cards">
        <Link href="/privacy/" className="card">
          <h2>Privacy Policy</h2>
          <p>What we collect, why, where it’s stored, and how to export or delete it.</p>
        </Link>
        <Link href="/terms/" className="card">
          <h2>Terms &amp; Conditions</h2>
          <p>The rules for using Stepwise on Android and iOS.</p>
        </Link>
      </div>
    </section>
  );
}

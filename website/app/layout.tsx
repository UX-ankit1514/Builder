import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";
import { site } from "./site";

export const metadata: Metadata = {
  title: { default: site.appName, template: `%s · ${site.appName}` },
  description: "Stepwise helps you stop organising and start the next thing — one small step at a time.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <header className="nav">
          <Link href="/" className="brand">
            <span className="mark">S</span>
            {site.appName}
          </Link>
          <nav>
            <Link href="/privacy/">Privacy</Link>
            <Link href="/terms/">Terms</Link>
          </nav>
        </header>
        <main>{children}</main>
        <footer className="footer">
          <span>© 2026 {site.appName}</span>
          <Link href="/privacy/">Privacy Policy</Link>
          <Link href="/terms/">Terms &amp; Conditions</Link>
          <a href={`mailto:${site.contactEmail}`}>Contact</a>
        </footer>
      </body>
    </html>
  );
}

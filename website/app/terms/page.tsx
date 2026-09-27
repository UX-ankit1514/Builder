import type { Metadata } from "next";
import { site } from "../site";

export const metadata: Metadata = { title: "Terms & Conditions" };

export default function Terms() {
  return (
    <article className="doc">
      <h1>Terms &amp; Conditions</h1>
      <p className="updated">Last updated: {site.lastUpdated}</p>

      <p>
        These terms apply to your use of the {site.appName} mobile app for Android and iOS (the “App”). By using the
        App, you agree to them. If you don’t agree, please don’t use the App.
      </p>

      <h2>1. The service</h2>
      <p>
        {site.appName} helps you capture tasks, plan your day, break tasks into steps and work on one step at a time.
        The App is provided free of charge. We may add, change or remove features over time.
      </p>

      <h2>2. Your account</h2>
      <ul>
        <li>You can use the App as a guest or sign in with Google.</li>
        <li>
          Guest data is linked to this installation of the App. If you uninstall the App or sign out before signing in
          with Google, your guest data may be lost.
        </li>
        <li>You are responsible for keeping your Google account and your device secure.</li>
        <li>You must be at least 13 years old, or the minimum age required in your country, to use the App.</li>
      </ul>

      <h2>3. Your content</h2>
      <p>
        You own the tasks, notes and steps you create. You give us permission to store and process them only as needed
        to run the App for you, as described in our <a href="/privacy/">Privacy Policy</a>. You can export or delete
        your content at any time.
      </p>

      <h2>4. Acceptable use</h2>
      <p>You agree not to:</p>
      <ul>
        <li>use the App for anything illegal or harmful;</li>
        <li>try to access other people’s data or interfere with the service, its security or its servers;</li>
        <li>reverse engineer, copy or resell the App, except where the law allows it.</li>
      </ul>

      <h2>5. No professional advice</h2>
      <p>
        {site.appName} is a productivity tool. It is not medical, mental-health, legal or financial advice, and it is
        not a substitute for professional support.
      </p>

      <h2>6. Availability and changes</h2>
      <p>
        We work to keep the App available and your data safe, but we can’t promise it will always be uninterrupted or
        error-free. Keep your own copy of anything important (Settings → Export data).
      </p>

      <h2>7. Disclaimer and liability</h2>
      <p>
        The App is provided “as is” and “as available”, without warranties of any kind, to the extent the law allows.
        To the extent the law allows, we are not liable for indirect or consequential losses, or for lost data or
        missed deadlines, arising from your use of the App. Nothing in these terms limits rights you have under
        consumer law that cannot be excluded.
      </p>

      <h2>8. Ending your use</h2>
      <p>
        You can stop using the App and delete your account at any time from Settings → Account. We may suspend access
        if these terms are seriously or repeatedly broken.
      </p>

      <h2>9. Changes to these terms</h2>
      <p>
        If we change these terms, we will update the date above. If a change is significant, we will let you know in
        the App. Continuing to use the App after a change means you accept the new terms.
      </p>

      <h2>10. Governing law</h2>
      <p>These terms are governed by the laws of India, and the courts of India have jurisdiction.</p>

      <h2>11. Contact</h2>
      <p>
        Questions about these terms: <a href={`mailto:${site.contactEmail}`}>{site.contactEmail}</a>
      </p>
    </article>
  );
}

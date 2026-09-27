import type { Metadata } from "next";
import { site } from "../site";

export const metadata: Metadata = { title: "Privacy Policy" };

export default function Privacy() {
  return (
    <article className="doc">
      <h1>Privacy Policy</h1>
      <p className="updated">Last updated: {site.lastUpdated}</p>

      <p className="summary">
        <strong>In short:</strong> we store your tasks so the app works and syncs across your devices. We don’t sell
        your data, show ads, or use tracking or analytics tools. You can export or delete everything from inside the
        app at any time.
      </p>

      <p>
        This policy explains how {site.appName} (“we”, “us”) handles information when you use the {site.appName} mobile
        app for Android and iOS (the “App”) and this website.
      </p>

      <h2>1. Information we collect</h2>
      <ul>
        <li>
          <strong>Account information.</strong> If you sign in with Google, we receive your name, email address and a
          unique account ID from Google. We never see your Google password.
        </li>
        <li>
          <strong>Guest account.</strong> If you use the App without signing in, we create an anonymous account ID so
          your tasks can be saved. It contains no name or email.
        </li>
        <li>
          <strong>Content you create.</strong> Task titles, notes, deadlines, steps, which list a task is in (Inbox,
          Today, Urgent), and when you complete, skip or pause tasks.
        </li>
        <li>
          <strong>Settings.</strong> Your in-app preferences, such as reduce motion, progress bars, haptics and higher
          contrast.
        </li>
        <li>
          <strong>Technical data.</strong> Our service providers (see section 3) may process limited technical data
          such as IP address and device type in order to deliver the service securely.
        </li>
      </ul>
      <p>We do not collect your location, contacts, photos, or advertising identifiers.</p>

      <h2>2. How we use it</h2>
      <ul>
        <li>To provide the App: save your tasks, sync them between your devices, and show your progress.</li>
        <li>To keep your account and data secure.</li>
        <li>To respond when you contact us.</li>
      </ul>
      <p>We do not sell your data, use it for advertising, or share it with data brokers.</p>

      <h2>3. Where your data is stored</h2>
      <p>
        We use Google Firebase (Firebase Authentication and Cloud Firestore), provided by Google LLC, to sign you in
        and store your data. Your tasks are stored in {site.dataRegion}. Data is encrypted in transit and at rest.
        Security rules ensure that only your signed-in account can read or change your tasks. Google processes this
        data on our behalf under its{" "}
        <a href="https://firebase.google.com/support/privacy" target="_blank" rel="noreferrer">
          Firebase privacy terms
        </a>
        .
      </p>

      <h2>4. How long we keep it</h2>
      <p>
        We keep your data while your account exists. When you delete your account in the App, we permanently delete
        your tasks, settings and account. A copy may remain in encrypted backups for a short period before it is
        overwritten.
      </p>

      <h2>5. Your choices and rights</h2>
      <ul>
        <li>
          <strong>Export:</strong> Settings → Export data gives you a file with all your tasks, steps and notes.
        </li>
        <li>
          <strong>Delete:</strong> Settings → Account → Delete account permanently removes your data.
        </li>
        <li>
          <strong>Correct:</strong> you can edit any task or setting in the App.
        </li>
        <li>
          You may also contact us to access, correct or delete your data, or to raise a concern. Depending on where you
          live, you may have further rights under laws such as India’s Digital Personal Data Protection Act, 2023 or
          the EU/UK GDPR.
        </li>
      </ul>

      <h2>6. Children</h2>
      <p>
        The App is not directed at children under 13, and we do not knowingly collect their data. If you believe a
        child has given us personal data, contact us and we will delete it.
      </p>

      <h2>7. Changes to this policy</h2>
      <p>
        If we make material changes, we will update the date above and, where appropriate, let you know in the App.
      </p>

      <h2>8. Contact</h2>
      <p>
        Questions or requests: <a href={`mailto:${site.contactEmail}`}>{site.contactEmail}</a>
      </p>
    </article>
  );
}

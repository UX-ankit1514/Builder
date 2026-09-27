// Runs against the Firestore emulator (npm test). Project "demo-stepwise" is a
// local-only demo project, so these tests can never touch real data.
import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { collection, deleteDoc, doc, getDoc, getDocs, setDoc, updateDoc } from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-stepwise',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});
after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

const alice = () => env.authenticatedContext('alice').firestore();
const bob = () => env.authenticatedContext('bob').firestore();
const nobody = () => env.unauthenticatedContext().firestore();

const task = (overrides = {}) => ({
  title: 'Call the bank about my card',
  notes: '',
  deadline: null,
  list: 'inbox',
  steps: [{ id: 'a1', title: 'Find the number', done: false, doneAt: null }],
  pausedAt: null,
  skippedAt: null,
  completedAt: null,
  createdAt: 1790000000000,
  updatedAt: 1790000000000,
  sortOrder: 1790000000000.5,
  plannedAt: null,
  ...overrides,
});

const profile = (overrides = {}) => ({
  onboarded: false,
  createdAt: 1790000000000,
  lastActiveAt: 1790000000000,
  settings: {
    aiSuggestions: true,
    reduceMotion: false,
    showProgressBars: true,
    showStepDoneScreen: true,
    haptics: true,
    higherContrast: false,
  },
  ...overrides,
});

test('owner can create, read, update and delete their own task', async () => {
  const ref = doc(alice(), 'users/alice/tasks/t1');
  await assertSucceeds(setDoc(ref, task()));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(getDocs(collection(alice(), 'users/alice/tasks')));
  await assertSucceeds(updateDoc(ref, { list: 'urgent', plannedAt: 1790000000001 }));
  await assertSucceeds(deleteDoc(ref));
});

test('another user can never read or change my tasks', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), 'users/alice/tasks/t1'), task());
  });
  await assertFails(getDoc(doc(bob(), 'users/alice/tasks/t1')));
  await assertFails(getDocs(collection(bob(), 'users/alice/tasks')));
  await assertFails(setDoc(doc(bob(), 'users/alice/tasks/t2'), task()));
  await assertFails(deleteDoc(doc(bob(), 'users/alice/tasks/t1')));
});

test('signed-out requests are denied everywhere', async () => {
  await assertFails(getDoc(doc(nobody(), 'users/alice/tasks/t1')));
  await assertFails(setDoc(doc(nobody(), 'users/alice/tasks/t1'), task()));
  await assertFails(getDoc(doc(nobody(), 'users/alice')));
});

test('unknown fields, bad lists and bad titles are rejected', async () => {
  const ref = doc(alice(), 'users/alice/tasks/t1');
  await assertFails(setDoc(ref, task({ isAdmin: true })));
  await assertFails(setDoc(ref, task({ list: 'someday' })));
  await assertFails(setDoc(ref, task({ title: '' })));
  await assertFails(setDoc(ref, task({ title: 'x'.repeat(201) })));
  await assertFails(setDoc(ref, task({ notes: 'x'.repeat(5001) })));
  await assertFails(setDoc(ref, task({ deadline: 'next friday' })));
  await assertFails(setDoc(ref, task({ createdAt: 'yesterday' })));
  await assertSucceeds(setDoc(ref, task({ deadline: '2026-10-02' })));
});

test('a task cannot hold more than 50 steps', async () => {
  const steps = Array.from({ length: 51 }, (_, i) => ({ id: `s${i}`, title: `Step ${i}`, done: false, doneAt: null }));
  await assertFails(setDoc(doc(alice(), 'users/alice/tasks/t1'), task({ steps })));
});

test('profiles: owner only, known settings only, no listing users', async () => {
  const ref = doc(alice(), 'users/alice');
  await assertSucceeds(setDoc(ref, profile()));
  await assertSucceeds(setDoc(ref, { onboarded: true }, { merge: true }));
  await assertFails(setDoc(ref, profile({ role: 'admin' })));
  await assertFails(setDoc(ref, profile({ settings: { haptics: 'yes' } })));
  await assertFails(getDoc(doc(bob(), 'users/alice')));
  await assertFails(getDocs(collection(alice(), 'users')));
});

test('paths outside users/{uid} are denied', async () => {
  await assertFails(setDoc(doc(alice(), 'config/global'), { anything: true }));
  await assertFails(getDoc(doc(alice(), 'config/global')));
});

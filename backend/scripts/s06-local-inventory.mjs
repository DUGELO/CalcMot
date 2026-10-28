// Metadata/counts only, fixed LOCAL bank. No grants, statements supplied by a
// caller, remote credentials, deletions, profile values or evidence contents.
import {existsSync, readdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {Miniflare} from 'miniflare';
const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const state = path.resolve(backend,'../.tmp/s06-issuance-local/v3/d1');
const sqliteExists = dir => existsSync(dir) && readdirSync(dir,{withFileTypes:true})
  .some(entry=>entry.isDirectory()?sqliteExists(path.join(dir,entry.name)):entry.name.endsWith('.sqlite'));
let runtime;
try {
  if (process.argv.length!==2 || !sqliteExists(state)) throw new Error('EXISTING_LOCAL_DATABASE_REQUIRED');
  runtime = new Miniflare({modules:true,script:'export default {fetch(){return new Response("not exposed")}}',
    compatibilityDate:'2026-07-01',cf:false,d1Databases:{DB:'00000000-0000-0000-0000-000000000000'},d1Persist:state});
  const db = await runtime.getD1Database('DB');
  const schema = await db.prepare("SELECT name, type, tbl_name, sql FROM sqlite_master WHERE type IN ('table','trigger')").all();
  const tables = [];
  for(const table of schema.results.filter(r=>r.type==='table' && /^(legacy_[a-z_]+|entitlement_snapshots)$/.test(r.name))) {
    const fields = await db.prepare(`PRAGMA table_info("${table.name}")`).all();
    const count = await db.prepare(`SELECT COUNT(*) AS n FROM "${table.name}"`).first();
    const triggers = schema.results.filter(r=>r.type==='trigger' && r.tbl_name===table.name);
    tables.push({table:table.name,rows:count.n,fields:fields.results.map(c=>c.name),
      deleteGuards:triggers.filter(t=>/BEFORE\s+DELETE/i.test(t.sql)).map(t=>t.name),
      updateGuards:triggers.filter(t=>/BEFORE\s+UPDATE/i.test(t.sql)).map(t=>t.name)});
  }
  process.stdout.write(JSON.stringify({scope:'existing DEV LOCAL only; metadata and counts',tables,
    personalRowContentsRead:false,mutationsRequested:false,remoteAccess:false},null,2)+'\n');
} catch(error) {
  process.stderr.write(JSON.stringify({code:error?.message==='EXISTING_LOCAL_DATABASE_REQUIRED'?error.message:'INVENTORY_UNAVAILABLE'})+'\n');
  process.exitCode=1;
} finally {await runtime?.dispose();}

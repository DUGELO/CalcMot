// Read-only. No D1, service-account creation, role changes or saved token.
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {build} from 'esbuild';

try {
  const project = process.env.FIREBASE_PROJECT_ID;
  const ownerUid = process.env.LEGACY_OWNER_UID;
  if (process.argv.slice(2).length === 1 && process.argv[2] === '--configuration') {
    const ready = !!project && !!ownerUid;
    process.stdout.write(JSON.stringify({status:ready?'TOKEN_VALIDATION_REQUIRED':'HOLD',
      projectConfigured:!!project,ownerConfigured:!!ownerUid,credentialsOmitted:true})+'\n');
    process.exitCode = ready ? 0 : 2;
  } else {
    if (process.argv.length !== 2 || !project || !ownerUid) throw new Error('CONFIGURATION_REQUIRED');
    let text = '';
    for await (const chunk of process.stdin) {
      text += chunk; if (Buffer.byteLength(text)>20000) throw new Error('INVALID_REQUEST');
    }
    const request = JSON.parse(text);
    if (!request || Object.keys(request).some(k=>!['token','expected'].includes(k)) ||
      typeof request.token !== 'string' || !['owner','ordinary'].includes(request.expected)) throw new Error('INVALID_REQUEST');
    const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
    const bundle = await build({stdin:{contents:"export {verifySession} from './src/auth.ts'; export {operatorReadiness} from './scripts/operator-readiness.ts';",
      resolveDir:backend,sourcefile:'operator-preflight.ts',loader:'ts'},absWorkingDir:backend,tsconfigRaw:{},bundle:true,
      platform:'node',format:'esm',write:false,logLevel:'silent'});
    const api = await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].contents).toString('base64')}`);
    const session = await api.verifySession(new Request('https://local.invalid',
      {headers:{Authorization:`Bearer ${request.token}`}}),project); // Fixed Google JWKS, no test-key input.
    delete request.token; text = '';
    const result = api.operatorReadiness(session,project,ownerUid,request.expected);
    process.stdout.write(JSON.stringify(result)+'\n'); process.exitCode = result.status === 'GO' ? 0 : 2;
  }
} catch(error) {
  const code = error?.constructor?.name === 'InvalidSession' ? 'AUTH_REQUIRED' :
    ['CONFIGURATION_REQUIRED','INVALID_REQUEST'].includes(error?.message) ? error.message : 'RETRY_LATER';
  process.stderr.write(JSON.stringify({code,credentialsOmitted:true})+'\n'); process.exitCode = 1;
}

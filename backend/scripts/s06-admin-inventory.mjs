// READ ONLY official Firebase Admin SDK. Optional pre-existing administrator
// runtime/ADC required: this script does not install SDKs, create keys or roles.
// Never output users, UIDs, emails, custom-claim payloads or credential errors.
try {
  const project = process.env.FIREBASE_PROJECT_ID; const owner = process.env.LEGACY_OWNER_UID;
  if (process.argv.length!==2 || project!=='calcmot' || !owner) throw new Error('CONFIGURATION_REQUIRED');
  let sdk; let authSdk;
  try {sdk = await import('firebase-admin/app'); authSdk = await import('firebase-admin/auth');}
  catch {throw new Error('EXISTING_ADMIN_RUNTIME_REQUIRED');}
  const app = sdk.initializeApp({projectId:project,credential:sdk.applicationDefault()},'s06-read-only-inventory');
  try {
    const auth = authSdk.getAuth(app);
    let pageToken; let users=0; let ownerFound=false; let ownerAdmin=false; let otherAdmins=0;
    do {
      const page = await auth.listUsers(1000,pageToken);
      for(const user of page.users) {
        users++;
        if(user.uid===owner) {ownerFound=true;ownerAdmin=user.customClaims?.admin===true;}
        else if(user.customClaims?.admin===true) otherAdmins++;
      }
      pageToken=page.pageToken;
    } while(pageToken);
    const ready = ownerFound && ownerAdmin && otherAdmins===0;
    process.stdout.write(JSON.stringify({status:ready?'GO':'HOLD',scope:'read-only current project user inventory',
      users,ownerFound,ownerAdmin,otherAdmins,allPagesRead:true,credentialsOmitted:true})+'\n');
    process.exitCode=ready?0:2;
  } finally {await sdk.deleteApp(app);}
} catch(error) {
  const code = ['CONFIGURATION_REQUIRED','EXISTING_ADMIN_RUNTIME_REQUIRED'].includes(error?.message) ? error.message : 'ADMIN_INVENTORY_UNAVAILABLE';
  process.stderr.write(JSON.stringify({code,credentialsOmitted:true})+'\n'); process.exitCode=1;
}

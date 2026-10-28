// Local caller for the private pilot bridge. No Firebase token reaches argv/disk/output.
let input='';for await(const chunk of process.stdin){input+=chunk;if(input.length>32768)throw new Error('INVALID_REQUEST');}
const command=JSON.parse(input);
const origin='http://127.0.0.1:8966';
const html=await(await fetch(origin)).text();
const csrf=html.match(/name="csrf" value="([a-f0-9]+)"/)?.[1];
if(!csrf)throw new Error('PRIVATE_CONSOLE_REQUIRED');
const result=await fetch(origin+'/command',{method:'POST',headers:{Origin:origin,'Content-Type':'application/x-www-form-urlencoded'},
  body:new URLSearchParams({csrf,command:JSON.stringify(command)})});
process.stdout.write(await result.text()+'\n');
if(!result.ok)process.exitCode=1;

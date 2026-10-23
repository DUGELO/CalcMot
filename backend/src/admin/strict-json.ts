/** Bounded request JSON with duplicate-member rejection. JSON.parse remains the grammar authority. */
export function strictJson(text:string):unknown {
  const parsed:unknown=JSON.parse(text);
  const tokens=text.match(/"(?:[^"\\]|\\[\s\S])*"|[{}\[\],:]|true|false|null|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/g)??[];
  let index=0;
  function value(depth:number):void {
    if(depth>32)throw Error('JSON depth');
    const token=tokens[index++];
    if(token==='{') {
      const seen=new Set<string>();
      if(tokens[index]==='}'){index++;return}
      for(;;){const key=JSON.parse(tokens[index++]);if(seen.has(key))throw Error('Duplicate JSON member');seen.add(key);
        if(tokens[index++]!==':')throw Error('JSON object');value(depth+1);
        const end=tokens[index++];if(end==='}')return;if(end!==',')throw Error('JSON object')}
    }
    if(token==='[') {
      if(tokens[index]===']'){index++;return}
      for(;;){value(depth+1);const end=tokens[index++];if(end===']')return;if(end!==',')throw Error('JSON array')}
    }
    if(token===undefined)throw Error('JSON value');
  }
  value(0);if(index!==tokens.length)throw Error('JSON trailing value');
  return parsed;
}
